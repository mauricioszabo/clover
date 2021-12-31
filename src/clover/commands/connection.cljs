(ns clover.commands.connection
  (:require [repl-tooling.editor-integration.connection :as connection]
            [repl-tooling.editor-integration.configs :as configs]
            [repl-tooling.editor-helpers :as helpers]
            [clover.vs :as vs]
            [clover.aux :as aux :include-macros true]
            [clover.ui :as ui]
            [clover.state :as state]
            [clojure.string :as str]
            [promesa.core :as p]
            ["vscode" :as vscode :refer [Task TaskDefinition]]
            ["path" :as path]
            ["os" :as os]
            ["fs" :refer [existsSync readFileSync] :as fs]))

(defn- create-pseudo-terminal [command]
  (let [emitter (-> vscode .-EventEmitter new)]
    (new (. vscode -CustomExecution)
      (fn [_]
        #js {:onDidClose (.-event emitter)
             :onDidWrite identity
             :close identity
             :handleInput identity
             :open (fn [& _]
                     (command)
                     (.fire ^js emitter))
             :setDimensions identity}))))

(defn- create-task [command-key command-name function]
  (doto (new Task
          #js {:type "Clover" :command (name command-key)}
          (.. vscode -TaskScope -Workspace)
          command-name
          "Clover"
          (create-pseudo-terminal function))
        (aset "presentationOptions"
              #js {:reveal (.. vscode -TaskRevealKind -Never)
                   :clear true})))

(defonce custom-commands (atom {}))

(def provider
  #js {:provideTasks (fn [_]
                       (->> @custom-commands
                            (map (fn [[k v]]
                                   (create-task k (:name v) (:command v))))
                            clj->js))
       :resolveTask (fn [_ _])})

(defn- register-console! []
  (ui/create-console!)
  (aux/add-transient! (.. vscode
                          -commands
                          (registerCommand "clover.clear-console"
                                           ui/clear-console!))))

(defn- disconnect! []
  (connection/disconnect!)
  (some-> ^js @ui/view .dispose)
  (reset! ui/view nil)
  (when-not (empty? @state/state)
    (aux/clear-commands!)
    (aux/clear-transients!)
    (swap! state/state dissoc :conn)
    (vs/info "Disconnected from REPLs")))

(defn- folders []
  (->> (.. vscode -workspace -workspaceFolders)
       (map #(-> % .-uri str (str/replace #"file://" "")))
       vec))

(defn- config-dir []
  (let [config (-> vscode
                   .-workspace
                   (.getConfiguration "clover")
                   .-configFile
                   not-empty
                   (or (path/join "$HOME" ".config" "clover" "config.cljs")))]
    (str/replace config #"\$HOME" (.homedir os))))

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

(def ^:private original-cmds
  #{"evaluate-block"
    "evaluate-top-block"
    "evaluate-selection"
    "clear-console"
    "disconnect"
    "load-file"
    "break-evaluation"
    "doc-for-var"
    "run-tests-in-ns"
    "run-test-for-var"
    "source-for-var"
    "connect-embedded"
    "open-config"
    "go-to-var-definition"})

(defn- register-commands! [commands]
  (let [experimental? (.. vscode -workspace (getConfiguration "clover") -experimental)]
    (aux/clear-commands!)
    (reset! custom-commands {})

    (doseq [[k command] commands]
      (if (original-cmds (name k))
        (decide-command k command experimental?)
        (swap! custom-commands assoc k command)))))

(defn- open-editor [{:keys [file-name line column]}]
  (p/let [doc (.. vscode -workspace (openTextDocument file-name))
          editor (.. vscode
                     -window
                     (showTextDocument doc #js {:viewColumn (.. vscode -ViewColumn -One)
                                                :preview true}))

          Sel (.. vscode -Selection)
          selection (new Sel
                      (or line 0) (or column 0)
                      (or line 0) (or column 0))]
    (aset editor "selection" selection)
    (.revealRange ^js editor selection)))

(defn- mkdir-p [directory]
  (let [parent (path/dirname directory)]
    (when-not (fs/existsSync directory)
      (when-not (fs/existsSync parent)
        (mkdir-p parent))
      (fs/mkdirSync directory))))

(defn- connect-clj [[host port]]
  (let [config-file (config-dir)]
    (when-not (fs/existsSync config-file)
      (try
        (mkdir-p (path/dirname config-file))
        (fs/closeSync (fs/openSync config-file "w"))
        (catch :default _)))

    (.. (connection/connect! host port
                             {:on-stdout #(ui/send-output! :stdout %)
                              :on-stderr #(ui/send-output! :stderr %)
                              :on-disconnect disconnect!
                              :prompt vs/choice
                              :get-config get-config
                              :open-editor open-editor
                              :run-command vs/run-command
                              :config-file-path config-file
                              :on-eval #(ui/send-result! % :clj)
                              :on-patch #(ui/post-message! {:command :patch :obj %})
                              :editor-data vs/get-editor-data
                              :register-commands register-commands!
                              :notify notify!})
        (then #(when-let [st %]
                 (swap! state/state assoc :conn st)
                 (register-console!))))))

(defn- extract-host-port [txt]
  (let [[host port] (str/split txt #":")
        port (js/parseInt port)]
    (if (js/isNaN port)
      (do (vs/error "Port must be a number") nil)
      (do
        (swap! state/state assoc :old-conn-info txt :old-port port)
        [host port]))))

(defn connect! []
  (let [nrepl? (.. vscode -workspace (getConfiguration "clover") -detectNreplPort)
        port (helpers/get-possible-port (folders) nrepl? (:old-port @state/state))
        conn-info (if port
                    (str "localhost:" port)
                    (:old-conn-info @state/state "localhost:"))]
    (if (state/repl-for-clj)
      (vs/warn "REPL is already connected")
      (.. (vs/prompt "Connect to Clojure: inform <host>:<port>"
                     conn-info)
          (then extract-host-port)
          (then #(some-> % connect-clj))))))
