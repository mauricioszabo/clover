(ns clover.aux)

(defmacro add-disposable! [command]
  `(add-disposable* (fn [] ~command)))
