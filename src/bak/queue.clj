(ns srg.queue)

(def queues (ref {}))

(def queues-config {"guess" {:game-player-count 2}})

(defn add-queue-player
  [queue type name]
  (update-in queue
             [type] #((fnil conj []) % name)))

(defn queue-info
  [name queues]
  (some (fn [type]
          (if (some #(= name %) (get queues type))
            type))
        (keys queues)))

(defn add-queue-player!
  [type name]
  (if-let [game-config (get queues-config type)]
    (if-let [queued-type  (queue-info name @queues)]
      (throw (Exception. (str "already in " queued-type " queue!")))
      (dosync
       (alter queues add-queue-player type name)
       (let [q (get @queues type)]
         (when (= (:game-player-count game-config) (count q))
           (alter queues dissoc type)
           q))))
    (throw (Exception. (str "no game type : " type "!")))))
