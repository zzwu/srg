(ns srg.action
  (:use [srg.utils]))

(defplayaction Guess "guess" [:string id :int kind])

(defn to-play-action
  [items]
  (when-let [f (get @action-header-changefn-map (first items))]
    (f (into [] (rest items)))))

(defn gen-start-game-message
  [event]
  (let [players (:players event)
        player-id0 (:player-id (first players))
        player-id1 (:player-id (second players))]
    (gen-msg "start-game" (:game-id event) player-id0 player-id1)))

(defn gen-curr-index-msg
  [event]
  (gen-msg "current-index" (:current-index event)))

(defn gen-guess-msg
  [event]
  (gen-msg "guess" (:kind event)))

(defn gen-turn-guess-msg
  [event]
  (gen-msg "turn-guess" (:seat event) (:kind event)))

(defn gen-clear-last-round-msg
  [event]
  (gen-msg "clear-last-round"))

(defn gen-game-over-msg
  [event]
  (gen-msg "game-over"))

(defn gen-winner-msg
  [event]
  (gen-msg "winner" (:player-id (:winner event))))

(defn gen-draw-msg
  [event]
  (gen-msg "draw"))

(defn event-to-message
  [event]
  (case (:game-event event)
    :start-game (gen-start-game-message event)
    :current-index (gen-curr-index-msg event)
    :guess (gen-guess-msg event)
    :turn-guess (gen-turn-guess-msg event)
    :clear-last-round (gen-clear-last-round-msg event)
    :game-over (gen-game-over-msg event)
    :winner (gen-winner-msg event)
    :draw (gen-draw-msg event)))