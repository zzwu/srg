(ns srg.server
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

(def default-options {:port 10000, :frame fr})

(defhandler add [items ch] [:int left :int right]
  (let [msg (gen-msg "add-result"  (+ left right))]
    (enqueue ch msg)))

(defn value-readder
  [k v]
  (case k
    :action (keyword v)
    :game-action (keyword v)
    v))

(defn verify
  [{:keys [username password]}]
  true)

(defn add-session
  [sessions name channel]
  (assoc sessions name channel))

(defn remove-session
  [sessions name]
  (dissoc sessions name))

(defn load-user-info
  [username]
  {:player-id username
   :bank 100000})

(def sessions (atom {}))

(defn handle-message
  [msg ch]
  (log/info :received-message msg)
  (let [action (json/read-str msg :value-fn value-readder :key-fn keyword)
        session-options (current-options)]
    (case (:action action)
      :login 
      (if (verify action)
        (let [{:keys [username password]} action]
          (do (.set local-options (assoc session-options :username username))
              (swap! sessions add-session username ch)
              (enqueue (get @sessions username)
                       (json/write-str (assoc (load-user-info username) :message :user-info))))))
      :chat-to
      (if-let [username (:username session-options)]
        (let [{:keys [chat-to message]} action]
          (if-let [chat-to-channel (get @sessions chat-to)]
            (enqueue chat-to-channel (json/write-str {:message :chat-message :from username :text message}))
            (enqueue ch (json/write-str {:message :warn :msg (str chat-to " is logout.")})))))
      (log/warn :invalied-message msg))))

(defn handle-old-message
  [msg ch]
  (log/info :received-message msg)
  (let [array (into []  (.split msg "\r"))
        header (first array)
        items (into [] (rest array))]
    (case header
      "add" (add items ch)
      "login" (login/logon items ch)
      "hello" (login/hello)
      "chat-to" (login/chat-to items)
      "queue" (game/queue-for-game items)
      "play-game" (game/play-game items)
      (log/warn :invalied-message msg))))

(defn handler [ch client-info]
  (log/info :channel ch :class-ch (class ch))
  (receive-all ch
               (fn [message]
                 (handle-message message ch))))

(defn start-server
  ([handler options]
     (log/info :start-server options :handler handler)
     (start-tcp-server handler options))
  ([handler]
     (start-tcp-server handler default-options)))
