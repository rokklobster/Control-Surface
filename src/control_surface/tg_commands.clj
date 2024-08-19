(ns control-surface.tg-commands
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [control-surface.constants :as const]
            [control-surface.db :refer [get-user remove-user try-add-user]]
            [control-surface.db :as db]
            [control-surface.models :refer [->TaskRunRequest ->TaskCancelRequest]]
            [control-surface.util :refer [split-args utc-now-str]]
            [morse.api :as tg]))

(defn send-md [req token text]
  (tg/send-text token
                (if (int? req) req (-> req :chat :id))
                {:parse_mode "Markdown"}
                text))

(defn cmd-help [req token]
  (send-md req token "The help!"))

(defn cmd-register
  "get registration token for servers"
  [req token]
  (let [utk (-> (clojure.core/random-uuid) str (str/replace "-" ""))
        uid (-> req :chat :id)
        inserted? (try-add-user uid utk)]
    (if inserted?
      (send-md req token (str "Your token is `" utk "`. Use it as a `userToken` parameter in confiuration of contorl-surface-client deployment"))
      (send-md req token "Failed to register. Are you already registered?"))))

(defn cmd-query-user-data [req token]
  (let [uid (-> req :chat :id)
        user (get-user uid)]
    (if (some? user)
      (send-md uid token (str "Your token value is `" (:token user) "`"))
      (cmd-help req token))))

(defn cmd-unregister [req token]
  (if (remove-user (-> req :chat :id))
    (send-md req token "You were unregistered, token voided. Register once again to use the bot.")
    (send-md req token "Failed to delete.")))

(defn third [s] (->> s (drop 2) first))

(defn cmd-schedule-task
  [req token]
  (let [text (:text req)
        uid (-> req :chat :id)
        cmd (drop 1 (str/split text #"\s+" 4))
        lnc (count cmd)]
    (if (= lnc 3)
      (let [srv (first cmd)
            reg (db/get-server srv uid)]
        (if (some? reg) 
          (let [tnm (second cmd)
                args (split-args (third cmd))
                rq (->TaskRunRequest tnm args "shell" (utc-now-str) srv)
                body (json/write-str rq)
                url (:pushurl reg)]
            (client/post url {:body body, :content-type :json})
            (send-md uid token const/schedule-ok))
          (send-md uid token const/schedule-server-missing)))
      (send-md uid token const/schedule-help))))

(defn cmd-cancel-task [req token]
  (let [text (:text req)
        uid (-> req :chat :id)
        cmd (drop 1 (str/split text #"\s+" 3))
        lnc (count cmd)]
    (if (= lnc 2)
      (let [srv (first cmd)
            tk (second cmd)
            reg (db/get-server srv uid)]
        (if (some? reg)
          (let [url (:cancelurl reg)
                rq (json/write-str (->TaskCancelRequest tk srv))]
            (client/post url {:body rq, :content-type :json})
            (send-md uid token const/cancel-processed))
          (send-md uid token (const/cancel-missing-server srv))))
      (send-md uid token const/cancel-help))))

(defn cmd-query-tasks [req token]
  (let [text (:text req)
        uid (-> req :chat :id)
        cmd (drop 1 (str/split text #"\s+" 2))
        lnc (count cmd)]
    (if (> lnc 0)
      (let [name (first cmd)
            reg (db/get-server name uid)]
        (if (some? reg)
          (let [url (:queryurl reg)
                res (client/get url)
                o (map #(str "`" (get % "name") "`: " (if (get % "alive") "running" "finished")) (-> res :body json/read-str (get "payload")))] 
            (send-md uid token (str/join "\n" o)))
          (send-md uid token (const/query-server-missing name))))
      (send-md uid token const/query-tasks-help))))

(defn cmd-query-servers [req token]
  (let [uid (-> req :chat :id)
        srvs (db/get-servers uid)
        rs (str "Registered servers:\n" (str/join "\n" (map #(str "`" (:servername %) "`: push to " (:pushurl %)) srvs)))]
    (send-md uid token rs)))