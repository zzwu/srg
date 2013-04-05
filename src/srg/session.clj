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
  ([sessions name channel]
     (update-session-user! name)
     (swap! sessions assoc name channel))
  ([name channel]
     (add-session! sessions name channel)))

(defn remove-session!
  ([sessions name]
     (swap! sessions dissoc name))
  ([name]
     (remove-session! sessions name)))

(defn send-to-user
  [user message]
  (enqueue (get @sessions user)
           message))