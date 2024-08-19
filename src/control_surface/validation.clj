(ns control-surface.validation
  (:require [clojure.string :refer [blank?]]))

(defn valid-config? [c]
  (and
   (-> c :botToken blank? not)
   (some? (:port c))
   (> (:port c) 0)))