(ns clover.core
  (:require [clover.aux :as aux :include-macros true]
            [clover.vs :as vs]
            [clover.state :as st]
            [clover.ui :as ui]
            [clover.commands.connection :as conn]
            [repl-tooling.features.definition :as definition]
            [repl-tooling.editor-helpers :as helpers]
            ["vscode" :as vscode :refer [Location Uri Position]]
            ["path" :as path]))

(defn- var-definition [document position]
  (let [data (vs/get-document-data document position)
        curr-var (helpers/current-var (:contents data) (-> data :range first))
        [_ curr-ns] (helpers/ns-range-for (:contents data) (-> data :range first))]
    (.. (definition/find-var-definition (st/repl-for-aux) curr-ns curr-var)
        (then (fn [{:keys [file-name line]}]
                (Location. (. Uri parse file-name) (Position. line 0)))))))

(defn activate [^js ctx]
  (when ctx (reset! ui/curr-dir (.. ctx -extensionPath)))
  (aux/add-disposable! (.. vscode -commands
                           (registerCommand "extension.connectSocketRepl"
                                            conn/connect!)))
  (aux/add-disposable! (.. vscode -languages
                           (registerDefinitionProvider
                            "clojure"
                            #js {:provideDefinition var-definition}))))

(defn deactivate []
  (aux/clear-all!))

(defn before [done]
  (deactivate)
  (done))

(defn after []
  (activate nil)
  (vs/info "Reloaded Clover")
  (println "Reloaded"))
