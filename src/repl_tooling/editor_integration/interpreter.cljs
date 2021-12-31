(ns repl-tooling.editor-integration.interpreter
  (:require [sci.core :as sci]
            [clojure.set :as set]
            [promesa.core :as p]
            [paprika.collection :as coll]
            [clojure.string :as str]
            [repl-tooling.editor-integration.commands :as cmds]
            [repl-tooling.editor-helpers :as helpers]
            [sci.impl.namespaces :as sci-ns]
            [repl-tooling.ui.pinkie :as pinkie]
            [pinkgorilla.ui.jsrender :as jsrender]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [repl-tooling.commands-to-repl.pathom :as pathom]
            ["path" :refer [dirname join]]
            ["fs" :refer [watch readFile existsSync]]
            ["ansi_up" :default Ansi]))

(defn- read-config-file [config-file]
  (let [p (p/deferred)]
    (readFile config-file #(p/resolve! p (str %2)))
    p))

(defn- name-for [k]
  (-> k name str/capitalize (str/replace #"-" " ")))

(def ^:private promised-let
  ^:sci/macro
  (fn [_&form _&env bindings & body]
    (let [binds (->> bindings (partition-all 2 2) reverse)]
      (loop [body (cons 'do body)
             [[var elem] & rest] binds]
        (if (nil? var)
          body
          (recur
            (list 'p/then (list 'promise elem) (list 'fn [var] body))
            rest))))))

(defn- find-repl [state]
  (p/let [data (cmds/run-callback! state :editor-data)]
    (cmds/run-feature! state :repl-for (:filename data) true)))

(defn- interactive-eval [state params]
  (set! helpers/*out-on-aux* true)
  (-> state
      (cmds/run-feature! :evaluate-and-render
                         (update params :pass assoc
                                 :interactive true
                                 :aux true))
      (p/finally (fn [ & _] (set! helpers/*out-on-aux* false)))))

(defn- editor-ns [repl state]
  (let [repl (delay (or repl (find-repl state)))]
    (cond->
      {'run-callback (partial cmds/run-callback! state)
       'run-feature (fn [cmd & args]
                      (p/let [curr-repl @repl]
                        (if (= cmd :go-to-var-definition)
                          (cmds/run-feature! state
                                             :go-to-var-definition
                                             (assoc (first args)
                                                    :repl curr-repl))
                          (apply cmds/run-feature! state cmd args))))
       'get-top-block #(cmds/run-feature! state :get-code :top-block)
       'get-block #(cmds/run-feature! state :get-code :block)
       'get-var #(cmds/run-feature! state :get-code :var)
       'get-selection #(cmds/run-feature! state :get-code :selection)
       'get-namespace #(p/let [res (cmds/run-feature! state :get-code :ns)]
                         (update res :text str))
       'eval-and-render #(cmds/run-feature! state :evaluate-and-render %)
       'eval-interactive #(interactive-eval state %)
       'eval (partial cmds/run-feature! state :eval)
       'add-resolver pathom/add-resolver
       'compose-resolver pathom/compose-resolver}

      (contains? (:editor/callbacks @state) :run-command)
      (assoc 'run-command (partial cmds/run-callback! state :run-command)))))

(defn- norm-reagent-fn [fun]
  (fn [ & args]
    (let [empty (js/Object.)
          state (r/atom empty)
          render (fn [ state & args]
                   (if (= empty @state)
                     (do
                       (p/let [res (apply fun args)]
                         (reset! state res))
                       [:div.repl-tooling.icon.loading])
                     @state))]
      (apply vector render state args))))

(defn- norm-pinkie-fn [fun]
  (fn [ & args]
    [jsrender/render-js
     {:f (fn [dom args]
           (let [div (.createElement js/document "div")
                 upd (fn [elem]
                       (try (.removeChild dom div) (catch :default _))
                       (.appendChild dom elem))
                 elem (apply fun (js->clj args))]
             (.. div -classList (add "repl-tooling" "icon" "loading"))
             (.appendChild dom div)
             (if (instance? js/Promise elem)
               (.then elem upd)
               (upd elem))))
      :data args}]))

(defn- render-ns [editor-state]
  {'js-require #(-> @editor-state
                    :editor/callbacks
                    :config-file-path
                    dirname
                    (join %)
                    js/require)
   'create-tag #(.createElement js/document %)
   'set-text #(aset %1 "innerText" %2)
   'set-html #(aset %1 "innerHTML" %2)
   'add-class (fn [^js e & args]
                (doseq [a args]
                  (.. e -classList (add a))))
   'set-attr (fn [^js e attr value]
              (.setAttribute e attr value))
   'register-reagent #(if (and (keyword? %1) (namespace %1) (fn? %2))
                        (pinkie/register-tag %1 (norm-reagent-fn %2))
                        (cmds/run-callback!
                         editor-state
                         :notify
                         {:type :error
                          :title "Invalid params"
                          :text (str "First argument needs to be a namespaced keyword, "
                                     "and second argument needs to be a reagent fn")}))
   'register-tag #(if (and (keyword? %1) (namespace %1) (fn? %2))
                    (pinkie/register-tag %1 (norm-pinkie-fn %2))
                    (cmds/run-callback!
                     editor-state
                     :notify
                     {:type :error
                      :title "Invalid params"
                      :text (str "First argument needs to be a namespaced keyword, "
                                 "and second argument needs to be a function that "
                                 "returns a HTML tag")}))})

(defn- prepare-nses [repl editor-state]
  (-> sci-ns/namespaces
      (set/rename-keys '{clojure.string str
                         clojure.set set
                         clojure.walk walk
                         clojure.template template
                         clojure.repl repl
                         clojure.edn edn})
      (assoc 'r {'atom r/atom
                 'render rdom/render
                 'adapt-react-class r/adapt-react-class
                 'as-element r/as-element
                 'create-class r/create-class
                 'create-element r/create-element
                 'current-component r/current-component
                 'cursor r/cursor
                 'is-client r/is-client
                 'reactify-component r/reactify-component
                 'wrap r/wrap})
      (assoc 'render (render-ns editor-state))
      (assoc 'editor (editor-ns repl editor-state))
      (assoc 'repl-tooling.editor-helpers {'Error helpers/Error})))

(def ^:private promised-bindings {'promise #(.resolve js/Promise %)
                                  'p/then #(.then ^js %1 %2)
                                  'p/catch #(.catch ^js %1 %2)
                                  'p/let promised-let})

(defn debug-bindings [editor-state]
  (let [run-callback (:run-callback @editor-state)]
    {'println (fn [& args]
                (run-callback :on-stdout (str (str/join " " args) "\n"))
                nil)
     'print (fn [& args]
              (run-callback :on-stdout (str/join " " args))
              nil)
     'prn (fn [& args]
            (->> args
                 (map pr-str)
                 (str/join " ")
                 (#(str % "\n"))
                 (run-callback :on-stdout))
            nil)
     'log (fn [& args] (apply js/console.log args))
     'pr (fn [& args]
           (->> args (map pr-str)
                (str/join " ")
                (run-callback :on-stdout))
           nil)}))

(defn- readers-for [editor-state]
  {'repl-tooling.editor-helpers.Error helpers/map->Error
   'repl-tooling.editor-helpers.Browseable helpers/map->Browseable
   'repl-tooling.editor-helpers.Patchable helpers/map->Patchable
   'repl-tooling.editor-helpers.IncompleteObj helpers/map->IncompleteObj})

(defn evaluate-code [{:keys [code bindings sci-state editor-state repl]
                      :or {sci-state (atom {})}}]
  (let [bindings (cond
                   editor-state (merge promised-bindings
                                       (debug-bindings editor-state)
                                       bindings)
                   :else promised-bindings)]
    (sci/eval-string code {:env sci-state
                           :classes {:allow :all}
                           :preset {:termination-safe true}
                           :readers (readers-for editor-state)
                           :namespaces (prepare-nses repl editor-state)
                           :bindings bindings})))
