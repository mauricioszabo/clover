(ns clover.core
  (:require [clover.aux :as aux :include-macros true]
            [clover.vs :as vs]
            [clover.state :as st]
            [clover.ui :as ui]
            [clover.commands.connection :as conn]
            [repl-tooling.features.definition :as definition]
            [repl-tooling.editor-helpers :as helpers]
            ["vscode" :as vscode :refer [Location Uri Position Range TextEdit]]
            ["path" :as path]))

(defn- var-definition [document position]
  (let [data (vs/get-document-data document position)
        [_ curr-var] (helpers/current-var (:contents data) (-> data :range first))
        [_ curr-ns] (helpers/ns-range-for (:contents data) (-> data :range first))]
    (.. (definition/find-var-definition (st/repl-for-aux) curr-ns curr-var)
        (then (fn [{:keys [file-name line]}]
                (Location. (. Uri parse file-name) (Position. line 0)))))))

(def icons
  (let [vs-icons (-> vscode .-CompletionItemKind (js->clj :keywordize-keys true))]
    {:method (:Method vs-icons)
     :field (:Field vs-icons)
     :static-method (:Function vs-icons)
     :static-field (:Field vs-icons)
     :local (:Variable vs-icons)
     :class (:Class vs-icons)
     :namespace (:Module vs-icons)
     :keyword (:Property vs-icons)
     :protocol-function (:Function vs-icons)
     :function (:Function vs-icons)
     :record (:Struct vs-icons)
     :type (:TypeParameter vs-icons)
     :protocol (:Interface vs-icons)
     :var (:Constant vs-icons)
     :macro (:Keyword vs-icons)
     :resource (:Reference vs-icons)
     :special-form (:Constructor vs-icons)
     :value (:Value vs-icons)}))

(defn- range-to-replace []
  (let [{:keys [contents range]} (vs/get-editor-data)
        [[s-row s-col] [e-row e-col]] (first (helpers/current-var contents (first range)))]
    (Range. s-row s-col e-row e-col)))

(defn- autocomplete [ & args]
  (when-let [complete (some-> @st/state :conn deref :editor/features :autocomplete)]
    (.. (complete)
        (then (fn [candidates]
                (let [range (range-to-replace)]
                  (map (fn [{:keys [type candidate] :as a}]
                         {:label candidate
                          :kind (icons type (:value icons))
                          :filterText candidate
                          :range range})
                       candidates))))
        (then clj->js)
        (catch (constantly #js [])))))

(defn activate [^js ctx]
  (when ctx (reset! ui/curr-dir (.. ctx -extensionPath)))
  (aux/add-disposable! (.. vscode -commands
                           (registerCommand "extension.connectSocketRepl"
                                            conn/connect!)))
  (aux/add-disposable! (.. vscode -languages
                           (registerDefinitionProvider
                            "clojure"
                            #js {:provideDefinition var-definition})))

  (aux/add-disposable! (.. vscode -languages
                           (registerCompletionItemProvider
                            "clojure"
                            #js {:provideCompletionItems autocomplete}))))

(defn deactivate []
  (aux/clear-all!))

(defn before [done]
  (deactivate)
  (done))

(defn after []
  (activate nil)
  (vs/info "Reloaded Clover")
  (println "Reloaded"))
