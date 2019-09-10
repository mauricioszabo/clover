(ns clover.ui
  (:require [cljs.reader :as edn]
            [repl-tooling.editor-helpers :as helpers]
            [repl-tooling.eval :as eval]
            [clover.state :as st]
            ["vscode" :refer [Uri] :as vscode]
            ["path" :as path]))

(defonce view (atom nil))
(defonce curr-dir (atom nil))

(defn- do-render-console! [^js webview curr-dir]
  (let [js-path (. path join curr-dir "view/js/main.js")
        font-path (. path join curr-dir "view/octicons.ttf")
        res-js-path (.. Uri (file js-path) (with #js {:scheme "vscode-resource"}))
        res-font-path (.. Uri (file font-path) (with #js {:scheme "vscode-resource"}))]
    (set! (.. webview -webview -html) (str "<html>
<head>
  <style>
@font-face {
  font-family: 'Octicons Regular';
  src: url(" res-font-path ");
}

div.chlorine.console {
  width: 100%;
  height: 100%;
  background-color: @syntax-background-color;
  color: #c5c8c6;
  overflow: auto;
  font-family: Menlo, Consolas, 'DejaVu Sans Mono', monospace;
  font-size: 14px;
}
  a { color: @text-color-subtle }

div.chlorine.console .items {
  margin-top: 5px;
  margin-bottom: 10px;
}

div.chlorine.console .cell {
  transition: all 0.1s ease;
  position: relative;
  top: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}
div.chlorine.console .cell .gutter {
  width: 2em;
  height: 100%;
  float: left;
  color: @syntax-gutter-text-color;
  text-align: center;
  -webkit-user-select: none;
}

div.chlorine.console .cell .gutter .icon {
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
.chlorine.result a.chevron {
  font-family: 'Octicons Regular';
  font-weight: bold;
  font-size: 16px;
  margin-right: 10px;
  width: 0px;
  height: 15px;
}
.chlorine.result a.chevron.closed::before {
  content: \"\\f078\";
}

.chlorine.result a.chevron.opened::before {
  content: \"\\f0a3\";
}

.chlorine.result div {
  display: flex;
  white-space: pre;
}

.chlorine.result div.browseable,div.row {
  flex-direction: column;
}
.chlorine.result div .children {
  flex-direction: column;
  margin-left: 1.5em;
}

  </style>
</head>
<body>
  <div>Hello, worlda!</div>
  <script type='text/javascript' src='" res-js-path "'></script>
</body>
</html>"))))

(defn- post-message! [message]
  (.. ^js @view -webview
      (postMessage (pr-str message))))

(defn- evaluate! [{:keys [command repl id]}]
  (let [evaluator (if (= repl :clj)
                    (st/repl-for-clj))]
    (eval/evaluate evaluator command {:ignore true}
                   (fn [result]
                     (post-message! {:command :eval-result
                                     :obj {:result result :id id}})))))

(defn- handle-message [{:keys [op args]}]
  (case op
    :eval-code (evaluate! args)))

(defn- listen-to-events! [^js view]
  (.. view -webview (onDidReceiveMessage (comp handle-message edn/read-string))))

(defn create-console! []
  (let [res-path (.. Uri (file (. path join @curr-dir "view")))]
    (reset! view (.. vscode -window
                     (createWebviewPanel "clover-console"
                                         "Clover REPL"
                                         (.. vscode -ViewColumn -Two)
                                         #js {:enableScripts true
                                              :localResourceRoots #js [res-path]})))
    (do-render-console! @view @curr-dir)
    (listen-to-events! @view)))

(defn send-output! [stream text]
  (post-message! {:command stream :obj text}))

(defn send-result! [result repl-flavor]
  (post-message! {:command :result :obj (pr-str result) :repl repl-flavor}))

(defn clear-console! []
  (post-message! {:command :clear}))
