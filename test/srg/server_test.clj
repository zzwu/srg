(ns srg.server-test
  (:use [srg.server]
        [midje.sweet]))

(facts "test room-id-and-empty-seat-no"
       (let [id 1
             room {:seats {0 "zzwu" 1 "cdf"} :max-player-count 6}]
         (room-id-and-empty-seat-no id room) => [1 2]))