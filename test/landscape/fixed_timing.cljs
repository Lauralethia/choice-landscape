(ns landscape.key-test
  (:require [landscape.fixed-timing :as ft]
            [clojure.test :refer [is deftest]]))
(deftest ideal-trial-time-test
  (is (= [7 11 14]
         (map :iti-ideal-end (ft/iti-ideal-end 4 2 [{:iti-dur 1} {:iti-dur 2} {}] 1)))))
