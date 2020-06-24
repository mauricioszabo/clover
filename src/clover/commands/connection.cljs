(ns clover.commands.connection
  (:require [repl-tooling.editor-integration.connection :as connection]
            [clover.vs :as vs]
            [clover.aux :as aux :include-macros true]
            [clover.ui :as ui]
            [clover.state :as state]
            [clojure.string :as str]
            ["vscode" :as vscode]
            ["path" :as path]
            ["fs" :refer [existsSync readFileSync]]))

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
  (some-> ^js @ui/view .dispose)
  (when-not (empty? @state/state)
    (aux/clear-commands!)
    (aux/clear-transients!)
    (reset! state/state {})
    (vs/info "Disconnected from REPLs")))

(defn- folders []
  (->> (.. vscode -workspace -workspaceFolders)
       (map #(-> % .-uri str (str/replace #"file://" "")))
       vec))

(defn- get-config []
  {:project-paths (folders)
   :eval-mode (-> vscode
                  .-workspace
                  (.getConfiguration "clover")
                  .-repl
                  keyword)})

(defn- notify! [{:keys [type title message]}]
  (let [txt (cond-> title message (str ": " message))]
    (case type
      :info (vs/info txt)
      :warning (vs/warn txt)
      :error (vs/error txt))))

(defn- decide-command [key command experimental?]
  (let [old-cmd (:old-command command)
        new-cmd (:command command)]

    (aux/add-command!
     (.. vscode
         -commands
         (registerCommand (str "clover." (name key))
                          (fn []
                            (if (and old-cmd (not= true experimental?))
                              (old-cmd)
                              (new-cmd))))))))

(defn- register-commands! [commands]
  (let [experimental? (.. vscode -workspace (getConfiguration "clover") -experimental)]
    (aux/clear-commands!)
    (doseq [[k command] commands]
      (decide-command k command experimental?))))

(defn- connect-clj [[host port]]
  (.. (connection/connect!
       host port
       {:on-stdout #(ui/send-output! :stdout %)
        :on-stderr #(ui/send-output! :stderr %)
        :on-disconnect disconnect!
        :prompt vs/choice
        :get-config get-config
        :on-eval #(ui/send-result! % :clj)
        :on-patch #(ui/post-message! {:command :patch :obj %})
        :editor-data vs/get-editor-data
        :register-commands register-commands!
        :notify notify!})
      (then #(when-let [st %]
               (swap! state/state assoc :conn st)
               ; (doseq [[key {:keys [command]}] (-> @st :editor/commands)]
               ;   (aux/add-transient! (.. vscode
               ;                           -commands
               ;                           (registerCommand (str "clover." (name key))
               ;                                            command))))
               (register-console!)))))

(defn- find-shadow-port []
  (->> (folders)
       (map #(path/join % ".shadow-cljs" "socket-repl.port"))
       (filter existsSync)
       first))

(defn connect! []
  (if (state/repl-for-clj)
    (vs/warn "REPL is already connected")
    (.. (vs/prompt "Connect to Clojure: inform <host>:<port>"
                   (str "localhost:" (some-> (find-shadow-port) readFileSync)))
        (then extract-host-port)
        (then #(when % (connect-clj %))))))
