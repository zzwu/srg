(ns srg.game
  (:use [srg.utils :only [gen-msg defhandler]])
  (:require [srg.queue :as queue]
            [srg.action :as actions]
            [srg.protocols :as p]
            [srg.guess-rules :as guess]
            [srg.zjh-rules :as zjh]
            [clojure.tools.logging :as log]
            [srg.session :as session])
  (:import [java.util UUID]))

(defn start-game-action
  [type game-id players]
  {:action :start-game :type (keyword type)
   :game-id game-id :players players})

(defn play-game-action
  [game-inst send-message-fn action]
  (log/warn 'play-game-action :action action :game-inst game-inst)
  (let [events (p/play game-inst action)
        _ (log/info :game-action action :gen-events (into [] events))
        new-game (reduce #(p/update %1 %2) game-inst events)]
    ;;send message
    (doseq [e events]
      (doseq [player-id (map :player-id (vals (:seats new-game)))]
        (send-message-fn player-id e)))
    new-game))

(defn new-game-agent
  [id type]
  (case type
    :guess (agent (assoc (guess/room-constructor) :id id))
    :zjh (agent (assoc (zjh/room-constructor) :id id))
    nil))

(defn play-game [games action send-message-fn]
  (if-let [game-agt (get @games (:game-id action))]
    (do (log/info :play-game @game-agt :action action)
        (send game-agt play-game-action send-message-fn action))
    (log/warn :can-not-find-game (:id action))))