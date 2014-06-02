(ns blobfish.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [me.raynes.conch.low-level :as sh]
            [clojure.data.json :as json])
  (:gen-class))

(def default-config
  "The defaults to use if no configuration file is specified."
  {:port 8080})

(defn load-config
  "Loads configurations parameters from the file passed as first
  argument, if any. Returns the effective configuration to use."
  [args]
  (merge default-config
         (when (= (count args) 1)
           (json/read-json (java.io.FileReader. (args 0))))))

(defn valid-config?
  "Check that the specified configuration map contains all mandatory
  parameters"
  [config]
  (every? #(contains? config %) #{:port :repo}))

(defn parse-uri [uri]
  (let [path (if (.startsWith uri "/") (.substring uri 1) uri)
        sep (.indexOf path "/")]
    (when-not (= sep -1)
      {:branch (.substring path 0 sep)
       :path   (.substring path (inc sep))})))

;; We will need to invoke various git subcommands; this is a generic
;; wrapper for doing that. It uses conch to perform the actual
;; invocation, and return a conch map describing the resulting
;; process, which can be used to access the process's status, stdout,
;; stderr etc.

(defn git
  "Invokes a git command with the specified arguments, in the context
  of a particular repo. The repo must be the absolute path of a local
  .git directory."
  [repo cmd & args]
  (apply sh/proc (into ["git" (str "--git-dir=" repo) cmd] args)))

(defn git-show
  [repo rev path]
  (git repo "show" (str rev ":" path)))

(defn git-rev-parse
  [repo rev]
  (let [p (git repo "rev-parse" rev)]
    (if (empty? (slurp (:err p)))
      (let [h (slurp (:out p))]
        (.substring h 0 (dec (count h)))))))

(defn git-branch-exists? [repo branch]
  (if-let [rev (git-rev-parse repo branch)]
    (= (count rev) 40)))

(defn git-tree
  [repo rev]
  (git-rev-parse repo (str rev "^{tree}")))

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
