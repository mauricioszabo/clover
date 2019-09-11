(ns clover.state)

(defonce state (atom {}))

(defn repl-for-clj []
  (some-> @state :conn deref :clj/repl))
(defn repl-for-aux []
  (some-> @state :conn deref :clj/aux))
; (defn repl-for-clj []
;   (some-> @state :conn deref :clj/repl))
