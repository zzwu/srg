(ns srg.protocols)

(defprotocol GameState
  (update [this game-event]))


(defprotocol GameRules
  (play [this action]))