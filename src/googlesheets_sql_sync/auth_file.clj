(ns googlesheets-sql-sync.auth-file
  (:refer-clojure :exclude [get])
  (:require
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [googlesheets-sql-sync.util :as util :refer [fail]]))

(s/def ::access_token ::util/str-not-empty)
(s/def ::expires_in pos-int?)
(s/def ::refresh_token ::util/str-not-empty)

(s/def ::google-auth (s/keys :req-un [::access_token
                                      ::expires_in
                                      ::refresh_token]))

(defn- valid
  "Validates auth data.
  Throws error containing spec violation."
  [data]
  (when-not (s/valid? ::google-auth data)
    (fail (s/explain-str ::google-auth data)))
  data)

(defn get
  "Read auth from JSON file, validate and return it."
  [auth-file]
  (let [f (io/file auth-file)]
    (when (.exists f)
      (valid (util/read-json-file f)))))

(defn save
  "Validate data and write to auth-file."
  [auth-file data]
  (util/write-json-file auth-file (valid data)))
