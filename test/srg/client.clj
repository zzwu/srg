(ns srg.client
  (:refer-clojure :exclude [send])
  (:use [lamina.core]
        [aleph.tcp]
        [gloss.core]
        [gloss.io]
        [aleph.netty core]
        [srg.utils])
  (:require [clojure.tools.logging :as log]
            [srg.login :as login]
            [srg.game :as game]
            [srg.session :as session]
            [clojure.data.json :as json]))

(def fr
  (finite-frame :int32 (string :utf-8)))

(defn connect
  [fr]
  (wait-for-result
    (tcp-client {:host "localhost",
                 :port 10000,
                 :frame fr})))

(defn start-reveive
  [ch]
  (loop []
    (prn (wait-for-message ch))
    (recur)))

(def ch (connect fr))

(.start (Thread. #(start-reveive ch)))

(defn send-action
  ([action ch]
     (enqueue ch (json/write-str action)))
  ([action]
     (send-action action ch)))


