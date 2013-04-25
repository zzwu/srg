(ns srg.zjh-poker
  (:refer-clojure :exclude [flush])
  (:require [srg.cards :as cards]))

(def patterns [:bomb :straight-flush :flush :straight :pair :high-card])

(defn score
  [[parent first second third]]
  (apply + (map * [third second first parent]
                (iterate #(* % 15) 1))))

(defn sort-by-rank-reverse
  [cards]
  (into []
        (reverse (sort-by :rank cards))))

(defn bomb
  "vector in, vector out."
  [cards]
  (if (apply = (map :rank cards))
    cards))

(defn flush
  [cards]
  (if (apply = (map :suit cards))
    (sort-by-rank-reverse cards)))

(defn serial?
  [nums]
  (let [l (drop-last nums)
        r (map dec (rest nums))]
    (every? true? (map = l r))))

(defn straight
  [cards]
  (if (serial? (sort (map :rank cards)))
    (sort-by-rank-reverse cards)))

(defn straight-flush
  [cards]
  (if (and (straight cards) (flush cards))
    (sort-by-rank-reverse cards)))

(defn pair-num
  [nums]
  (for [l (range (count nums))
        r (range (count nums))
        :when (not= l r)]
    (if (= (nums l) (nums r)) (nums l))))

(defn pair
  [cards]
  (if-let [pair-rank (first (map identity
                                 (pair-num (into [] (map :rank cards)))))]
    (let [pair-cards (filter #(= pair-rank (:rank %)) cards)
          left-card (first (filter #(not= pair-rank (:rank %)) cards))]
      [(first pair-cards) (second pair-cards) left-card])))

(defn high-card
  [cards]
  (sort-by-rank-reverse cards))

(def patterns-matchfns (map #(vector %1 %2) patterns [bomb straight-flush flush straight pair high-card]))

(defn index-of
  [type patterns]
  (first
   (keep-indexed #(if (= type %2) %1) patterns)))

(defn match-result
  [type cards]
  (let [pattern-index (index-of type patterns)
        score-factor (into [(- (count patterns) pattern-index)] (map :rank cards))]
    {:pattern type
     :score (score score-factor)
     :cards cards}))

(defn match
  [cards]
  (filter identity
       (map (fn [[pattern matchfn]]
              (if-let [res (matchfn cards)]
                (match-result pattern res))) patterns-matchfns)))

(defn diff352?
  "花色不同的352"
  [cards]
  (let [ranks-set (set (map :rank cards))
        suit-set (set (map :suit cards))]
    (and (= #{3 5 2} ranks-set)
         (= 3 (count suit-set)))))

(defn pk
  [left-cards right-cards]
  (cond
   (and (diff352? left-cards) (bomb right-cards)) :win
   (and (bomb left-cards) (diff352? right-cards)) :lose
   :else (let [left-cards-score (:score (first (match left-cards)))
               right-cards-score (:score (first (match right-cards)))]
           (cond
            (> left-cards-score right-cards-score) :win
            ;;平牌比牌的输
            (<= left-cards-score right-cards-score) :lose))))

