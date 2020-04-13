(ns clover.commands.formatter
  (:require [clover.vs :as vs]
            [clojure.string :as str]
            [repl-tooling.editor-helpers :as helpers]
            ["parinfer" :as par]
            ["vscode" :as vscode :refer [Range TextEdit Position]]
            [cljfmt.core :as format]))

(defn- spaces [n]
  (->> n
       range
       (map (constantly " "))
       (str/join "")))

(defn- indent-idx [contents row col]
  (let [bizarre-str (pr-str (str (rand) "-" (rand)))
        [[[brow bcol]] block-txt] (helpers/block-for contents [row col])
        splitted (str/split-lines block-txt)
        diff-row (- row brow)]
    (-> splitted
        (update 0 #(str (spaces bcol) %))
        (update diff-row #(str (subs % 0 col) bizarre-str (subs % col)))
        (->> (str/join "\n"))
        format/reformat-string
        str/split-lines
        (get diff-row)
        (.indexOf bizarre-str))))

(defn- format-on-type [document]
  (try
    (let [{:keys [contents range]} (vs/get-editor-data)
          [[srow scol] [erow ecol]] range
          indent-idx (indent-idx contents srow scol)]
      (if (< indent-idx scol)
        #js [(.. TextEdit (delete (Range. srow indent-idx srow scol)))]
        #js [(.. TextEdit (insert (Position. srow scol) (spaces (- indent-idx scol))))]))
    (catch :default e
      nil)))

(def formatter #js {:provideOnTypeFormattingEdits format-on-type})
