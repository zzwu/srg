(ns srg.ddz-rules
  (:use [srg.utils])
  (:require [srg.protocols :as p]
            [clojure.tools.logging :as log]
            [srg.cards :as cards]))

;;初始 发牌 叫分 埋底牌 打牌 结束
(def phases [:init :deal-cards :raise :hide :play :game-over])

(def init-room
  {:game-type :ddz
   :current-play-index nil
   :current-raise-index nil
   :history-raises []
   :game-players-count 3
   :seats {}
   :phase :init
   :boss nil
   :current-turn-actions []
   :history-actions []})

(defmulti handle-game-event
  (fn [room event] (:game-event event)))

(defmulti play-action
  (fn [room action] (:action action)))

(defn deal-cards
  [room]
  (let [cards (cards/random-deck)
        player-ids (map :player-id (vals (:seats room)))]
    [{:game-event :deal-players-cards
      :players-cards {(first player-ids) (take 17 cards)
                      (second player-ids) (take 17 (drop 17 cards))
                      (last player-ids) (take 17 (drop 34 cards))}}
     {:game-event :deal-hide-cards
      :cards (take 3 (reverse cards))}]))

(defn start-raise-phase
  [room]
  (let [lucky-index (rand-int (count (:seats room)))]
    [{:game-event :current-raise-index
      :current-raise-index lucky-index
      :enable-reises [1 2 3]}]))

(defmethod play-action :start-game
  [room action]
  (chain-rules room
               (constantly [{:game-event :start-game
                             :players (:players action)
                             :game-id (:game-id action)}])
               deal-cards
               start-raise-phase))

(defn raise-boss
  [room]
  (cond
   (= 3 (-> room :history-raises last :raise)) (-> room :history-raises last :player-id) 
   (let [reversed-raises (reverse (:history-raises room))
         one (first reversed-raises)
         two (second reversed-raises)
         three (first (drop 2 reversed-raises))]
     (and (= -1 (:raise one) (:raise two)) (not= -1 (:raise three))))
   (-> room
       :history-raises
       reverse
       rest
       rest
       :player-id)
   :else nil))

(defmethod play-action :raise
  [room action]
  (chain-rules room
               (constantly [:game-event :raise
                            :raise (:raise action)])
               #(if-let [boss (raise-boss)]
                  [{:game-event :boss
                    :player-id boss}
                   {:game-event :phase
                    :phase :hide}
                   {:game-event :ask-for-hide-cards
                    :player-id boss}])))


(defrecord DdzRoom []
  p/GameRules
  (play [this action]
    (chain-rules this
                 (partial start action)
                 (partial raise action)
                 (partial hide action)
                 (partial play action))))

(defn room-constructor
  ([init-room]
     ))
