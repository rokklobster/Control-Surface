(ns control-surface.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.pprint :as pprint]
            [clojure.tools.logging :as log]
            [compojure.core :as comp]
            [compojure.route :as route]
            [control-surface.db :as db]
            [control-surface.endpoints :as eps]
            [control-surface.models :refer [map->Options]]
            [control-surface.tg-commands :as cmd]
            [control-surface.validation :refer [valid-config?]]
            [morse.handlers :as h]
            [morse.polling :as tg]
            [mount.core :as mount]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.request :as request]))

(defonce server (atom nil))
(defonce cfg (atom nil))
(defonce bot-chan (atom nil))

(comp/defroutes routes
  (comp/POST "/api/tasks/log" req (eps/ep-consume-task-output req (:botToken @cfg)))
  (comp/DELETE "/api/servers" req (eps/ep-unregister-server req (:botToken @cfg)))
  (comp/POST "/api/servers" req (eps/ep-register-server req (:botToken @cfg)))
  (route/not-found (eps/not-found)))

(def app
  (-> routes
      wrap-keyword-params
      (wrap-json-body {:keywords? true})
      wrap-params))

(defn log-request [req]
  (log/info "request:"
            (:request-method req)
            (:uri req)
            (request/content-type req)
            (request/content-length req)))

(defn start-server [c]
  (reset! server
          (jetty/run-jetty
           (fn [req] (log-request req) (app req))
           {:port (:port c)
            :join? false})))

(defn stop-server []
  (when-some [s @server]
    (.stop s)
    ;; (.close s)
    (reset! server nil)))

(defn restart-server []
  (stop-server)
  (start-server nil))

(h/defhandler bot
  (h/command "start" req (cmd/cmd-help req (:botToken @cfg)))
  (h/command "help" req (cmd/cmd-help req (:botToken @cfg)))
  (h/command "schedule" req (cmd/cmd-schedule-task req (:botToken @cfg)))
  (h/command "token" req (cmd/cmd-query-user-data req (:botToken @cfg)))
  (h/command "register" req (cmd/cmd-register req (:botToken @cfg)))
  (h/command "unregister" req (cmd/cmd-unregister req (:botToken @cfg)))
  (h/command "query" req (cmd/cmd-query-tasks req (:botToken @cfg)))
  (h/command "servers" req (cmd/cmd-query-servers req (:botToken @cfg)))
  (h/command "cancel" req (cmd/cmd-cancel-task req (:botToken @cfg))))

(defn hook-shutdown [_]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. ^Runnable
                     #(do (tg/stop @bot-chan)
                          (stop-server)
                          (mount/stop #'db/db)))))

(defn hook-database [_]
  (mount/start #'db/db))

(defn -main [& _]
  (log/info "running from " (System/getProperty "user.dir"))
  (let [conf (json/read-str (slurp "./config.json") :key-fn #(keyword %))]
    (reset! cfg (map->Options conf))
    (log/info "read config: " (with-out-str (pprint/pprint cfg)))
    (let [chan (tg/start (:botToken @cfg) bot)]
      (reset! bot-chan chan)
      (cond
        (valid-config? @cfg) (-> @cfg
                                 start-server
                                 hook-database
                                 hook-shutdown)
        :else (log/error "Config is invalid, can't start server")))))
