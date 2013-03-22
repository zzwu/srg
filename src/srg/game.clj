(ns srg.game)

;; {:game-type1 [game1 game2 game3 ...]
;;  ;game-type2 [game1 game2 game3 ...]
;;  :game-type3 [game1 game2 game3 ...]}
(def games (atom {}))

(defn new-game
  [type])

(defn play-game-action
  [game-inst action])

(defn start-game!
  [type players]
  )