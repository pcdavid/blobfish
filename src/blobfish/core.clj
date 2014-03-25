(ns blobfish.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [me.raynes.conch.low-level :as sh]
            [clojure.data.json :as json])
  (:gen-class))

(def default-config {:port 8080})

(defn load-config [args]
  (if (= (count args) 1)
    (let [file (java.io.FileReader. (nth args 0))]
      (merge default-config (json/read-json file)))
    default-config))

(defn valid-config? [config]
  (every? #(contains? config %) #{:port :repo}))

(defn parse-uri [uri]
  (let [path (if (.startsWith uri "/") (.substring uri 1) uri)
        sep (.indexOf path "/")]
    (when-not (= sep -1)
      {:branch (.substring path 0 sep)
       :path   (.substring path (inc sep))})))

(defn git [repo cmd & args]
  (apply sh/proc (apply conj ["git" (str "--git-dir=" repo) cmd] args)))

(defn git-show [repo branch path]
  (git repo "show" (str branch ":" path)))

(defn git-branch-exists? [repo branch]
  (let [p (git repo "rev-parse" branch)]
    (and (empty? (slurp (:err p)))
         (>= (count (slurp (:out p))) 41))))

(defn handle-locally [repo branch path]
  (if (git-branch-exists? repo branch)
    (let [p (git-show repo branch path)
          err (> (.available (:err p)) 0)]
      (if (not err)
        {:status 200
         :headers {"Content-Type" "application/octet-stream"}
         :body (:out p)}
        (resp/not-found (:err p))))
    (resp/not-found (str "Branch '" branch "' does not exist in '" repo "'"))))

(defn handle-remotely [remote branch path]
  (resp/redirect (str remote branch "/" path)))

(defn find-remote [config branch]
  (let [key (keyword branch)
        rule (first (filter #(contains? % key) (:remotes config)))]
    (get rule key)))

(defn dispatcher [config request]
  (if-let [query (parse-uri (:uri request))]
    (if-let [remote (find-remote config (:branch query))]
      (handle-remotely remote (:branch query) (:path query))
      (handle-locally (:repo config) (:branch query) (:path query)))
    (resp/not-found (str "Invalid path " (:uri request)))))

(defn -main [& args]
  (let [config (load-config args)]
    (if (valid-config? config)
      (jetty/run-jetty (partial dispatcher config) {:port (:port config)})
      (do
        (.println System/err "Invalid of missing configuration.")
        (System/exit 1)))))
