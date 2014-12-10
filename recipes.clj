;; https://wiki.eclipse.org/JGit/User_Guide#Concepts
;; http://www.codeaffine.com/2014/09/22/access-git-repository-with-jgit/
;; http://download.eclipse.org/jgit/docs/latest/apidocs/

(import 'org.eclipse.jgit.storage.file.FileRepositoryBuilder)

(defprotocol Coercions
  (as-repo [x] "Coerce argument to a Git Repository."))

(extend-protocol Coercions
  nil
  (as-repo [_] nil)

  String
  (as-repo [path] (as-repo (java.io.File. path)))

  java.io.File
  (as-repo [file] (-> (FileRepositoryBuilder.) (.setGitDir file) .readEnvironment .findGitDir .build)))

(defn git-show [repo-path branch path]
  (let [repo (as-repo repo-path)
        rev-spec (str branch ":" path)
        blob-id (.resolve repo rev-spec)]
    (-> repo (.open blob-id) .openStream)))

(def repo-path "/home/pcdavid/tmp/usage/repo/.git")
(def file-path "2.0.0-N20140919-050057/luna/targets/modules/acceleo-3.4.tpd")
(slurp (git-show repo-path "master" file-path))
