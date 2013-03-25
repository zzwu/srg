(ns srg.session
  (:use [aleph.netty core]
        [aleph.tcp]
        [lamina.core]))

(def sessions (atom {}))

(defn update-session-user!
  [username]
  (let [session (current-options)]
    (.set local-options (assoc session :username username))))

(defn add-session!
  [sessions name channel]
  (assoc sessions name channel))

(defn send-to-user
  [user message]
  (enqueue (get @sessions user)
           message))