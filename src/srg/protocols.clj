(ns srg.protocols)

(defprotocol GameState
  (update [this game-event]))


(defprotocol GameRules
  (play [this action]))

(defprotocol UserDataService
  (register [this user-info])
  (load-user-info [this id]))