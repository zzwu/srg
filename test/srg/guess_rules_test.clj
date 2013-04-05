(ns srg.guess-rules-test
  (:use [srg.guess-rules]
        [clojure.test])
  (:require [srg.protocols :as p]))

(def actions
  [{:action :start-game, :players [{:player-id "zzwu"} {:player-id "cdf"}] :game-id "kkk"}
   {:action :guess :kind 1}
   {:action :guess :kind 1}
   {:action :guess :kind 2}
   {:action :guess :kind 2}
   {:action :guess :kind 3}
   {:action :guess :kind 1}])

(defn run-actions
  [room actions]
  (loop [r room
         left actions
         events []]
    (if-not (seq left)
      [r events]
      (let [curr-events (p/play r (first left))
            next-room (reduce p/update r curr-events)]
        (recur next-room
               (rest left)
               (into events curr-events))))))

(def except-events
  [{:game-event :start-game, :players [{:player-id "zzwu"} {:player-id "cdf"}], :game-id "kkk"}
   {:game-event :current-index, :current-index 0}
   {:game-event :guess, :seat 0, :kind 1}
   {:game-event :current-index, :current-index 1}
   {:game-event :guess, :seat 1, :kind 1}
   {:game-event :turn-guess, :kind 1, :seat 0}
   {:game-event :turn-guess, :kind 1, :seat 1}
   {:game-event :draw}
   {:game-event :clear-last-round}
   {:game-event :current-index, :current-index 0}
   {:game-event :guess, :seat 0, :kind 2}
   {:game-event :current-index, :current-index 1}
   {:game-event :guess, :seat 1, :kind 2}
   {:game-event :turn-guess, :kind 2, :seat 0}
   {:game-event :turn-guess, :kind 2, :seat 1}
   {:game-event :draw}
   {:game-event :clear-last-round}
   {:game-event :current-index, :current-index 0}
   {:game-event :guess, :seat 0, :kind 3}
   {:game-event :current-index, :current-index 1}
   {:game-event :guess, :seat 1, :kind 1}
   {:game-event :turn-guess, :kind 3, :seat 0}
   {:game-event :turn-guess, :kind 1, :seat 1}
   {:game-event :winner, :winner {:history-guesses [1 2 1], :current-guess 1, :player-id "cdf"}}
   {:game-event :game-over}])

(def except-room
  (merge (room-constructor)
         {:winner {:history-guesses [1 2 1], :current-guess 1, :player-id "cdf"},
          :current-index 1,
          :game-id "kkk",
          :state :game-over,
          :seats {0 {:history-guesses [1 2 3], :current-guess 3, :player-id "zzwu"},                   1 {:history-guesses [1 2 1], :current-guess 1, :player-id "cdf"}},
          :game-players-count 2,
          :game-type :guess,
          :round 3}))

(deftest rules-test
  (let [result (run-actions (room-constructor) actions)
        final-room (first result)
        events (second result)]
    (is (= final-room except-room))
    (is (= events except-events))))
