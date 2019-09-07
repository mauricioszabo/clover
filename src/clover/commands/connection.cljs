(ns clover.commands.connection
  (:require [repl-tooling.editor-integration.connection :as connection]
            [clover.vs :as vs]
            [clover.aux :as aux]
            [clojure.string :as str]
            ["vscode" :as vscode]))

(defn- get-editor-data []
  (let [current-editor (.. vscode -window -activeTextEditor)
        document (. current-editor -document)
        sel (. current-editor -selection)
        start (. sel -start)
        end (. sel -end)]
    {:editor current-editor
     :contents (.getText document)
     :filename (.-fileName document)
     :range [[(.-line start) (.-character start)]
             [(.-line end) (.-character end)]]}))

(defn- extract-host-port [txt]
  (let [[host port] (str/split txt #":")
        port (js/parseInt port)]
    (if (js/isNaN port)
      (do (vs/error "Port must be a number") nil)
      [host port])))

(defn- connect-clj [[host port]]
  (.. (connection/connect-unrepl!
       host port
       {:on-stdout vs/info
        :on-stderr vs/info
        :on-result #(prn :RESULT %)
        :on-disconnect vs/info
        :on-start-eval vs/info
        :on-eval vs/info
        :editor-data get-editor-data
        :get-config vs/info
        :notify vs/info})
    (then (fn [st]
            (doseq [[key {:keys [command]}] (-> @st :editor/commands)]
              (prn :REGISTERING key command)
              (aux/add-disposable! (.. vscode
                                       -commands
                                       (registerCommand (str "clover." (name key))
                                                        command))))

            (vs/info "Clojure REPL connected")))))


(defn connect! []
  (.. (vs/prompt "Connect to Clojure: inform <host>:<port>"
                 "localhost:4444")
      (then extract-host-port)
      (then #(when % (connect-clj %)))))
