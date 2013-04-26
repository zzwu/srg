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

(defn add-session-fn
  [sessions-atom]
  (fn [name channel]
    (swap! sessions-atom assoc name channel)))

(defn remove-session-fn
  [sessions-atom]
  (fn [name]
    (swap! sessions-atom dissoc name)))

(defn load-user-info
  [username]
  {:player-id username
   :bank 100000})

(defn send-message-fn
  [sessions-atom]
  (fn [username message]
    (if-let [channel (get @sessions-atom username)]
      (do
        (enqueue channel (json/write-str message))
        message))))

(defn add-new-game-agent-fn
  [games]
  (fn [id type]
    (let [game-agt (game/new-game-agent id type)]
      (dosync
       (alter games assoc id game-agt)))))

(defn handle-message
  [msg ch sessions games]
  (log/info :received-message msg)
  (let [action (json/read-str msg :value-fn value-readder :key-fn keyword)
        session-options (current-options)
        add-session (add-session-fn sessions)
        remove-session (remove-session-fn sessions)
        send-message (send-message-fn sessions)]
    (case (:action action)
      :login 
      (if (verify action)
        (let [{:keys [username password]} action]
          (do (.set local-options (assoc session-options :username username))
              (add-session username ch)
              (send-message username (assoc (load-user-info username) :message :user-info)))))
      :chat-to
      (if-let [username (:username session-options)]
        (let [{:keys [chat-to message]} action]
          (if-not (send-message chat-to {:message :chat-message :from username :text message})
            (send-message username {:message :warn :msg (str chat-to " is logout.")}))))
      :game-action
      (if-let [username (:username session-options)]
        (game/play-game games
                        (assoc action :player-id username :player-info (load-user-info username))
                        send-message))
      (log/warn :invalied-message msg))))

(defn make-handler
  [sessions games]
  (fn [ch client-info]
    (log/info :channel ch :class-ch (class ch))
    (receive-all ch
                 (fn [message]
                   (handle-message message ch sessions games)))))

(defn start-server
  []
  (let [sessions (atom {})
        games (ref {})
        handler (make-handler sessions games)
        add-new-game-agent (add-new-game-agent-fn games)]
    ;;add zjh rooms
    (doseq [id (range 0 3)]
      (add-new-game-agent id :zjh))
    ;;start tcp server
    {:games games
     :sessions sessions
     :server (start-tcp-server handler default-options)}))
