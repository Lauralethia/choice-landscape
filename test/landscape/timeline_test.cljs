(ns landscape.timeline-test
  (:require  [cljs.test :as t :include-macros true]
             [landscape.model.timeline :refer [side-probs gen-wells]]
             [clojure.test :refer [is deftest]]))
   
(deftest side-probs-test
  (is (= (first (side-probs 20 50 :left))
         {:up 20 ,:right 50,:left 100 :side-best :left}))
  (is (= (first (side-probs 80 20 50 :left))
         {:up 20 ,:right 50,:left 80 :side-best :left})))

(deftest have-iti
  (let [wells (gen-wells { :prob-low 20 :prob-high 50 :reps-each-side 1 :side-best :left})]
    (is (= 1000  (get-in wells [0 :iti-dur])))))
