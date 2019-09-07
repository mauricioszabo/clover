(ns clover.core
  (:require [clover.aux :as aux :include-macros true]
            [clover.vs :as vs]
            [clover.commands.connection :as conn]
            ["vscode" :as vscode]))

(defn activate [_ctx]
  (aux/add-disposable! (.. vscode -commands
                           (registerCommand "extension.connectSocketRepl"
                                            conn/connect!))))

(defn deactivate []
  (aux/clear-all!))

(defn before [done]
  (deactivate)
  (done))

(defn after []
  (activate nil)
  (vs/info "Reloaded Clover")
  (println "Reloaded"))
