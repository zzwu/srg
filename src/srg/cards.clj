(ns srg.cards)

;;cards

(def suits [:hearts :spades :clubs :diamonds])

;;(2 3 4 5 6 7 8 9 10 J Q K A)
(def ranks (reverse (range 2 15)))

(defn shuffle-random
  "Return a random permutation of coll"
  [^java.util.Collection coll seed]
  (let [al (java.util.ArrayList. coll)]
    (java.util.Collections/shuffle al (java.util.Random. seed))
    (clojure.lang.RT/vector (.toArray al))))

(defn card [rank suit]
  {:rank rank :suit suit})

(def all-cards
  (for [suit suits
        rank ranks]
    (card rank suit)))

(defn obscure-card [card]
  (-> card
      (dissoc :rank :suit)
      (assoc :facedown true :id (str (gensym)))))

;;一副牌
(def pokers
  (into [{:suit :king :rank 16} {:suit :king :rank 17}]
        (for [s suits r ranks] {:suit s :rank r})))

(defn- random-index-suit
  [total]
  (loop [result []
         left (into [] (range 0 total))]
    (if (empty? left)      result
      (let [curr (get left (rand-int (count left)))]
        (recur (conj result curr)
               (vec (remove #(= % curr) left)))))))

(defn random-deck
  "获取一副洗过的牌"
  [pokers]
  (map #(nth pokers %) (random-index-suit (count pokers))))