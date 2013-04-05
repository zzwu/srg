(ns srg.game
  (:use [srg.utils :only [gen-msg defhandler]])
  (:require [srg.queue :as queue]
            [srg.action :as actions]
            [srg.protocols :as p]
            [srg.guess-rules :as guess]
            [clojure.tools.logging :as log]
            [srg.session :as session])
  (:import [java.util UUID]))

(def games (ref {}))

(defn start-game-action
  [type game-id players]
  {:action :start-game :type (keyword type)
   :game-id game-id :players players})

(defn send-to-player
  [player-name event]
  (let [message (actions/event-to-message event)]
    (session/send-to-user player-name message)))

(defn send-to-room
  [game-inst event]
  (let [message (actions/event-to-message event)]
    (log/warn :game-message message)
    (doseq [player-id (map :player-id (vals (:seats game-inst)))]
      (session/send-to-user player-id message))))

(defn play-game-action
  [game-inst action]
  (let [events (p/play game-inst action)
        _ (log/info :game-action action :gen-events (into [] events))
        new-game (reduce #(p/update %1 %2) game-inst events)]
    (doseq [e events]
           (send-to-room new-game e))
    new-game))

(defn start-game!
  [type players]
  (let [game-id (str (UUID/randomUUID))
        new-game-inst-agt (agent (assoc (guess/room-constructor) :id game-id))
        start-action (start-game-action type game-id (map #(hash-map :player-id %) players))]
    (dosync
     (alter games assoc game-id new-game-inst-agt))
    (log/info :start-a-new-game new-game-inst-agt :current-games @games)
    (send new-game-inst-agt play-game-action start-action)))

(defhandler queue-for-game
  [items] [:string game-type]
  (if-let [players (queue/add-queue-player! game-type (session/current-username))]
    (start-game! game-type players)))

(defn play-game [items]
  (if-let [action (actions/to-play-action items)]
    (if-let [game-agt (get @games (:id action))]
      (do (log/info :play-game @game-agt :action action)
          (send game-agt play-game-action action))
      (log/warn :can-not-find-game (:id action)))
    (log/warn :can-not-parse-action-form-items items)))