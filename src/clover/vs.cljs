(ns clover.vs
  (:require ["vscode" :as vscode]
            [repl-tooling.editor-helpers :as h]))

(defn info [text]
  (.. vscode -window (showInformationMessage text)))
(defn warn [text]
  (.. vscode -window (showWarningMessage text)))
(defn error [text]
  (.. vscode -window (showErrorMessage text)))

(defn prompt [prompt placeholder]
  (.. vscode -window (showInputBox #js {:prompt prompt
                                        :value placeholder})))

(defn choice [{:keys [message arguments]}]
  (let [mapped (->> arguments (map (juxt :value :key)) (into {}))]
    (.. vscode -window (showQuickPick (->> arguments (map :value) clj->js)
                                      #js {:placeHolder message})
        (then #(mapped %)))))

(defn get-document-data [^js document ^js position]
  {:contents (.getText document)
   :filename (.-fileName document)
   :range [[(.-line position) (.-character position)]]})

(defn get-editor-data
  ([] (get-editor-data (.. vscode -window -activeTextEditor)))
  ([^js editor]
   (let [document (. editor -document)
         sel (. editor -selection)
         start (. sel -start)
         end (. sel -end)
         start [(.-line start) (.-character start)]
         end [(.-line end) (.-character end)]
         end (cond-> end (not= start end) (update 1 dec))]
     {:editor editor
      :contents (.getText document)
      :filename (.-fileName document)
      :range [start end]})))

(defn run-command [command & args]
  (let [run! (.. vscode -commands -executeCommand)]
    (apply run! command args)))
