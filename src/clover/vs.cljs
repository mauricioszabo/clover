(ns clover.vs
  (:require ["vscode" :as vscode]))

(defn info [text]
  (.. vscode -window (showInformationMessage text)))
(defn warn [text]
  (.. vscode -window (showWarningMessage text)))
(defn error [text]
  (.. vscode -window (showErrorMessage text)))

(defn prompt [prompt placeholder]
  (.. vscode -window (showInputBox #js {:prompt prompt
                                        :value placeholder})))
