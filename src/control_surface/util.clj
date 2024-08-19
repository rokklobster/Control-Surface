(ns control-surface.util
  (:require [clojure.data.json :as json]
            [java-time.api :as jt]))

(defonce rx #"(((?<!\\)\".*?(?<!\\)\")|((?<!\\)\'.*?(?<!\\)\')|((\S|(?<=\\)\s)+))+")

(defn api-response [code ok msg]
  {:status code
   :body (json/write-str {:ok ok :message msg})
   :headers {"Content-Type" "application/json"}})

(defn split-args [s]
  (->> s
       (re-seq rx)
       (map first)
       (map #(let [c (count %)
                   lp (- c 1) 
                   f (get % 0)
                   l (get % lp)]
               (cond
                 (not= l f) % 
                 (#{\' \"} l) (subs % 1 lp)
                 :else %)))))

(defn utc-now-str [] (-> "UTC" jt/zone-id jt/zoned-date-time str))