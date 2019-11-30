(ns clover.vs
  (:require ["vscode" :as vscode]))

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
    (info (pr-str mapped))
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
         end (. sel -end)]
     {:editor editor
      :contents (.getText document)
      :filename (.-fileName document)
      :range [[(.-line start) (.-character start)]
              [(.-line end) (.-character end)]]})))
