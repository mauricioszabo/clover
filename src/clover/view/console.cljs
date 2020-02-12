(ns clover.view.console
  (:require [reagent.core :as r]
            [repl-tooling.editor-integration.renderer :as render]
            [repl-tooling.editor-helpers :as helpers]
            [cljs.reader :as edn]
            [repl-tooling.eval :as eval]
            [clojure.core.async :as async :include-macros true]
            [repl-tooling.editor-integration.connection :as conn]
            ["ansi_up" :default Ansi]))

(defonce out-state (r/atom []))

(defn- rendered-content [parsed-ratom]
  (let [error? (-> parsed-ratom meta :error)]
    [:div {:class ["result" "chlorine" (when error? "error")]}
     [render/view-for-result parsed-ratom]]))

(defonce ansi (new Ansi))
(defn- cell-for [[out-type object] idx]
  (let [kind (out-type {:stdout :output :stderr :err :result :result})
        icon (out-type {:stdout "icon-quote" :stderr "icon-alert" :result "icon-code"})]
    [:div.cell {:key idx}
     [:div.gutter [:span {:class ["icon" icon]}]]
     (if (= out-type :result)
       [:div.content [rendered-content object]]
       (let [html (. ansi ansi_to_html object)]
         [:div.content [:div {:class kind :dangerouslySetInnerHTML #js {:__html html}}]]))]))

(defn console-view []
  [:div.chlorine.console.native-key-bindings {:tabindex 1}
   [:<> (map cell-for @out-state (range))]])

(defonce div (. js/document querySelector "div"))

(defn- all-scrolled? []
  (let [window-scroll-pos (+ (.. js/window -scrollY) (.. js/window -innerHeight) 1)
        document-height (.. js/document (querySelector "body") -clientHeight)]
    (>= window-scroll-pos document-height)))

(defn- scroll-to-end! [scrolled?]
  (when @scrolled?
    (.scroll js/window 0 (.. js/document (querySelector "body") -clientHeight))))

(defn clear []
  (reset! out-state []))

(defn- append-text [stream text]
  (let [[old-stream old-text] (peek @out-state)]
    (if (= old-stream stream)
      (swap! out-state #(-> % pop (conj [stream (str old-text text)])))
      (swap! out-state conj [stream text]))))

(defn result [parsed-result repl]
  (swap! out-state conj [:result (render/parse-result parsed-result repl (atom {}))]))

(defn register-console! []
  (let [scrolled? (atom true)]
    (r/render [(with-meta console-view
                 {:component-will-update #(reset! scrolled? (all-scrolled?))
                  :component-did-update #(scroll-to-end! scrolled?)})]
              div)))

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

(defn- render-result [string-result repl-flavor]
  (let [repl (->Evaluator repl-flavor)
        result (to-edn string-result)]
    (swap! out-state conj [:result (render/parse-result result repl (atom {}))])))

(defn- send-response! [{:keys [id result]}]
  (let [callback (get @pending-evals id)]
    (callback result))
  (swap! pending-evals dissoc id))

(defn- patch-result! [{:keys [id result]}]
  (let [repl (->Evaluator :cljs)
        norm {:result (:as-text result)
              :as-text (:as-text result)}
        results (->> @out-state
                     (filter #(-> % first (= :result)))
                     (map second))]
    (doseq [result (conn/find-patch id results)]
      (swap! result assoc :value (render/parse-result norm repl (atom {}))))))

(defn main []
  (.. js/window
      (addEventListener "message"
                        (fn [message]
                          (let [{:keys [command obj repl]} (->> message
                                                                .-data
                                                                (edn/read-string {:default tagged-literal}))]
                            (case command
                              :stdout (append-text :stdout obj)
                              :stderr (append-text :stderr obj)
                              :result (render-result obj repl)
                              :eval-result (send-response! obj)
                              :patch (patch-result! obj)
                              :clear (clear))))))
  (register-console!))
