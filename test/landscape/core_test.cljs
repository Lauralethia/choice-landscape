(ns landscape.core-test
  (:require
   [landscape.core :refer [gen-well-list]]
   [landscape.settings :refer [BOARD]]
   ;[thinktopic.aljabr.core :as nd]
   [clojure.core.matrix :as m]
   [clojure.test :refer [is deftest]]))

(defn all-eq [s] (every? #(= % (first s)) s))

; 3*(19 + 16*2 + 8*2)
(def TOTAL-TRIALS 201)
;; will break if probs are not all distinct
;; 20 50 100
(def PROBS (-> BOARD :prob vals sort)) 

;; (defmacro get-in-trial
;;   "using trial and side within body. doesn't work in cljs? probably wrong anway
;;   (get-in-trial wl (if (get-in trial [side :open]) 0 1)"

;;   [wl keys &body]
;;   `(map (fn[trial] (map (fn[side] (progn ,@body)) [:left :up :right])) ,wl))

(def side-keys [:left :up :right])
(def wl (gen-well-list))

(deftest generate-well-list
    (is (= (count wl) 201))
    (is (-> wl count (mod 3) (= 0))))

;; 2d array 1/0 for if open. hardcoded key for same order on every row
(def mat2d-side (map (fn[trial]
                        (map (fn[side]
                               (if (get-in trial [side :open]) 1 0))
                             side-keys)) wl))
(def mat2d-prob (map (fn[trial]
                    (map (fn[side]
                           (if (get-in trial [side :open]) (get-in trial [side :prob]) 0))
                         side-keys)) wl))
(deftest sides-count-eq
    (is (->> mat2d-side m/rows (apply m/add) all-eq)))

(deftest prob-count-eq
   (is (->> mat2d-side m/rows (apply m/add) all-eq))) 

(defn deval-type [probs] (if (>= (count (filter #(= 100 %) probs)) 2) :deval :learn))
;; (deval-type [100 100 0]) :deval
;; (deval-type [100 50 0]) :learn

(def f (frequencies mat2d-prob))
;; {(0 20 50) 35,
;;  (0 100 100) 16,
;;  (100 100 0) 16,
;;  (100 0 100) 16,
;;  (0 50 20) 16,
;;  (100 20 0) 35,
;;  (100 0 50) 35,
;;  (100 50 0) 16,
;;  (100 0 20) 16}

;;  16 for all deval
(deftest same-number-trial-sides-deval
  (is (all-eq (map #(f %) (filter #(= (deval-type %) :deval) (keys f))))))

;; 16 for each side-prob combination seen for one block (reversal 1)
;; 35 for init (additional trials) and reversal 2
;; should maybe check freq of the :learn counts (3 16s and 3 35s)
(deftest same-number-trial-sides-learn
  (is (every? #{16 35} (map #(f %) (filter #(= (deval-type %) :learn) (keys f))))))
