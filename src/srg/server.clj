(ns srg.server
  (:use [lamina.core]
        [aleph.tcp]
        [gloss.core]
        [gloss.io]
        [aleph.netty core]
        [srg.utils]
        [srg.system])
  (:require [clojure.tools.logging :as log]
            [srg.game :as game]
            [clojure.data.json :as json]
            [srg.protocols :as p]))

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
  [system {:keys [username password]}]
  (p/verify system username password))

(defn add-session-fn
  [sessions-atom]
  (fn [name channel]
    (swap! sessions-atom assoc name channel)))

(defn remove-session-fn
  [sessions-atom]
  (fn [name]
    (swap! sessions-atom dissoc name)))

(defn load-user-info
  [system username]
  (p/load-user-info system username))

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

(defn room-id-and-empty-seat-no
  [id room]
  (let [max (:max-player-count room)]
    (some (fn [seat-no] (if (nil? (get (:seats room) seat-no))
                          [id seat-no]))
          (range max))))

(defn find-seat
  [games]
  (some (fn [[id room]] (room-id-and-empty-seat-no id @room)) games))

(defn handle-message
  [msg ch sessions games system]
  (log/info :received-message msg)
  (let [action (json/read-str msg :value-fn value-readder :key-fn keyword)
        session-options (current-options)
        add-session (add-session-fn sessions)
        remove-session (remove-session-fn sessions)
        send-message (send-message-fn sessions)]
    (case (:action action)
      :register
      (let [{:keys [username password]} action]
        (p/register system (assoc (select-keys action [:username :password :sex]) :bank 1000 :exp 0)))
      :login 
      (if (verify system action)
        (let [{:keys [username password]} action]
          (do (.set local-options (assoc session-options :username username))
              (add-session username ch)
              (send-message username (assoc (load-user-info system username) :message :user-info)))))
      :chat-to
      (if-let [username (:username session-options)]
        (let [{:keys [chat-to message]} action]
          (if-not (send-message chat-to {:message :chat-message :from username :text message})
            (send-message username {:message :warn :msg (str chat-to " is logout.")}))))
      :find-seat
      (if-let [username (:username session-options)]
        (let [[game-id seat-no] (find-seat @games)]
          (send-message username {:message :find-seat :game-id game-id :seat-no seat-no})))
      :game-action
      (if-let [username (:username session-options)]
        (game/play-game games
                        (assoc action :player-id username :player-info (load-user-info username))
                        send-message))
      (log/warn :invalied-message msg))))

(defn make-handler
  [sessions games system]
  (fn [ch client-info]
    (log/info :channel ch :class-ch (class ch))
    (receive-all ch
                 (fn [message]
                   (handle-message message ch sessions games system)))))

(defn start-server
  []
  (let [sessions (atom {})
        games (ref {})
        system (srg.system.system. "hello")
        handler (make-handler sessions games system)
        add-new-game-agent (add-new-game-agent-fn games)]
    ;;add zjh rooms
    (doseq [id (range 0 3)]
      (add-new-game-agent id :zjh))
    ;;start tcp server
    {:games games
     :sessions sessions
     :system system
     :server (start-tcp-server handler default-options)}))
