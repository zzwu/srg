(ns srg.zjh-json-test
  (:use [srg.zjh-rules])
  (:require [clojure.data.json :as json]))

(defn value-readder
  [k v]
  (if (= :action k)
    (keyword v)
    v))