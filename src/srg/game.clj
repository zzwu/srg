(ns srg.game
  (:use [srg.utils :only [gen-msg defhandler]])
  (:require [srg.queue :as queue])
  (:import [java.util UUID]))

(def games (atom {}))

(defn new-game
  [type players]
  {:type type :players players})

(defn play-game-action
  [game-inst action])

(defn send-to-player
  [player-name message]
  [{:to player-name :message message}])

(defn send-to-room
  [game-inst message]
  (map #(hash-map :to % :message message) (:players game-inst)))

(defn start-game!
  [type players]
  (let [new-game-inst (new-game type players)
        game-id (UUID/randomUUID)]
    (swap! games assoc game-id new-game-inst)
    (send-to-room new-game-inst (gen-msg "start-game" type (first players) (second players)))))

(defhandler queue-for-game
  [items] [:string username :string game-type]
  (if-let [players (queue/add-queue-player! game-type username)]
    (start-game! game-type players)))