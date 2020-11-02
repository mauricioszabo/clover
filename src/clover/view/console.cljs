(ns clover.view.console
  (:require [repl-tooling.editor-integration.renderer.console :as console]
            [repl-tooling.editor-integration.renderer :as render]
            [repl-tooling.eval :as eval]
            [promesa.core :as p]
            [clojure.edn :as edn]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(defn register-console! []
  (let [scrolled? (atom true)]
    (rdom/render [(with-meta console/console-view
                    {:get-snapshot-before-update #(reset! scrolled? (console/all-scrolled?))
                     :component-did-update #(console/scroll-to-end! scrolled?)})]
                 console/div)
    (.. js/document
        (querySelector "div")
        (replaceWith console/div))))

(def ^:private pending-evals (atom {}))
(defonce ^:private post-message! (-> (js/acquireVsCodeApi) .-postMessage))

(defrecord Evaluator [flavor]
  eval/Evaluator
  (evaluate [_ command _ callback]
    (let [id (gensym)]
      (swap! pending-evals assoc id callback)
      (post-message! (pr-str {:op :eval-code
                              :args {:command (str "(do\n" command "\n)")
                                     :repl flavor
                                     :id id}}))
      id)))

(defn- to-edn [string]
  (let [edn (edn/read-string {:default tagged-literal} string)
        txt (:as-text edn)
        key (if (:error edn) :error :result)]
    (-> edn
        (dissoc :parsed?)
        (assoc key txt))))

(defonce pending-calls (atom {}))
(defn- run-call! [op cmd args]
  (let [id (gensym)
        prom (p/deferred)]
    (swap! pending-calls assoc id prom)
    (post-message! (pr-str {:op op :cmd cmd :args args :id id}))
    prom))

(defn- run-call-args! [callback]
  (fn [ & args] (run-call! :run-callback callback args)))

(def editor-state
  {:run-callback (fn [cmd & args] (run-call! :run-callback cmd args))
   :run-feature (fn [cmd & args] (run-call! :run-feature cmd args))
   :editor/callbacks {:file-exists (run-call-args! :file-exists)
                      :read-file (run-call-args! :read-file)
                      :open-editor (run-call-args! :open-editor)
                      :config-file-path (run-call-args! :config-file-path)
                      :register-commands (run-call-args! :register-commands)
                      :on-start-eval (run-call-args! :on-start-eval)
                      :on-eval (run-call-args! :on-eval)
                      :editor-data (run-call-args! :editor-data)
                      :notify (run-call-args! :notify)
                      :get-config (run-call-args! :get-config)
                      :prompt (run-call-args! :prompt)
                      :on-copy (run-call-args! :on-copy)
                      :on-stdout (run-call-args! :on-stdout)
                      :on-stderr (run-call-args! :on-stderr)
                      :on-result (run-call-args! :on-result)
                      :get-rendered-results (run-call-args! :get-rendered-results)
                      :on-patch (run-call-args! :on-patch)
                      :on-disconnect (run-call-args! :on-disconnect)}})

(defn- render-result [string-result repl-flavor]
  (let [repl (->Evaluator repl-flavor)
        result (to-edn string-result)]
    (console/result result #(render/parse-result % repl (r/atom editor-state)))))

(defn- send-response! [{:keys [id result]}]
  (let [callback (get @pending-evals id)]
    (callback result))
  (swap! pending-evals dissoc id))

(defn- find-patch [id maybe-coll]
  (let [elem (if (instance? reagent.ratom/RAtom maybe-coll)
               (dissoc @maybe-coll :editor-state :repl)
               maybe-coll)]
    (if (and (instance? render/Patchable elem)
             (= id (:id elem)))
      maybe-coll
      (when (coll? elem)
        (->> elem
             (map #(find-patch id %))
             flatten
             (filter identity))))))

(defn- patch-result! [{:keys [id result]}]
  (let [repl (->Evaluator :cljs)
        norm {:result (:as-text result)
              :as-text (:as-text result)}
        results (->> @console/out-state
                     (filter #(-> % first (= :result)))
                     (map second))]
    (doseq [result (find-patch id results)]
      (swap! result assoc :value (render/parse-result norm repl (r/atom editor-state))))))

(defn- resolve-prom! [{:keys [id result]}]
  (when-let [prom (get @pending-calls id)]
    (swap! pending-calls dissoc id)
    (p/resolve! prom result)))

(defn main []
  (.. js/window
      (addEventListener "message"
                        (fn [message]
                          (let [{:keys [command obj repl]} (->> message
                                                                .-data
                                                                (edn/read-string {:default tagged-literal}))]
                            (case command
                              :stdout (console/stdout obj)
                              :stderr (console/stderr obj)
                              :result (render-result obj repl)
                              :eval-result (send-response! obj)
                              :patch (patch-result! obj)
                              :clear (console/clear)
                              :call-result (resolve-prom! obj))))))
  (register-console!))
