(ns srg.guess
  (:use [srg.utils]))

(def init-guess-room
  {:type :guess
   :same-times 0
   :current-player-index 0})

(defhandler do-guess
  [items room username] [guess-type]
  )

(defn play-action
  [room action]
  (case (first action)
    "new-game" ))
