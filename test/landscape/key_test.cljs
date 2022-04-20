(ns landscape.key-test
  (:require [landscape.key :as key]
            [clojure.test :refer [is deftest]]))
(deftest key-to-dir-lookup-test
  "uses current settings. might not be set? use with-redefs?"
  (is (= (key/side-from-keynum 37) :left))
  (is (= (key/side-from-keynum 38) :up)))

