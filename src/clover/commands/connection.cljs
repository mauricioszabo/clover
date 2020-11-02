(ns clover.commands.connection
  (:require [repl-tooling.editor-integration.connection :as connection]
            [repl-tooling.editor-integration.configs :as configs]
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
             :onDidWrite prn
             :close prn
             :handleInput prn
             :open (fn [& _]
                     (command)
                     (.fire ^js emitter))
             :setDimensions prn}))))

(defn- create-task [command-key command-name function]
  (doto (new Task
          #js {:type "clover" :command (name command-key)}
          (.. vscode -TaskScope -Workspace)
          command-name
          "clover"
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

(aux/add-disposable!
  (.. vscode -tasks
      (registerTaskProvider "clover"
                            provider)))

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
    "goto-var-definition"})

(defn- register-commands! [commands]
  (let [experimental? (.. vscode -workspace (getConfiguration "clover") -experimental)]
    (aux/clear-commands!)
    (reset! custom-commands {})

    (doseq [[k command] commands]
      (if (original-cmds (name k))
        (decide-command k command experimental?)
        (swap! custom-commands assoc k command)))))

(defn- open-editor [{:keys [file-name line column]}]
  (p/let [doc (.. vscode -workspace (openTextDocument file-name))]
    (.. vscode -window (showTextDocument doc))))

(defn- connect-clj [[host port]]
  (let [config-file (path/join (.homedir os) ".config" "clover" "config.cljs")]
    (when-not (fs/existsSync config-file)
      (try (fs/mkdirSync (path/dirname config-file)) (catch :default _))
      (fs/closeSync (fs/openSync config-file "w")))

    (.. (connection/connect!
         host port
         {:on-stdout #(ui/send-output! :stdout %)
          :on-stderr #(ui/send-output! :stderr %)
          :on-disconnect disconnect!
          :prompt vs/choice
          :get-config get-config
          :open-editor open-editor
          :config-file-path config-file
          :on-eval #(ui/send-result! % :clj)
          :on-patch #(ui/post-message! {:command :patch :obj %})
          :editor-data vs/get-editor-data
          :register-commands register-commands!
          :notify notify!})
        (then #(when-let [st %]
                 (swap! state/state assoc :conn st)
                 (register-console!))))))

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
