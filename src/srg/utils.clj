(ns srg.utils
  (:require [clojure.tools.logging :as log]))

(defn to-int
  [s]
  (log/warn :to-int s)
  (Integer/parseInt s))

(defn change-fn
  [type-keyword]
  (case type-keyword
    :int to-int
    :string str identity))

(defn to-type-items
  "以types的数量为准"
  [types items]
  (into []
        (map #((change-fn (get types %)) (get items %))
             (range (count types)))))

(defmacro defhandler [fn-name args messages & body]
    (let [pair-count (/ (count messages) 2)
          types (into [] (map first (partition 2 messages)))
          type-fns (into [] (map #(case % :int to-int :string str identity) types))
          symbols (into [] (map second (partition 2 messages)))
          items (first args)]
      `(defn ~fn-name ~args
         (let [type-items# (to-type-items ~types ~items)
               ~symbols type-items#]
           ~@body))))

(defn gen-msg [header & items]
  (let [str-seq (interpose "\r" (into [header] items))]
    (apply str str-seq)))


