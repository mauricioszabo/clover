(ns clover.aux
  (:require ["vscode" :as vscode]))

(def ^:private cmds (atom []))

(defn add-disposable* [fun]
  (swap! cmds conj (fun)))

(defn clear-all! []
  (doseq [cmd @cmds]
    (.dispose ^js cmd)))
