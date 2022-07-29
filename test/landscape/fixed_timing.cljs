(ns landscape.fixed-timing-test
  (:require [landscape.fixed-timing :as ft]
            [landscape.utils :as utils]
            [clojure.test :refer [is deftest]]))
(deftest ideal-trial-time-test
  (is (= [1 5 8]
         (map :iti-ideal-end (utils/iti-ideal-end 2 [{:iti-dur 1} {:iti-dur 2} {}] 1)))))
