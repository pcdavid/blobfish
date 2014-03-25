(ns blobfish.core-test
  (:require [clojure.test :refer :all]
            [blobfish.core :refer :all]))

(deftest config-test
  (testing "Configuration validation"
    (is (not (valid-config? default-config)))
    (is (not (valid-config? {:port 8080 })))
    (is (not (valid-config? {:repo "."})))
    (is (valid-config? {:port 8080 :repo "."}))
    (is (valid-config? {:port 8080 :repo "." :foo "bar"}))))

(deftest uri-parsing-test
  (testing "Request URI parsing"
    (is (nil? (parse-uri "")))
    (is (nil? (parse-uri "/")))
    (is (nil? (parse-uri "master")))
    (is (nil? (parse-uri "/master")))
    (is (= {:branch "master" :path ""} (parse-uri "/master/")))
    (is (= {:branch "master" :path "content.jar"} (parse-uri "/master/content.jar")))
    (is (= {:branch "master" :path "plugins/bundle.jar"} (parse-uri "/master/plugins/bundle.jar")))))

(run-tests)
