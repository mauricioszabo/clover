(ns clover.ui
  (:require [clojure.edn :as edn]
            [promesa.core :as p]
            [repl-tooling.editor-helpers :as helpers]
            [repl-tooling.editor-integration.connection :as connection]
            [repl-tooling.eval :as eval]
            [clover.state :as st]
            ["vscode" :refer [Uri] :as vscode]
            ["path" :as path]))

(defonce view (atom nil))
(defonce curr-dir (atom nil))

(defn- do-render-console! [^js webview curr-dir]
  (let [js-path (. path join curr-dir "view" "js" "main.js")
        font-path (. path join curr-dir "view" "octicons.ttf")
        res-js-path (.. webview -webview (asWebviewUri (. Uri file js-path)))
        res-font-path (.. webview -webview (asWebviewUri (. Uri file font-path)))]
    (set! (.. webview -webview -html) (str "<html>
<head>
  <style>
@font-face {
  font-family: 'Octicons Regular';
  src: url(" res-font-path ");
}

div.repl-tooling.console {
  width: 100%;
  height: 100%;
  color: var(--vscode-editor-foreground);
  overflow: auto;
  font-family: Menlo, Consolas, 'DejaVu Sans Mono', monospace;
  font-size: 14px;
}

a {
  color: var(--vscode-icon-foreground);
  text-decoration: none;
}
a:hover {
  color: var(--vscode-icon-foreground);
  text-decoration: underline;
}

div.repl-tooling.console .items {
  margin-top: 5px;
  margin-bottom: 10px;
}

div.repl-tooling.error {
  color: var(--vscode-errorForeground);
}

div.repl-tooling div.exception div.description {
  font-weight: bold;
}

div.repl-tooling div.exception div.additional {
  margin-left: 0;
}

div.repl-tooling div.exception div.stack {
  opacity: 0.6;
}

div.repl-tooling.console .cell {
  transition: all 0.1s ease;
  position: relative;
  top: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}
div.repl-tooling.console .cell .gutter {
  width: 2em;
  height: 100%;
  float: left;
  color: @syntax-gutter-text-color;
  text-align: center;
  -webkit-user-select: none;
}

div.repl-tooling.console .cell .gutter .icon {
  transition: all 0.1s ease;
}

span.icon {
  font-family: 'Octicons Regular';
  width: 16px;
  height: 16px;
}
.icon-code::before {
  content: \"\\f05f\";
}
.icon-quote::before {
  content: \"\\f063\";
}
.icon-alert::before {
  content: \"\\f02d\";
}

.content {
  margin-left: 2em;
  padding-left: 5px;
  padding-right: 20px;
}
.content .output,.err,.result {
  border-bottom: 1px solid rgba(171, 178, 191, 0.25);
  margin-top: 3px;
  padding-bottom: 4px;
}
.err {
  color: #fa0000;
}
.info {
  color: @text-color-info;
}

/* RESULTS RENDERER */
.repl-tooling.result a.chevron {
  font-family: 'Octicons Regular';
  font-weight: bold;
  font-size: 16px;
  margin-right: 10px;
  width: 0px;
  height: 15px;
}
.repl-tooling.result a.chevron.closed::before {
  content: \"\\f078\";
}

.repl-tooling.result a.chevron.opened::before {
  content: \"\\f0a3\";
}

.repl-tooling.result div {
  display: flex;
  white-space: pre;
}

/* Interactive Results */
.repl-tooling.result div.browseable,div.row {
  flex-direction: column;
}
.repl-tooling.result .children {
  flex-direction: column;
  margin-left: 1.5em;
}
.repl-tooling.result div.error {
  color: var(--vscode-errorForeground);
}
.repl-tooling.result div.rows {
  display: flex;
  flex-direction: column;
}
.repl-tooling.result div.cols {
  display: flex;
  flex-direction: row;
}
.repl-tooling.result div.title {
  font-weight: 800;
}
.repl-tooling.result div.pre {
  white-space: pre-wrap;
}
.repl-tooling.result div.space {
  opacity: 0.1;
  margin: 0.6em;
}
.repl-tooling.result select {
}
.repl-tooling.result button {
  padding-left: 0.5em;
  padding-right: 0.5em;
  border-width: 1px;
  border-radius: 3px;
}
.repl-tooling.result div input[type=text] {
  border: 0px @syntax-selection-color;
  border-radius: 1px;
  border: 0px solid @pane-item-border-color;
}


  </style>
</head>
<body>
  <div></div>
  <script type='text/javascript' src='" res-js-path "'></script>
</body>
</html>"))))

(defn post-message! [message]
  (some-> ^js @view
          .-webview
          (.postMessage (pr-str message))))

(defn- evaluate! [{:keys [command repl id]}]
  (when-let [evaluator (if (= repl :clj)
                         (st/repl-for-clj))]
    (eval/evaluate evaluator command {:ignore true}
                   (fn [result]
                     (post-message! {:command :eval-result
                                     :obj {:result result :id id}})))))

(defn- resolve-result! [id call cmd args]
  (p/let [result (apply call cmd args)]
    (post-message! {:command :call-result
                    :obj {:result result :id id}})))

(defn- handle-message [{:keys [op args cmd id]}]
  (when-let [editor-state (-> @st/state :conn)]
    (let [{:keys [run-callback run-feature]} @editor-state]
      (case op
        :eval-code (evaluate! args)
        :run-callback (resolve-result! id run-callback cmd args)
        :run-feature (resolve-result! id run-feature cmd args)))))

(defn- listen-to-events! [^js view]
  (.. view (onDidDispose connection/disconnect!))
  (.. view -webview (onDidReceiveMessage (comp handle-message edn/read-string))))

(defn create-console! []
  (let [res-path (.. Uri (file (. path join @curr-dir "view")))]
    (reset! view (.. vscode -window
                     (createWebviewPanel "clover-console"
                                         "Clover REPL"
                                         (.. vscode -ViewColumn -Two)
                                         (clj->js
                                          (cond-> {:enableScripts true
                                                   :retainContextWhenHidden true
                                                   :localResourceRoots [res-path]}

                                            js/goog.DEBUG
                                            (assoc
                                             :portMapping [{:extensionHostPort 9699
                                                            :webviewPort 9699}]))))))

    (do-render-console! @view @curr-dir)
    (listen-to-events! @view)))

(defn send-output! [stream text]
  (post-message! {:command stream :obj text}))

(defn send-result! [{:keys [result]} repl-flavor]
  (let [norm-result (cond-> result
                            (contains? result :result) (assoc :result true)
                            (contains? result :error) (assoc :error true))]
    (post-message! {:command :result
                    :obj (pr-str norm-result)
                    :repl repl-flavor})))

(defn clear-console! []
  (post-message! {:command :clear}))
