(ns srg.zjh-poker-test
  (:refer-clojure :exclude [flush])
  (:use [srg.zjh-poker]
        [midje.sweet]))

(facts "test bomb"
       (bomb [{:rank 3, :suit :spades} {:rank 5, :suit :spades} {:rank 4, :suit :spades}]) => nil
       (bomb [{:rank 3, :suit :spades} {:rank 3, :suit :hearts} {:rank 3, :suit :diamonds}]) => [{:rank 3, :suit :spades} {:rank 3, :suit :hearts} {:rank 3, :suit :diamonds}])

(facts "test flush"
       (flush [{:rank 3, :suit :spades} {:rank 3, :suit :hearts} {:rank 3, :suit :diamonds}]) => nil
       (flush [{:rank 3, :suit :spades} {:rank 5, :suit :spades} {:rank 4, :suit :spades}]) => [{:rank 5, :suit :spades} {:rank 4, :suit :spades} {:rank 3, :suit :spades}])

(facts "test straight"
       (straight [{:rank 7, :suit :spades} {:rank 5, :suit :spades} {:rank 4, :suit :spades}]) => nil
       (straight [{:rank 3, :suit :spades} {:rank 5, :suit :spades} {:rank 4, :suit :spades}]) => [{:rank 5, :suit :spades} {:rank 4, :suit :spades} {:rank 3, :suit :spades}])

(facts "test straight-flush"
       (straight-flush [{:rank 5, :suit :spades} {:rank 4, :suit :spades} {:rank 7, :suit :spades}]) => nil
       (straight-flush [{:rank 5, :suit :spades} {:rank 4, :suit :spades} {:rank 3, :suit :spades}]) => [{:rank 5, :suit :spades} {:rank 4, :suit :spades} {:rank 3, :suit :spades}])

(facts "test pair"
       (pair [{:rank 5, :suit :spades} {:rank 4, :suit :spades} {:rank 3, :suit :spades}]) => nil
       (pair [{:rank 5, :suit :spades} {:rank 5, :suit :hearts} {:rank 3, :suit :spades}]) => [{:rank 5, :suit :spades} {:rank 5, :suit :hearts} {:rank 3, :suit :spades}])

(facts "test high-card"
       (high-card [{:rank 5, :suit :spades} {:rank 3, :suit :hearts} {:rank 5, :suit :spades}]) => [{:rank 5, :suit :spades} {:rank 5, :suit :spades} {:rank 3, :suit :hearts}])

(facts "test match"
       (match [{:rank 5, :suit :spades} {:rank 5, :suit :spades} {:rank 3, :suit :hearts}]) =>  (list {:cards [{:rank 5, :suit :spades} {:rank 5, :suit :spades} {:rank 3, :suit :hearts}], :pattern :pair, :score 7953} {:cards [{:rank 5, :suit :spades} {:rank 5, :suit :spades} {:rank 3, :suit :hearts}], :pattern :high-card, :score 4578})
       (match [{:rank 3, :suit :spades} {:rank 5, :suit :spades} {:rank 4, :suit :spades}]) => (list {:cards [{:rank 5, :suit :spades} {:rank 4, :suit :spades} {:rank 3, :suit :spades}], :pattern :straight-flush, :score 18063} {:cards [{:rank 5, :suit :spades} {:rank 4, :suit :spades} {:rank 3, :suit :spades}], :pattern :flush, :score 14688} {:cards [{:rank 5, :suit :spades} {:rank 4, :suit :spades} {:rank 3, :suit :spades}], :pattern :straight, :score 11313} {:cards [{:rank 5, :suit :spades} {:rank 4, :suit :spades} {:rank 3, :suit :spades}], :pattern :high-card, :score 4563}))


(facts "test win?"
       (win? [{:rank 5, :suit :spades} {:rank 2, :suit :diamonds} {:rank 3, :suit :hearts}] [{:rank 5, :suit :spades} {:rank 5, :suit :diamonds} {:rank 5, :suit :hearts}]) => true
       (win? [{:rank 5, :suit :spades} {:rank 5, :suit :diamonds} {:rank 5, :suit :hearts}] [{:rank 5, :suit :spades} {:rank 2, :suit :diamonds} {:rank 3, :suit :hearts}]) => false
       (win? [{:rank 5, :suit :spades} {:rank 4, :suit :diamonds} {:rank 3, :suit :hearts}] [{:rank 5, :suit :spades} {:rank 5, :suit :diamonds} {:rank 5, :suit :hearts}]) => false
       (win? [{:rank 5, :suit :spades} {:rank 4, :suit :spades} {:rank 3, :suit :spades}] [{:rank 5, :suit :spades} {:rank 5, :suit :diamonds} {:rank 5, :suit :hearts}]) => false
       (win? [{:rank 5, :suit :spades} {:rank 4, :suit :diamonds} {:rank 3, :suit :hearts}] [{:rank 5, :suit :diamonds} {:rank 3, :suit :diamonds} {:rank 4, :suit :hearts}]) => false)