(ns srg.zjh-test
  (:use [srg.zjh-rules]
        [midje.sweet]))

(def join-room-msg {:player-id "zzwu" :player-info {:bank 100} :seat-no 3 :game-event :join-room})

(facts "test handle join-room event"
       (let [room init-room
             except-seat-3 {:player-id "zzwu" :player-info {:bank 100} :seat-no 3}]
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
                       {:game-event :winner :seat-no 0 :amount 110}]
             new-room (reduce handle-game-event init-room messages)]
         new-room => {:base 10, :dealer 0,
                      :history-bids [{:amount 10, :seat-no 0} {:amount 10, :seat-no 1} {:amount 30, :seat-no 0} {:amount 30, :seat-no 1} {:amount 30, :seat-no 0}],
                      :last-bid 30,
                      :max-bid 100, :max-player-count 6, :min-add 10,
                      :pot 110,
                      :seats {0 {:amount 70, :cards [{:rank 6, :suit :clubs} {:rank 8, :suit :clubs} {:rank 5, :suit :clubs}], :player-id "zzwu", :player-info {:bank 140}, :seat-no 0, :state :in},
                              1 {:amount 40, :cards [{:rank 3, :suit :clubs} {:rank 7, :suit :clubs} {:rank 10, :suit :clubs}], :player-id "cdf", :player-info {:bank 60}, :seat-no 1, :state :out}},
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
