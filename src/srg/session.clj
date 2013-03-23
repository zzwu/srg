(ns srg.session
  (:use [aleph.netty core]))

(def sessions (atom {}))

(defn update-session-user!
  [username]
  (let [session (current-options)]
    (.set local-options (assoc session :username username))))

(defn add-session!
  [sessions name channel]
  (assoc sessions name channel))