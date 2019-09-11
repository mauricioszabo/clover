(ns clover.commands.connection
  (:require [repl-tooling.editor-integration.connection :as connection]
            [clover.vs :as vs]
            [clover.aux :as aux :include-macros true]
            [clover.ui :as ui]
            [clover.state :as state]
            [clojure.string :as str]
            ["vscode" :as vscode]))

(defn- extract-host-port [txt]
  (let [[host port] (str/split txt #":")
        port (js/parseInt port)]
    (if (js/isNaN port)
      (do (vs/error "Port must be a number") nil)
      [host port])))

(defn- register-console! []
  (ui/create-console!)
  (aux/add-transient! (.. vscode -commands
                          (registerCommand "clover.clear-console"
                                           ui/clear-console!))))

(defn- disconnect! []
  (connection/disconnect!)
  (aux/clear-transients!)
  (reset! state/state {})
  (vs/info "Disconnected from REPLs"))

(defn- connect-clj [[host port]]
  (.. (connection/connect-unrepl!
       host port
       {:on-stdout #(ui/send-output! :stdout %)
        :on-stderr #(ui/send-output! :stderr %)
        :on-result #(ui/send-result! % :clj)
        :on-disconnect disconnect!
        :on-start-eval vs/info
        :on-eval vs/info
        :editor-data vs/get-editor-data
        :get-config vs/info
        :notify vs/info})
      (then (fn [st]
              (swap! state/state assoc :conn st
                     (register-console!)
                     (doseq [[key {:keys [command]}] (-> @st :editor/commands)]
                       (aux/add-transient! (.. vscode
                                               -commands
                                               (registerCommand (str "clover." (name key))
                                                                command))))

                     (vs/info "Clojure REPL connected"))))))


(defn connect! []
  (if (state/repl-for-clj)
    (vs/warn "REPL is already connected")
    (.. (vs/prompt "Connect to Clojure: inform <host>:<port>"
                   "localhost:4444")
        (then extract-host-port)
        (then #(when % (connect-clj %))))))
