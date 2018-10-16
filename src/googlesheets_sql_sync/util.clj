(ns googlesheets-sql-sync.util
  "Collection of utility functions for generic tasks around timing and networking and files"
  (:import (java.net URI))
  (:require
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [jsonista.core :as json]
   [org.httpkit.client :refer [url-encode]]))

(s/def ::str-not-empty (s/and string? not-empty))

(defn fail [& args]
  (throw (Exception. (apply str args))))

; Thanks https://gist.github.com/apeckham/78da0a59076a4b91b1f5acf40a96de69
(defn get-free-port
  "Let Java find a free port for you."
  []
  (let [socket (java.net.ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

(defn valid-port? [p]
  (< 0 p 0x10000))

(defn valid-url? [s]
  (try
    (.toURL (java.net.URI. s))
    true
    (catch Exception e false)))

(defn query-string
  "Returns URL-encoded query string for given params map."
  [m]
  (->> m
       (map (fn [[k v]]  (str (url-encode (name k)) "=" (url-encode v))))
       (string/join "&")))

(defn hostname [s]
  (.getHost (new URI s)))

(defn now []
  (.getTime (java.util.Date.)))

(defn with-duration
  "Invokes f, then calls cb with duration of calling f."
  [f cb]
  (let [t (now)]
    (f)
    (cb (- (now) t))))

(defn with-tempfile
  "Creates a temporary file, calls f with file as argument
   and deletes file after."
  [filename ext f]
  (let [file (java.io.File/createTempFile filename ext)]
    (f file)
    (.delete file)))

(defn write-json-file
  "Write data as JSON to file"
  [file data]
  (json/write-value (io/file file) data (json/object-mapper {:pretty true})))

(defn read-json-file
  "Read data as JSON from file"
  [file]
  (json/read-value file (json/object-mapper {:decode-key-fn true})))

