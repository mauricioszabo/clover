(ns clover.core
  (:require ["vscode" :as vscode]))

(def ^:private cmds (atom []))

(defn activate [_ctx]
  (reset! cmds [])
  (prn :ACTIVATE!)
  (let [disposable (.. vscode -commands
                       (registerCommand "extension.helloWorld"
                                        #(.. vscode -window
                                             (showInformationMessage "Hello, Dude!"))))]
    (swap! cmds conj disposable)))

(defn deactivate []
  (doseq [cmd @cmds]
    (.dispose ^js cmd)))

(def commands
  (clj->js {:foo 10
            :bar 20
            :lol (fn [] 10)
            :activate (fn [] activate)}))
  ; (fn []
  ;   (clj->js {:activate activate
  ;             :deactivate deactivate})))

(defn before [done]
  (deactivate)
  (done))

(defn after []
  (activate nil)
  (.. vscode -window (showInformationMessage "Reloaded Clover"))
  (println "Reloaded"))
