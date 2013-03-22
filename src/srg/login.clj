(ns srg.login
  (:use [aleph.netty core]
        [srg.utils])
  (:require [clojure.tools.logging :as log]))

(defn update-session-user!
  [username]
  (let [session (current-options)]
      (.set local-options (assoc session :username username))))

(defhandler logon [items] [:string username :string password]
  (update-session-user! username)
  (gen-msg "welcome" username))

(defn hello
  []
  (let [session (current-options)]
    (gen-msg "<s-hello>" (:username session))))

(defhandler logout [items] [:string username]
  (log/warn :logout username))

