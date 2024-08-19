(ns control-surface.endpoints
  (:require [clojure.string :as str]
            #_[clojure.tools.logging :as log]
            [control-surface.db :as db]
            [control-surface.models :refer [map->ServerRegistrationRequest map->ServerUnregisterRequest]]
            [control-surface.tg-commands :refer [send-md]]
            [control-surface.util :refer [api-response]]
            [control-surface.constants :as const]))

(defn get-token-header [req] (->> req
                                  :headers
                                  (map #(map clojure.string/lower-case %))
                                  (filter #(= (first %) "x-cs-token"))
                                  first
                                  second))

(defn ep-consume-task-output [req token]
  (let [text (->> req :body
                  (group-by :taskName)
                  (map (fn [[k v]]
                         (str "Output of " k ":\n```\n"
                              (->> v
                                   (sort-by :timestamp)
                                   (map :text)
                                   (str/join "\n"))
                              "\n```")))
                  (str/join "\n\n"))
        hd (get-token-header req)
        u (db/get-user-by-token hd)]
    (send-md (:id u) token (str text))
    text)
  (api-response 200 true "OK"))

(defn ep-register-server [req token]
  (let [hd (get-token-header req)
        u (db/get-user-by-token hd)
        r (-> req :body map->ServerRegistrationRequest (dissoc :forceUpdate))
        force-upd (-> req :body :forceUpdate)
        uid (:id u)]
    (if (some? u)
      (if (db/try-register-server r uid force-upd)
        (do
          (when force-upd (send-md uid token (-> r :serverName const/successful-registration)))
          (api-response 200 true "OK"))
        (do
          (send-md uid token (-> r :serverName const/failed-registration))
          (api-response 400 false "Failed to register server")))
      (api-response 403 false "User is not registered"))))

(defn ep-unregister-server [req token]
  (let [hd (get-token-header req)
        u (db/get-user-by-token hd)
        uid (:id u)
        rq (-> req :body map->ServerUnregisterRequest)
        nm (:serverName rq)]
    (if (db/try-unregister-server nm uid)
      (do (send-md uid token (str "Server " nm " unregistered successfully"))
          (api-response 200 true "OK"))
      (do (send-md uid token (str "Failed to unregister " nm))
          (api-response 400 false "Failed to unregister")))))

(defn not-found []
  {:status 404
   :body "Not found."
   :headers {"Content-Type" "text/plain"}})