(ns srg.queue)

(def queues (atom {}))

(defn add-queue-player
  [queue type name]
  (update-in queue
             [type] #((fnil conj #{}) % name)))

(defn add-queue-player!
  [type name]
  (swap! queues add-queue-player type name))