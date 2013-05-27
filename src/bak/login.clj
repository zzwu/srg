(ns srg.login
  (:use [aleph.netty core]
        [lamina.core]
        [srg.utils])
  (:require [clojure.tools.logging :as log]
            [srg.session :as session]))

(defhandler logon [items ch] [:string username :string password]
  (session/add-session! username ch)
  (enqueue ch (gen-msg "welcome" username)))

(defn hello
  []
  (let [session (current-options)
        username (:username session)]
    (session/send-to-user username
                          (gen-msg "hello" (:username session)))))

(defhandler chat-to [items] [:string to-user :string content]
  (session/send-to-user to-user
                        (gen-msg "chat-msg" (:username (current-options)) content)))

(defhandler logout [items] [:string username]
  (log/info :logout username)
  (session/remove-session! username)
  ;;TODO close channel
  )

