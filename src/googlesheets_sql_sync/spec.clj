(ns googlesheets-sql-sync.spec
  (:require
   [clojure.java.jdbc.spec :as jdbc-spec]
   [clojure.spec.alpha :as s]
   [googlesheets-sql-sync.util :refer [valid-url?]]))

(s/def ::str-not-empty (s/and string? not-empty))

(s/def ::table ::str-not-empty)
(s/def ::spreadsheet_id ::str-not-empty)
(s/def ::target ::str-not-empty)
(s/def ::sheets (s/coll-of (s/keys :req-un [::table]
                                   ::spreadsheet_id
                                   ::target)))

(s/def ::targets (s/map-of keyword?
                           ::jdbc-spec/db-spec))

(s/def ::client_id ::str-not-empty)
(s/def ::client_secret ::str-not-empty)
(s/def ::redirect_uri valid-url?)
(s/def ::access_token ::str-not-empty)
(s/def ::expires_in pos-int?)
(s/def ::refresh_token ::str-not-empty)

(s/def ::google_credentials (s/keys :req-un [::client_id
                                             ::client_secret
                                             ::redirect_uri]
                                    :opt-un [::access_token
                                             ::expires_in
                                             ::refresh_token]))

(s/def ::days nat-int?)
(s/def ::hours nat-int?)
(s/def ::minutes nat-int?)
(s/def ::seconds nat-int?)
(s/def ::interval (s/and (s/keys :req-un [(or ::days
                                              ::hours
                                              ::minutes
                                              ::seconds)])))

(defn- targets-exist?
  "Check if targets in sheets actually exist"
  [{:keys [sheets targets]}]
  (every? #(targets (keyword (:target %))) sheets))

(comment
  (= true targets-exist? {:sheets [{:target "a"} {:target "b"}]
                          :targets {:a 1 :b {}}})
  (= false (targets-exist? {:sheets [{:target "a"}]
                            :targets {:b 1}})))

(s/def ::config (s/and (s/keys :req-un [::sheets
                                        ::targets
                                        ::google_credentials
                                        ::interval])
                       targets-exist?))

(defn valid-config
  "Validates config data.
  Throws error containing spec violation."
  [data]
  (when-not (s/valid? ::config data)
    (throw (Exception. (s/explain-str ::config data))))
  data)
