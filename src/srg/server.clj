(ns srg.server
  (:use [lamina.core]
        [aleph.tcp]
        [gloss.core]
        [gloss.io]
        [aleph.netty core]
        [srg.utils])
  (:require [clojure.tools.logging :as log]
            [srg.login :as login]))

(def fr
  (finite-frame :int32 (string :utf-8)))

(def default-options {:port 10000, :frame fr})

(defhandler add [items] [:int left :int right]
  (gen-msg "add-result"  (+ left right)))

(defn handle-message
  [msg ch]
  (log/info :received-message msg)
  (let [array (into []  (.split msg "\r"))
        header (first array)
        items (into [] (rest array))]
    (case header
      "add" (add items)
      "login" (login/logon items ch)
      "hello" (login/hello)
      "hello-to" (login/hello-to items)
      (log/warn :invalied-message msg))))

(defn handler [ch client-info]
  (log/info :channel ch :class-ch (class ch))
  (prn (class ch))
  (receive-all ch
               (fn [message]
                 (if-let [result (handle-message message ch)]
                   (if (string? result)
                     (enqueue ch result))))))

(defn start-server
  ([handler options]
     (log/info :start-server options :handler handler)
     (start-tcp-server handler options))
  ([handler]
     (start-tcp-server handler default-options)))
