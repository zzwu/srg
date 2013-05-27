(ns srg.zjh-rules
  (:use [srg.utils])
  (:require [srg.protocols :as p]
            [clojure.tools.logging :as log]
            [srg.cards :as cards]
            [srg.zjh-poker :as poker]))


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

(def bid-options [10 20 50 100])

(defmulti handle-game-event
  (fn [room event] (:game-event event)))

(defmethod handle-game-event :join-room
  [room {:keys [player-id player-info seat-no join-time]}]
  {:pre [(<= 0 seat-no (dec (:max-player-count room))) (nil? (get-in room [:seats seat-no]))]}
  (assoc-in room [:seats seat-no] {:player-info player-info :player-id player-id :seat-no seat-no
                                   :state :init :join-time join-time}))

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

(defmethod handle-game-event :current-player
  [room {:keys [seat-no enable-actions]}]
  (-> room
      (assoc :current-player {:current-index seat-no :enable-actions enable-actions})))

(defmethod handle-game-event :reverse
  [room {:keys [seat-no cards]}]
  (-> room
      (assoc-in [:seats seat-no :reverse] true)
      (assoc-in [:seats seat-no :cards] cards)))

(defn true-bid
  [reverse? amount bid-options]
  (if reverse?
    (last (filter #(< % amount) bid-options))
    amount))

(defmethod handle-game-event :bid
  [room {:keys [seat-no amount pk?]}]
  {:pre [(>= amount (:last-bid room))]}
  (-> room
      (update-in [:seats seat-no :player-info :bank] - amount)
      (update-in [:pot] + amount)
      (update-in [:seats seat-no :amount] (fnil + 0) amount)
      (update-in [:history-bids] conj {:seat-no seat-no :amount amount})
      (assoc :last-bid
        (if pk?
          (:last-bid room)
          (true-bid (get-in room [:seats seat-no :reverse]) amount bid-options)))))

(defmethod handle-game-event :fold
  [room {:keys [seat-no]}]
  (assoc-in room [:seats seat-no :state] :out))

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
      (dissoc :winner :current-player)
      cleat-seats-last-game-info))


(defn find-seat-no
  [room player-id]
  (first
   (keep (fn [[seat-no player]]
           (if (= player-id (:player-id player))
             seat-no)) (:seats room))))

(defmulti play-action
  (fn [room action] (:game-action action)))

(defmethod play-action :join-room
  [room action]
  [(merge {:game-event :join-room} (select-keys action [:seat-no :player-id :player-info :join-time]))])

(defmethod play-action :ready
  [room action]
  [{:game-event :ready :seat-no (find-seat-no room (:player-id action))}])

(defn start-game
  [room]
  (let [into-game-seats (keep (fn [[seat-no player]]
                                (if (= :ready (:state player))
                                  seat-no)) (:seats room))]
    [{:game-event :start-game :into-game-seats (vec into-game-seats)}]))

(defn seats-no-cycle
  [from-no seats-no]
  (let [seats (sort seats-no)
        larger-seats (filter #(> % from-no) seats)
        smaller-seats (filter #(<= % from-no) seats)
        new-seats (into (vec larger-seats) smaller-seats)]
    (cycle new-seats)))

(defn in-game?
  [room seat-no]
  (= :in (get-in room [:seats seat-no :state])))

(defn dealer-index
  [room]
  (let [from-no (or (:dealer room) 0)
        seats-no (keys (:seats room))]
    (assert ((apply hash-set seats-no) from-no))
    (first
     (filter
      (partial in-game? room)
      ;;take 12 to avoid endless loop
      (take 12 (seats-no-cycle from-no seats-no))))))

(defn dealer
  [room]
  [{:game-event :dealer
    :dealer (dealer-index room)}])

(defn deal-cards-to-player
  [room seed]
  (let [deck (cards/shuffle-random cards/all-cards seed)]
    (map (fn [seat-no cards]
           {:game-event :deal-cards-to-player :seat-no seat-no :cards (into [] cards)})
         (filter (partial in-game? room) (keys (:seats room)))
         (partition 3 deck))))

(defn next-index
  ([last seats-no]
     (let [orders (seats-no-cycle last seats-no)]
       (first orders)))
  ([room]
     (let [in-seats-no (filter (partial in-game? room) (keys (:seats room)))
           current-index (or ((comp :current-index :current-player) room) (:dealer room))]
       (next-index current-index in-seats-no))))

(defn enable-bids
  [last-bid reverse? bank bid-options]
  (filter #(>= bank %)
          (if reverse?
            (rest (filter #(>= % last-bid) bid-options))
            (filter #(>= % last-bid) (pop bid-options)))))

(defn next-player
  [room]
  (let [current-index ((comp :current-player :current-index) room)
        last-bid (:last-bid room)
        next-player-index (next-index room)
        reverse? (get-in room [:seats next-player-index :reverse])
        bank (get-in room [:seats next-player-index :player-info :bank])
        bids (enable-bids last-bid reverse? bank bid-options)]
    [{:game-event :current-player
      :seat-no next-player-index
      :enable-actions {:fold true :bid bids :reverse (not reverse?)}}]))

(defmethod play-action :start
  [room {:keys [seed]}]
  (chain-rules room
               start-game
               dealer
               #(deal-cards-to-player % seed)
               next-player))

(def actions #{:fold :bid :reverse})

(defn bid-event
  [room amount]
  [{:game-event :bid :seat-no ((comp :current-index :current-player) room) :amount amount}])

(defn get-winner-seat-no
  [room]
  (let [left (filter (partial in-game? room) (keys (:seats room)))]
    (if (= 1 (count left))
      (first left))))

(defn winner-message
  [winner-no win-amount]
  [{:game-event :winner :seat-no winner-no :amount win-amount}
   {:game-event :game-over}])

(defn game-over-or-next-player
  [room]
  (if-let [winner-no (get-winner-seat-no room)]
    (winner-message winner-no (:pot room))
    (next-player room)))

(defmethod play-action :bid
  [room {:keys [amount]}]
  (chain-rules room
               #(bid-event % amount)
               next-player))

(defn current-seat-no
  [room]
  ((comp :current-index :current-player) room))

(defmethod play-action :reverse
  [room action]
  (let [seat-no (current-seat-no room)]
    [{:game-event :reverse :seat-no seat-no :cards (get-in room [:seats seat-no :cards])}]))

(defn fold-event
  [room]
  (let [seat-no (current-seat-no room)]
    [{:game-event :fold :seat-no seat-no}]))

(defmethod play-action :fold
  [room action]
  (chain-rules room
               fold-event
               game-over-or-next-player))

(defn auto-bid-before-pk
  [room]
  (let [last-bid (:last-bid room)
        amount (* 2 (first (-> room :current-player :enable-actions :bid)))]
    [{:game-event :bid
      :amount amount
      :pk true
      :seat-no (-> room :current-player :current-index)}]))

(defn pk-winner-loser
  [left-cards left-no right-cards right-no]
  (if (poker/win? left-cards right-cards)
    [left-no right-no]
    [right-no left-no]))

(defn pk-result
  [room seat-no pk-with-seat-no]
  (let [left-cards (get-in room [:seats seat-no :cards])
        right-cards (get-in room [:seats pk-with-seat-no :cards])
        [winner-no loser-no] (pk-winner-loser left-cards seat-no right-cards pk-with-seat-no)]
    [{:game-event :pk-result
      :loser-no loser-no
      :winner-no winner-no}]))

(defmethod play-action :pk
  [room {:keys [pk-with-seat-no]}]
  (chain-rules room
               auto-bid-before-pk
               #(pk-result % (current-seat-no %) pk-with-seat-no)
               game-over-or-next-player))

(defn add-seat-no
  [room action]
  (if-let [player-id (:player-id action)]
    (assoc action :seat-no (find-seat-no room player-id))
    action))

(defrecord ZjhRoom []
  p/GameRules
  (play [this  action]
    (chain-rules this
                 (fn [r]
                   (play-action r
                                action))))
  p/GameState
  (update [this game-event]
    (handle-game-event this game-event)))

(defn room-constructor
  ([init-room]
     (merge (ZjhRoom.) init-room))
  ([] (room-constructor init-room)))