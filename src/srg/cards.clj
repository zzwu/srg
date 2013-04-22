(ns srg.cards)

;;cards

(def suits [:hearts :spades :clubs :diamonds])

;;(2 3 4 5 6 7 8 9 10 J Q K A)
(def ranks (reverse (range 2 15)))

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