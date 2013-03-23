(ns srg.login
  (:use [aleph.netty core]
        [lamina.core]
        [srg.utils]
        [srg.session])
  (:require [clojure.tools.logging :as log]))

(defhandler logon [items ch] [:string username :string password]
  (update-session-user! username)
  (swap! sessions add-session! username ch)
  (gen-msg "welcome" username))

(defn hello
  []
  (let [session (current-options)]
    (gen-msg "<s-hello>" (:username session))))

(defhandler hello-to [items] [:string to-user]
  (enqueue (get @sessions to-user)
           (gen-msg "<s-hello>" "from" (:username (current-options)))))

(defhandler logout [items] [:string username]
  (log/warn :logout username))

