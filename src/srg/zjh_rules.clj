(ns srg.zjh-rules
  (:use [srg.utils])
  (:require [srg.protocols :as p]
            [clojure.tools.logging :as log]))


(def init-room
  {:dealer 0
   :seats {}
   :min-add 10
   :base 10
   :max-bid 100
   :last-bid 10
   :history-bids []
   :pot 0
   :max-player-count 6})

(defmulti handle-game-event
  (fn [room event] (:game-event event)))

(defmethod handle-game-event :join-room
  [room {:keys [player-id player-info seat-no]}]
  {:pre [(<= 0 seat-no (dec (:max-player-count room))) (nil? (get-in room [:seats seat-no]))]}
  (assoc-in room [:seats seat-no] {:player-info player-info :player-id player-id :seat-no seat-no}))

(defmethod handle-game-event :ready
  [room {:keys [seat-no]}]
  (assoc-in room [:seats seat-no :state] :ready))

(defn make-ready-player-in
  [room seat-no]
  (assoc-in room [:seats seat-no :state] :in))

(defmethod handle-game-event :start-game
  [room {:keys [into-game-seats]}]
  (reduce make-ready-player-in room into-game-seats))

(defmethod handle-game-event :dealer
  [room {:keys [dealer]}]
  (assoc room :dealer dealer))

(defmethod handle-game-event :deal-cards-to-player
  [room {:keys [seat-no cards]}]
  (assoc-in room [:seats seat-no :cards] cards))

(defmethod handle-game-event :reverse
  [room {:keys [seat-no cards]}]
  (-> room
      (assoc-in [:seats seat-no :reverse] true)
      (assoc-in [:seats seat-no :cards] cards)))

(defmethod handle-game-event :bid
  [room {:keys [seat-no amount]}]
  {:pre [(>= amount (:last-bid room))]}
  (-> room
      (update-in [:seats seat-no :player-info :bank] - amount)
      (update-in [:pot] + amount)
      (update-in [:seats seat-no :amount] (fnil + 0) amount)
      (update-in [:history-bids] conj {:seat-no seat-no :amount amount})
      (assoc :last-bid amount)))

(defmethod handle-game-event :pk
  [room event]
  room)

(defmethod handle-game-event :pk-result
  [room {:keys [loser-no winner-no]}]
  (assoc-in room [:seats loser-no :state] :out))

(defmethod handle-game-event :winner
  [room {:keys [seat-no amount]}]
  (-> room
      (assoc :winner {:seat-no seat-no :amount amount})
      (update-in [:seats seat-no :player-info :bank] + amount)))

(defn clear-last-game-info
  [room seat-no]
  (let [{:keys [seat-no player-id player-info]} (get-in room [:seats seat-no])]
    (assoc-in room [:seats seat-no] {:seat-no seat-no :player-id player-id :player-info player-info :state :init})))

(defn cleat-seats-last-game-info
  [room]
  (reduce clear-last-game-info room (keys (:seats room))))

(defmethod handle-game-event :game-over
  [room event]
  (-> room
      (assoc :pot 0 :last-bid 10 :history-bids [])
      (dissoc :winner)
      cleat-seats-last-game-info))

(defmulti play-action
  (fn [room action] (:action action)))

(defmethod play-action :join-room
  [room action])

(defmethod play-action :ready
  [room action])

(defmethod play-action :start
  [room action])

(defmethod play-action :bid
  [room action])

(defmethod play-action :reverse
  [room action])

(defmethod play-action :pk
  [room action])

(defrecord ZjhRoom []
  p/GameRules
  (play [this  action]
    (chain-rules this
                 (fn [r]
                   (play-action r action))))
  p/GameState
  (update [this game-event]
    (handle-game-event this game-event)))

(defn room-constructor
  ([init-room]
     (merge (ZjhRoom.) init-room))
  ([] (room-constructor init-room)))