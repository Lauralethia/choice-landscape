(ns landscape.wells-test
  (:require
   [clojure.test :refer [is deftest]] 
   [landscape.model.wells :as wells]
   [landscape.fixed-timing :refer [trials]]))


(deftest add-only-if-pos
  (let [well-nopos {:side :left :step 1 }
        well-pos (assoc well-nopos :pos {:x 0 :y 0})]
    (is (= 0 (->  (wells/add-pos-if-missing :right well-pos) :pos :x)))
    (is (< 200 (->  (wells/add-pos-if-missing :right well-nopos) :pos :x)))) )
(deftest add-positions-test
  (is (> (-> trials :debug wells/list-add-pos (get-in [0 :right :pos :y])) 200)))
  
  
