(ns clover.view.console
  (:require [reagent.core :as r]
            [repl-tooling.editor-integration.renderer :as render]
            [cljs.reader :as edn]
            ["ansi_up" :default Ansi]))

(defonce out-state (r/atom []))

(defn- rendered-content [parsed-ratom]
  (let [error? (-> parsed-ratom meta :error)]
    [:div {:class ["result" "chlorine" (when error? "error")]}
     [render/view-for-result parsed-ratom]]))

(defonce ansi (new Ansi))
(defn- cell-for [[out-type object] idx]
  (let [kind (out-type {:stdout :output :stderr :err :result :result})
        icon (out-type {:stdout "icon-quote" :stderr "icon-alert" :result "icon-code"})]
    [:div.cell {:key idx}
     [:div.gutter [:span {:class ["icon" icon]}]]
     (if (= out-type :result)
       [:div.content [rendered-content object]]
       (let [html (. ansi ansi_to_html object)]
         [:div.content [:div {:class kind :dangerouslySetInnerHTML #js {:__html html}}]]))]))

(defn console-view []
  [:div.chlorine.console.native-key-bindings {:tabindex 1}
   [:<> (map cell-for @out-state (range))]])

(defonce div (. js/document querySelector "div"))

(defn- chlorine-elem []
  (. div (querySelector "div.chlorine")))

(defn- all-scrolled? []
  (let [chlorine (chlorine-elem)
        chlorine-height (.-scrollHeight chlorine)
        parent-height (.. div -clientHeight)
        offset (- chlorine-height parent-height)
        scroll-pos (.-scrollTop chlorine)]
    (>= scroll-pos offset)))
(defn- scroll-to-end! [scrolled?]
  (let [chlorine (chlorine-elem)]
    (when @scrolled?
      (set! (.-scrollTop chlorine) (.-scrollHeight chlorine)))))

(defn clear []
  (reset! out-state []))

(defn- append-text [stream text]
  (let [[old-stream old-text] (peek @out-state)]
    (if (= old-stream stream)
      (swap! out-state #(-> % pop (conj [stream (str old-text text)])))
      (swap! out-state conj [stream text]))))

(defn result [parsed-result repl]
  (swap! out-state conj [:result (render/parse-result parsed-result repl)]))

(defn register-console! []
  (let [scrolled? (atom true)]
    (r/render [(with-meta console-view
                 {:component-will-update #(reset! scrolled? (all-scrolled?))
                  :component-did-update #(scroll-to-end! scrolled?)})]
              div)))

(defn main []
  (.. js/window
      (addEventListener "message"
                        (fn [message]
                          (prn :TXT (.-data message) message)
                          (let [{:keys [command obj repl]} (->> message
                                                                .-data
                                                                edn/read-string)]
                            (case command
                              :stdout (append-text :stdout obj)
                              :stderr (append-text :stderr obj)
                              :clear (clear))))))
  (register-console!))
