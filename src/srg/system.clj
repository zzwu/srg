(ns srg.system
  (:require [srg.data.user-info :as user-data]
            [srg.protocols :as p]))

(defrecord system [name]
  p/UserDataService
  (register [this user-info]
    (user-data/register user-info))
  (p/load-user-info [this username]
    (user-data/load-user username))
  (p/verify [this username password]
    (let [info (user-data/load-user username)]
      (and (= username (:username info))
           (= password (:password info))))))