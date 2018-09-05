(ns googlesheets-sql-sync.util)

(defmacro try-http [msg & body]
  `(try
     ~@body
     (catch Exception e#
       (let [d# (ex-data e#)]
         (throw (ex-info (str "failed to " ~msg ": " (:status d#) "\n" (:body d#))
                         (select-keys d# [:status :body])))))))
