(ns control-surface.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [mount.core :as mount]))

(def uri "jdbc:sqlite:surface.db3")

(declare db)

(defn on-start []
  (let [spec {:connection-uri uri}
        conn (jdbc/get-connection spec)
        res (assoc spec :connection conn)
        init-script (slurp "./resources/db-init.sql")
        vec (str/split init-script #"--cmdcut")]
    (log/info "got a connection to db")
    (jdbc/db-do-commands res vec)
    res))

(defn on-stop []
  (-> db :connection .close)
  (log/info "connection closed")
  nil)

(mount/defstate
  ^{:on-reload :noop}
  db
  :start (on-start)
  :stop (on-stop))

(defn get-user-by-token [token]
  (when-first [res (jdbc/find-by-keys db :users {:token token})]
    res))

(defn get-user [id]
  (jdbc/get-by-id db :users id))

(defn remove-user [id]
  (try
    (jdbc/delete! db :users ["id = ?" id])
    (jdbc/delete! db :registrations ["userId = ?" id])
    (catch Exception e
      (log/warn "Failure upon user deletion:\n" e))))

(defn try-add-user [id token]
  (try
    (jdbc/insert! db :users {:id id :token token})
    true
    (catch Exception e
      (do (log/warn "failed to register user\n" e)
          nil))))

(defn get-server [name uid] 
  (first (jdbc/find-by-keys db :registrations {:userId uid :serverName name})))

(defn get-servers [uid]
  (jdbc/find-by-keys db :registrations {:userId uid}))

(defn try-register-server [m uid force]
  (try
    (let [item (assoc m :userId uid)
          fnd (get-server (:serverName m) uid)]
      (if (-> fnd some?)
        (if force
          (-> (jdbc/update! db :registrations item ["userId = ?" uid])
              first
              (> 0))
          true)
        (-> (jdbc/insert! db :registrations item) first some?)))
    (catch Exception e
      (log/warn "Failed to register " (:serverName m) " for " uid ":\n" e)
      false)))

(defn try-unregister-server [name uid]
  (try
    (jdbc/delete! db :registrations ["userId = ? and serverName = ?" uid name])
    true
    (catch Exception e
      (log/warn "Failed to unregister " name " for " uid ":\n" e)
      false)))