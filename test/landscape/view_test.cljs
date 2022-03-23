(ns landscape.view-test
  (:require [landscape.view :refer [photodiode-color]]
            ;[cljs.test :as t :include-macros true]
            [clojure.test :refer [is deftest]]
))


(deftest photodiode-color-test
  ;; default color
  (is (= "black" (photodiode-color {:name "dasfsafd"})))
  ;; neighboring phases do not have the same color
  (is (not (= (photodiode-color {:name :iti})
              (photodiode-color {:name :chose})))))
