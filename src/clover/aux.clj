(ns clover.aux)

(defmacro add-disposable! [command]
  `(add-disposable* (fn [] ~command)))
(defmacro add-transient! [command]
  `(add-transient-disposable* (fn [] ~command)))