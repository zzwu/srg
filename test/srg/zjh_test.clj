(ns srg.zjh-test
  (:require [clojure.data.json :as json])
  (:use [srg.zjh-rules]
        [midje.sweet]))

(def join-room-msg {:player-id "zzwu" :player-info {:bank 100} :seat-no 3 :game-event :join-room :join-time 1000})

(facts "test handle join-room event"
       (let [room init-room
             except-seat-3 {:player-id "zzwu" :player-info {:bank 100} :seat-no 3
                            :state :init :join-time 1000}]
         (get-in (handle-game-event room join-room-msg) [:seats 3]) => except-seat-3))

(facts "test handle ready event"
       (let [room (merge init-room {:seats {3 {:player-id "zzwu" :player-info {:bank 100}}}})
             ready-msg {:seat-no 3 :game-event :ready}
             new-room (handle-game-event room ready-msg)]
         (get-in new-room [:seats 3 :state]) => :ready))

(facts "test handle start event"
       (let [room init-room
             messages [{:player-id "zzwu" :player-info {:bank 100} :seat-no 0 :game-event :join-room}
                       {:player-id "ddy" :player-info {:bank 100} :seat-no 1 :game-event :join-room}
                       {:player-id "cdf" :player-info {:bank 100} :seat-no 3 :game-event :join-room}
                       {:seat-no 0 :game-event :ready}
                       {:seat-no 1 :game-event :ready}
                       {:seat-no 3 :game-event :ready}
                       {:game-event :start-game :into-game-seats [0 3]}]
             new-room (reduce handle-game-event room messages)]
         (get-in new-room [:seats 0 :state]) => :in
         (get-in new-room [:seats 3 :state]) => :in))


(facts "test handle deal-cards-to-player event"
       (let [messages [{:player-id "zzwu"  :game-event :join-room :player-info {:bank 100} :seat-no 0}
                       {:seat-no 0 :game-event :ready}
                       {:game-event :start-game :into-game-seats [0]}
                       {:seat-no 0 :game-event :deal-cards-to-player :cards [{:rank -1 :suit :facedown} {:rank -1 :suit :facedown}]}]
             new-room (reduce handle-game-event init-room messages)]
         (get-in new-room [:seats 0 :cards]) => [{:rank -1 :suit :facedown} {:rank -1 :suit :facedown}]))

(facts "test dealer handler"
       (let [msg {:game-event :dealer :dealer 0}]
         (:dealer (handle-game-event init-room msg)) => 0))

(facts "test reverse action"
       (let [messages [{:player-id "zzwu"  :game-event :join-room :player-info {:bank 100} :seat-no 0}
                       {:seat-no 0 :game-event :ready}
                       {:game-event :start-game :into-game-seats [0]}
                       {:seat-no 0 :game-event :deal-cards-to-player :cards [{:rank -1 :suit :facedown} {:rank -1 :suit :facedown}]}
                       {:game-event :reverse :seat-no 0 :cards [{:rank 15, :suit :clubs} {:rank 15, :suit :clubs}]}]
             new-room (reduce handle-game-event init-room messages)]
         (get-in new-room [:seats 0 :reverse]) => true
         (get-in new-room [:seats 0 :cards]) => [{:rank 15, :suit :clubs} {:rank 15, :suit :clubs}]))


(facts "test bid event handler"
       (let [messages [{:player-id "zzwu"  :game-event :join-room :player-info {:bank 100} :seat-no 0}
                       {:seat-no 0 :game-event :ready}
                       {:game-event :start-game :into-game-seats [0]}
                       {:seat-no 0 :game-event :deal-cards-to-player :cards [{:rank -1 :suit :facedown} {:rank -1 :suit :facedown}]}
                       {:game-event :bid :seat-no 0 :amount 10}
                       {:game-event :bid :seat-no 0 :amount 50}]
             new-room (reduce handle-game-event init-room messages)]
         (:pot new-room) => 60
         (get-in new-room [:seats 0 :amount]) => 60
         (:history-bids new-room) => [{:amount 10, :seat-no 0} {:amount 50, :seat-no 0}]
         (:last-bid new-room) => 50))

(facts "test pk-result handler"
       (let [messages [{:player-id "zzwu"  :game-event :join-room :player-info {:bank 100} :seat-no 0 :join-time 1000}
                       {:player-id "cdf"  :game-event :join-room :player-info {:bank 100} :seat-no 1 :join-time 1000}
                       {:seat-no 0 :game-event :ready}
                       {:seat-no 1 :game-event :ready}
                       {:game-event :start-game :into-game-seats [0 1]}
                       {:game-event :bid :seat-no 0 :amount 10}
                       {:game-event :bid :seat-no 1 :amount 10}
                       {:seat-no 0 :game-event :deal-cards-to-player :cards [{:rank 6, :suit :clubs} {:rank 8, :suit :clubs} {:rank 5 :suit :clubs}]}
                       {:seat-no 1 :game-event :deal-cards-to-player :cards [{:rank 3, :suit :clubs} {:rank 7, :suit :clubs} {:rank 10 :suit :clubs}]}
                       {:game-event :bid :seat-no 0 :amount 30}
                       {:game-event :bid :seat-no 1 :amount 30}
                       {:game-event :bid :seat-no 0 :amount 30}
                       {:game-event :pk :seat-no 0 :pk-with 1}
                       {:game-event :pk-result :loser-no 1 :winner-no 0}
                       {:game-event :winner :seat-no 0 :amount 110}]
             new-room (reduce handle-game-event init-room messages)]
         new-room => {:base 10, :dealer 0,
                      :history-bids [{:amount 10, :seat-no 0} {:amount 10, :seat-no 1} {:amount 30, :seat-no 0} {:amount 30, :seat-no 1} {:amount 30, :seat-no 0}],
                      :last-bid 30,
                      :max-bid 100, :max-player-count 6, :min-add 10,
                      :pot 110,
                      :seats {0 {:amount 70, :cards [{:rank 6, :suit :clubs} {:rank 8, :suit :clubs} {:rank 5, :suit :clubs}], :player-id "zzwu", :player-info {:bank 140}, :seat-no 0, :state :in :join-time 1000},
                              1 {:amount 40, :cards [{:rank 3, :suit :clubs} {:rank 7, :suit :clubs} {:rank 10, :suit :clubs}], :player-id "cdf", :player-info {:bank 60}, :seat-no 1, :state :out :join-time 1000}},
                      :winner {:amount 110, :seat-no 0}}))


(facts "test game-over handler"
       (let [messages [{:player-id "zzwu"  :game-event :join-room :player-info {:bank 100} :seat-no 0}
                       {:player-id "cdf"  :game-event :join-room :player-info {:bank 100} :seat-no 1}
                       {:seat-no 0 :game-event :ready}
                       {:seat-no 1 :game-event :ready}
                       {:game-event :start-game :into-game-seats [0 1]}
                       {:game-event :bid :seat-no 0 :amount 10}
                       {:game-event :bid :seat-no 1 :amount 10}
                       {:seat-no 0 :game-event :deal-cards-to-player :cards [{:rank 6, :suit :clubs} {:rank 8, :suit :clubs} {:rank 5 :suit :clubs}]}
                       {:seat-no 1 :game-event :deal-cards-to-player :cards [{:rank 3, :suit :clubs} {:rank 7, :suit :clubs} {:rank 10 :suit :clubs}]}
                       {:game-event :bid :seat-no 0 :amount 30}
                       {:game-event :bid :seat-no 1 :amount 30}
                       {:game-event :bid :seat-no 0 :amount 30}
                       {:game-event :pk :seat-no 0 :pk-with 1}
                       {:game-event :pk-result :loser-no 1 :winner-no 0}
                       {:game-event :winner :seat-no 0 :amount 110}
                       {:game-event :game-over}]
             new-room (reduce handle-game-event init-room messages)]
         new-room => {:base 10, :dealer 0, :history-bids [], :last-bid 10,
                      :max-bid 100, :max-player-count 6, :min-add 10, :pot 0,
                      :seats {0 {:player-id "zzwu", :player-info {:bank 140}, :seat-no 0, :state :init}, 1 {:player-id "cdf", :player-info {:bank 60}, :seat-no 1, :state :init}}}))


(facts "seats-no-cycle test"
       (first (seats-no-cycle 3 [0 1 2 3 4 5])) => 4
       (first (seats-no-cycle 0 [3 4 5 0 1 2])) => 1
       (first (seats-no-cycle 4 [3 4 5 0 1 2])) => 5
       (first (seats-no-cycle 5 [0 1 2 3 4 5])) => 0)

(facts "start-game test"
       (let [actions [{:game-action :join-room :seat-no 0 :player-id "zzwu" :player-info {:bank 1000} :join-time 1000}
                      {:game-action :join-room :seat-no 1 :player-id "ddy" :player-info {:bank 1000} :join-time 1000}
                      {:game-action :join-room :seat-no 2 :player-id "cdf" :player-info {:bank 1000} :join-time 1000}
                      {:game-action :ready :player-id "zzwu"}
                      {:game-action :ready :player-id "ddy"}
                      {:game-action :ready :player-id "cdf"}
                      {:game-action :start :seed 2}]
             [room events]
             (loop [as actions
                    es []
                    r (room-constructor)]
               (if (seq as)
                 (let [curr-events (play-action r (first as))
                       ;_ (prn curr-events)
                       new-room (reduce handle-game-event r curr-events)
                       ;_ (prn new-room)
                       ]
                   (recur (rest as) (into es curr-events) new-room))
                 [r es]))]
         (into {} room) => {:base 10,
                            :dealer 1,
                            :history-bids [],
                            :last-bid 10,
                            :max-bid 100,
                            :max-player-count 6,
                            :min-add 10, :pot 0,
                            :current-player {:current-index 2, :enable-actions {:bid (list 10 20 50), :fold true, :reverse true}},
                            :seats {0 {:cards [{:rank 4, :suit :spades} {:rank 13, :suit :hearts} {:rank 10, :suit :diamonds}], :player-id "zzwu" :player-info {:bank 1000}, :seat-no 0, :state :in :join-time 1000},
                                    1 {:cards [{:rank 4, :suit :clubs} {:rank 9, :suit :spades} {:rank 10, :suit :hearts}], :player-id "ddy" :player-info {:bank 1000}, :seat-no 1, :state :in :join-time 1000},
                                    2 {:cards [{:rank 8, :suit :diamonds} {:rank 13, :suit :spades} {:rank 14, :suit :clubs}], :player-id "cdf" :player-info {:bank 1000}, :seat-no 2, :state :in :join-time 1000}}}
         events => [{:game-event :join-room, :player-id "zzwu", :player-info {:bank 1000}, :seat-no 0 :join-time 1000}
                    {:game-event :join-room, :player-id "ddy", :player-info {:bank 1000}, :seat-no 1 :join-time 1000}
                    {:game-event :join-room, :player-id "cdf", :player-info {:bank 1000}, :seat-no 2 :join-time 1000}
                    {:game-event :ready, :seat-no 0}
                    {:game-event :ready, :seat-no 1}
                    {:game-event :ready, :seat-no 2}
                    {:game-event :start-game, :into-game-seats [2 1 0]}
                    {:dealer 1, :game-event :dealer}
                    {:cards [{:rank 8, :suit :diamonds} {:rank 13, :suit :spades} {:rank 14, :suit :clubs}], :game-event :deal-cards-to-player, :seat-no 2}
                    {:cards [{:rank 4, :suit :clubs} {:rank 9, :suit :spades} {:rank 10, :suit :hearts}], :game-event :deal-cards-to-player, :seat-no 1}
                    {:cards [{:rank 4, :suit :spades} {:rank 13, :suit :hearts} {:rank 10, :suit :diamonds}], :game-event :deal-cards-to-player, :seat-no 0}
                    {:enable-actions {:bid (list 10 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 2}]))

(facts "test enable-bids"
       (enable-bids 10 false 100 [10 20 50 100]) => (list 10 20 50)
       (enable-bids 10 true 100 [10 20 50 100]) => (list 20 50 100)
       (enable-bids 20 false 100 [10 20 50 100]) => (list 20 50)
       (enable-bids 20 true 100 [10 20 50 100]) => (list 50 100)
       (enable-bids 50 false 100 [10 20 50 100]) => (list 50)
       (enable-bids 50 true 100 [10 20 50 100]) => (list 100)
       (enable-bids 50 true 80 [10 20 50 100]) => (list))

(facts "win without pk test"
       (let [actions [{:game-action :join-room :seat-no 0 :player-id "zzwu" :player-info {:bank 1000}}
                      {:game-action :join-room :seat-no 1 :player-id "ddy" :player-info {:bank 1000}}
                      {:game-action :join-room :seat-no 2 :player-id "cdf" :player-info {:bank 1000}}
                      {:game-action :ready :player-id "zzwu"}
                      {:game-action :ready :player-id "ddy"}
                      {:game-action :ready :player-id "cdf"}
                      {:game-action :start :seed 2}
                      {:game-action :bid :amount 10 :player-id "cdf"}
                      {:game-action :bid :amount 10 :player-id "zzwu"}
                      {:game-action :bid :amount 20 :player-id "ddy"}
                      {:game-action :reverse :player-id "cdf"} {:game-action :fold :player-id "cdf"}
                      {:game-action :reverse :player-id "zzwu"} {:game-action :bid :amount 50 :player-id "zzwu"}
                      {:game-action :bid :player-id "ddy" :amount 50}
                      {:game-action :fold :player-id "zzwu"}
                      ]
             [room events]
             (loop [as actions
                    es []
                    r (room-constructor)]
               (if (seq as)
                 (let [curr-events (play-action r (first as))
                       new-room (reduce handle-game-event r curr-events)]
                   (recur (rest as) (into es curr-events) new-room))
                 [r es]))]
         (into {} room) => {:base 10,
                            :dealer 1, :history-bids [], :last-bid 10, :max-bid 100,
                            :max-player-count 6, :min-add 10, :pot 0,
                            :seats {0 {:player-id "zzwu", :player-info {:bank 940}, :seat-no 0, :state :init},
                                    1 {:player-id "ddy", :player-info {:bank 1070}, :seat-no 1, :state :init},
                                    2 {:player-id "cdf", :player-info {:bank 990}, :seat-no 2, :state :init}}}
         events => [{:game-event :join-room, :player-id "zzwu", :player-info {:bank 1000}, :seat-no 0}
                    {:game-event :join-room, :player-id "ddy", :player-info {:bank 1000}, :seat-no 1}
                    {:game-event :join-room, :player-id "cdf", :player-info {:bank 1000}, :seat-no 2}
                    {:game-event :ready, :seat-no 0}
                    {:game-event :ready, :seat-no 1}
                    {:game-event :ready, :seat-no 2}
                    {:game-event :start-game, :into-game-seats [2 1 0]}
                    {:dealer 1, :game-event :dealer}
                    {:cards [{:rank 8, :suit :diamonds} {:rank 13, :suit :spades} {:rank 14, :suit :clubs}], :game-event :deal-cards-to-player, :seat-no 2}
                    {:cards [{:rank 4, :suit :clubs} {:rank 9, :suit :spades} {:rank 10, :suit :hearts}], :game-event :deal-cards-to-player, :seat-no 1}
                    {:cards [{:rank 4, :suit :spades} {:rank 13, :suit :hearts} {:rank 10, :suit :diamonds}], :game-event :deal-cards-to-player, :seat-no 0}
                    {:enable-actions {:bid (list 10 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 2}
                    {:amount 10, :game-event :bid, :seat-no 2}
                    {:enable-actions {:bid (list 10 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 0}
                    {:amount 10, :game-event :bid, :seat-no 0}
                    {:enable-actions {:bid (list 10 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 1}
                    {:amount 20, :game-event :bid, :seat-no 1}
                    {:enable-actions {:bid (list 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 2}
                    {:cards [{:rank 8, :suit :diamonds} {:rank 13, :suit :spades} {:rank 14, :suit :clubs}], :game-event :reverse, :seat-no 2}
                    {:game-event :fold, :seat-no 2}
                    {:enable-actions {:bid (list 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 0}
                    {:cards [{:rank 4, :suit :spades} {:rank 13, :suit :hearts} {:rank 10, :suit :diamonds}], :game-event :reverse, :seat-no 0}
                    {:amount 50, :game-event :bid, :seat-no 0}
                    {:enable-actions {:bid (list 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 1}
                    {:amount 50, :game-event :bid, :seat-no 1}
                    {:enable-actions {:bid (list 100), :fold true, :reverse false}, :game-event :current-player, :seat-no 0}
                    {:game-event :fold, :seat-no 0}
                    {:amount 140, :game-event :winner, :seat-no 1}
                    {:game-event :game-over}]))

(facts "win with pk test"
       (let [actions [{:game-action :join-room :seat-no 0 :player-id "zzwu" :player-info {:bank 1000}}
                      {:game-action :join-room :seat-no 1 :player-id "ddy" :player-info {:bank 1000}}
                      {:game-action :join-room :seat-no 2 :player-id "cdf" :player-info {:bank 1000}}
                      {:game-action :ready :seat-no 0 :player-id "zzwu"}
                      {:game-action :ready :seat-no 1 :player-id "ddy"}
                      {:game-action :ready :seat-no 2 :player-id "cdf"}
                      {:game-action :start :seed 2}
                      {:game-action :bid :amount 10 :player-id "cdf"}
                      {:game-action :bid :amount 10 :player-id "zzwu"}
                      {:game-action :bid :amount 20 :player-id "ddy"}
                      {:game-action :reverse :player-id "cdf"} {:game-action :fold :player-id "cdf"}
                      {:game-action :reverse :player-id "zzwu"} {:game-action :bid :amount 50 :player-id "zzwu"}
                      {:game-action :bid :player-id "ddy" :amount 50}
                      {:game-action :pk :pk-with-seat-no 1 :player-id "zzwu"}
                      ]
             [room events]
             (loop [as actions
                    es []
                    r (room-constructor)]
               (if (seq as)
                 (let [curr-events (play-action r (first as))
                       new-room (reduce handle-game-event r curr-events)]
                   (recur (rest as) (into es curr-events) new-room))
                 [r es]))]
         (into {} room) => {:base 10, :dealer 1,
                            :history-bids [], :last-bid 10, :max-bid 100, :max-player-count 6, :min-add 10, :pot 0,
                            :seats {0 {:player-id "zzwu", :player-info {:bank 1080}, :seat-no 0, :state :init},
                                    1 {:player-id "ddy", :player-info {:bank 930}, :seat-no 1, :state :init},
                                    2 {:player-id "cdf", :player-info {:bank 990}, :seat-no 2, :state :init}}}
         events => [{:game-event :join-room, :player-id "zzwu", :player-info {:bank 1000}, :seat-no 0}
                    {:game-event :join-room, :player-id "ddy", :player-info {:bank 1000}, :seat-no 1}
                    {:game-event :join-room, :player-id "cdf", :player-info {:bank 1000}, :seat-no 2}
                    {:game-event :ready, :seat-no 0}
                    {:game-event :ready, :seat-no 1}
                    {:game-event :ready, :seat-no 2}
                    {:game-event :start-game, :into-game-seats [2 1 0]}
                    {:dealer 1, :game-event :dealer}
                    {:cards [{:rank 8, :suit :diamonds} {:rank 13, :suit :spades} {:rank 14, :suit :clubs}], :game-event :deal-cards-to-player, :seat-no 2}
                    {:cards [{:rank 4, :suit :clubs} {:rank 9, :suit :spades} {:rank 10, :suit :hearts}], :game-event :deal-cards-to-player, :seat-no 1}
                    {:cards [{:rank 4, :suit :spades} {:rank 13, :suit :hearts} {:rank 10, :suit :diamonds}], :game-event :deal-cards-to-player, :seat-no 0}
                    {:enable-actions {:bid (list 10 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 2}
                    {:amount 10, :game-event :bid, :seat-no 2}
                    {:enable-actions {:bid (list 10 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 0}
                    {:amount 10, :game-event :bid, :seat-no 0}
                    {:enable-actions {:bid (list 10 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 1}
                    {:amount 20, :game-event :bid, :seat-no 1}
                    {:enable-actions {:bid (list 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 2}
                    {:cards [{:rank 8, :suit :diamonds} {:rank 13, :suit :spades} {:rank 14, :suit :clubs}], :game-event :reverse, :seat-no 2}
                    {:game-event :fold, :seat-no 2}
                    {:enable-actions {:bid (list 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 0}
                    {:cards [{:rank 4, :suit :spades} {:rank 13, :suit :hearts} {:rank 10, :suit :diamonds}], :game-event :reverse, :seat-no 0}
                    {:amount 50, :game-event :bid, :seat-no 0}
                    {:enable-actions {:bid (list 20 50), :fold true, :reverse true}, :game-event :current-player, :seat-no 1}
                    {:amount 50, :game-event :bid, :seat-no 1}
                    {:enable-actions {:bid (list 100), :fold true, :reverse false}, :game-event :current-player, :seat-no 0}
                    {:amount 200, :game-event :bid, :pk true, :seat-no 0}
                    {:game-event :pk-result, :loser-no 1, :winner-no 0}
                    {:amount 340, :game-event :winner, :seat-no 0}
                    {:game-event :game-over}]))

(defn to-json-message [msg] (json/write-str (assoc msg :message :game-event)))