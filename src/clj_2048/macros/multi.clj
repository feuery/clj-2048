(ns clj-2048.macros.multi)

(defmacro def-real-multi [Name params fn]
  (let [symbolname (name Name)
        dispatcher-symbol (symbol (str symbolname "-dispatcher"))]
  `(do
     (defn ~dispatcher-symbol ~params ~fn)
     (defmulti ~Name (var ~dispatcher-symbol)))))
