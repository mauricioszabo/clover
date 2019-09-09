(ns clover.ui
  (:require ["vscode" :refer [Uri]]
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
  </style>
</head>
<body>
  <div>Hello, worlda!</div>
  <script type='text/javascript' src='" res-js-path "'></script>
</body>
</html>"))))

(defn render-console! []
  (do-render-console! @view @curr-dir))

(defn send-output! [stream text]
  (.. ^js @view -webview
      (postMessage (pr-str {:command stream :obj text}))))
