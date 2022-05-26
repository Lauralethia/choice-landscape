(ns landscape.floater-test
  (:require 
   [cljs.test :as t :include-macros true]
   [landscape.model.floater :as f]
   [clojure.test :refer [is deftest]]))

(deftest create-floater-test
  (let [flt (f/floater-new (f/->pos 0 0))]
    (is (= 0 (-> flt :pos :x)))
    (is (= 0 (-> flt :pos :y)))
    (is (= 50 (-> flt :step-off)))))

(deftest use-floater-test
  
  (let [moved (-> (f/->pos 0 0) (f/floater-new) (f/move-up))]
    (is (> 0 (get-in moved [:pos :y]) )) ; moving up is more negative y
    (is (= 1 (get-in moved [:step-cur])))
    (is (> 1 (get-in moved [:alpha])))))

(deftest rand-floater-test
  (let [rand (-> (f/->pos 0 0) (f/floater-new) (f/rand-init))]
    (is (>= (:size rand)  50))
    (is (<= (:size rand) 100))
    (is (>= (:alpha rand) .5))
    (is (<= (:alpha rand)  1))))

