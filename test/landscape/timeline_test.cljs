(ns landscape.timeline-test
  (:require  [cljs.test :as t :include-macros true]
             [landscape.model.timeline :refer [side-probs]]
             [clojure.test :refer [is deftest]]))
   
(deftest side-probs-test
  (is (= (first (side-probs 20 50 :left))
         {:up 20 ,:right 50,:left 100 :side-best :left}))
  (is (= (first (side-probs 80 20 50 :left))
         {:up 20 ,:right 50,:left 80 :side-best :left})))
