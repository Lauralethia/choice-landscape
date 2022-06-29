(ns landscape.key-test
  (:require [landscape.key :as key]
            [landscape.settings :refer [current-settings mri-glove-keys]]
            [clojure.test :refer [is deftest]]))
(deftest key-to-dir-lookup-test
  "uses current settings. might not be set? use with-redefs?"
  (is (= (key/side-from-keynum 37) :left))
  (is (= (key/side-from-keynum 38) :up)))

(deftest instruction-lookup
  "instructions take any key"
  (with-redefs [current-settings (atom (assoc @current-settings :keycodes mri-glove-keys))]
    (is (nil? (key/side-from-keynum 37)))
    (is (= (key/side-from-keynum 50) :left))
    (is (= (key/side-from-keynum-instructions 37) :left))
    (is (= (key/side-from-keynum-instructions 50) :left))))

