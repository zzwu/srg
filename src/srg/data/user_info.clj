(ns srg.data.user-info)

(def user-infos (atom {}))

(defn register [user-info]
  (swap! user-infos assoc (:username user-info) user-info))

(defn load-user [username]
  (get username @user-infos))