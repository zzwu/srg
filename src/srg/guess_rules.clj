(ns srg.guess-rules
  (:require [srg.protocols :as p]))

(def kinds {1 :rock 2 :scissors 3 :paper})

(defn chain-rules
  [room & rule-fns]
  (loop [room-state room
         fs rule-fns
         output nil]
    (if (seq fs)
      (let [f (first fs)
            messages (flatten (f room-state))
            new-room-state (reduce p/update room-state messages)]
        (recur new-room-state (rest fs) (concat output messages)))
      output)))

(def init-room
  {:round 1
   :game-type :guess
   :current-index nil
   :game-players-count 2
   :seats {}
   :state :init})

(defmulti handle-game-event
  (fn [room event] (:game-event event)))

(defmethod handle-game-event :start-game
  [room event]
  (let [{:keys [players]} event
        seats (into {} (map #(vector %2 %1) players (range 2)))]
    (-> room
        (assoc :seats seats))))

(defmethod handle-game-event :current-index
  [room event]
  (-> room
      (assoc :current-index (:current-index event))))

(defmethod handle-game-event :guess
  [room event]
  (let [{:keys [kind]} event
        {:keys [current-index]} room]
    (-> room
        (update-in [:seats current-index :history-guesses] (fnil conj []) kind)
        (assoc-in [:seats current-index :current-guess] kind))))

(defmethod handle-game-event :turn-guess
  [room event]
  (let [{:keys [seat kind]} event]
    room))

(defn clear-current-guesses
  [room]
  (reduce (fn [r [seat player]]
            (update-in r [:seats seat] dissoc :current-guess))
          room
          (:seats room)))

(defmethod handle-game-event :clear-last-round
  [room event]
  (-> room
      (dissoc :current-index)
      clear-current-guesses
      (update-in [:round] inc)))

(defmethod handle-game-event :game-over
  [room event]
  (assoc room :state :game-over))

(defmethod handle-game-event :winner
  [room event]
  (assoc room :winner (:winner event)))

(defmethod handle-game-event :default
  [room event]
  (prn "receive not handle event:" event)
  room)

(defmulti play-action
  (fn [room action] (:action action)))

(defn all-guess?
  [room]
  (every? :current-guess
          (vals (:seats room))))

(defn next-player
  [room]
  (let [next-index
        (if-not (:current-index room)
          0
          (mod (inc (:current-index room)) 2))]
    {:game-event :current-index
     :current-index next-index}))

(defmethod play-action :start-game
  [room action]
  [{:game-event :start-game
    :players (:players action)}
   (next-player room)])

(defmethod play-action :guess
  [room action]
  (chain-rules room
               (constantly [{:game-event :guess :seat (:current-index room)
                             :kind (:kind action)}])
               (fn [r]
                 (if-not (all-guess? r)
                   [(next-player room)]))))

(defn turn-guesses
  [room]
  (if (all-guess? room)
    (map
     (fn [[seat player]]
       {:game-event :turn-guess
        :kind (:current-guess player)
        :seat seat})
     (:seats room))))

(defn winner-events
  [room]
  (when (all-guess? room)
    (let [[player1 player2] (vals (:seats room))]
      (cond
       (= (:current-guess player1) (:current-guess player2))
       [{:game-event :draw}
        {:game-event :clear-last-round}
        (next-player room)]
       (= (mod (inc (:current-guess player2)) 3) (:current-guess player2))
       [{:game-event :winner
         :winner player1}
        {:game-event :game-over}]
       :else
       [{:game-event :winner
         :winner player2}
        {:game-event :game-over}]))))

(defrecord GuessRoom []
  p/GameRules
  (play [this  action]
    (chain-rules this
                 (fn [r]
                   (play-action r action))
                 turn-guesses
                 winner-events))
  p/GameState
  (update [this game-event]
    (handle-game-event this game-event)))

(defn room-constructor
  [init-room]
  (merge (GuessRoom.) init-room))

(def actions
  [{:action :start-game, :players [{:player-id "zzwu"} {:player-id "cdf"}]}
   {:action :guess :kind 1}
   {:action :guess :kind 1}
   {:action :guess :kind 2}
   {:action :guess :kind 2}
   {:action :guess :kind 3}
   {:action :guess :kind 1}])

(defn test-game-events
  [room actions]
  (loop [r room
         left actions
         events []]
    (if-not (seq left)
      (do
        (doseq [e events]
          (prn e))
        r)
      (let [curr-events (p/play r (first left))
            next-room (reduce p/update r curr-events)]
        (recur next-room
               (rest left)
               (into events curr-events))))))
