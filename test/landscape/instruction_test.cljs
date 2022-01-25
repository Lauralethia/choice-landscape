(ns landscape.instruction-test
(:require
 [landscape.instruction :as instruction]
 [landscape.settings :as settings]
 [clojure.test :refer [is deftest]]))

;; moving right changes x
(deftest well-mine
  (swap! settings/current-settings  assoc :vis-type "mountain")
  (is (= (instruction/item-name :well) "mine")))

(deftest well-well
  (swap! settings/current-settings  assoc :vis-type "desert")
  (is (= (instruction/item-name :well) "well")))
