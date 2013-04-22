(ns srg.utils
  (:require [clojure.tools.logging :as log]
            [srg.protocols :as p]))

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

(defn static? [field]
  (java.lang.reflect.Modifier/isStatic
   (.getModifiers field)))

(defn get-record-field-names [record]
  (->> record
       .getDeclaredFields
       (remove static?)
       (map #(.getName %))
       (remove #{"__meta" "__extmap"})))

(def action-header-changefn-map (atom {}))

(defmacro defplayaction [name header attributes]
  (let [pair-count (/ (count attributes) 2)
        types (into [] (map first (partition 2 attributes)))
        type-fns (into [] (map change-fn types))
        symbols (into [] (map second (partition 2 attributes)))
        symbol-keywrods (into [] (map keyword symbols))]
    `(do
       (swap! action-header-changefn-map assoc ~header (fn [items#]
                                                          (let [type-items# (to-type-items ~types items#)
                                                                map-items# (map vector ~symbol-keywrods type-items#)]
                                                            (into {:action (keyword :guess)} map-items#)))))))

(defn chain-rules
  [room & rule-fns]
  (loop [room-state room
         fs rule-fns
         output nil]
    (if (seq fs)
      (let [f (first fs)
            messages (flatten (f room-state))
            new-room-state (reduce p/update room-state messages)]
        (recur new-room-state (rest fs) (concat output messages)))
      output)))

