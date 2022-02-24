(ns landscape.core-test
  (:require
   [landscape.core :refer [gen-well-list vis-type-from-url photodiode-from-url]]
   [cemerick.url :as url]
   [landscape.settings :refer [current-settings]]
   ;[thinktopic.aljabr.core :as nd]
   [clojure.core.matrix :as m]
   [clojure.test :refer [is deftest]]))

(defn all-eq [s] (every? #(= % (first s)) s))

; 3*(19 + 16*2 + 8*2)
(def TOTAL-TRIALS 204)
;; will break if probs are not all distinct
;; 20 50 100
(def PROBS (-> @current-settings :prob vals sort)) 

;; (defmacro get-in-trial
;;   "using trial and side within body. doesn't work in cljs? probably wrong anway
;;   (get-in-trial wl (if (get-in trial [side :open]) 0 1)"

;;   [wl keys &body]
;;   `(map (fn[trial] (map (fn[side] (progn ,@body)) [:left :up :right])) ,wl))

(def side-keys [:left :up :right])
(def wl (gen-well-list))

(deftest generate-well-list
    (is (= (count wl) TOTAL-TRIALS))
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
;; pre 20211216 -- 2x reversal since removed. now 20 deval, or 24 (instead of 16 or 35)
;; {(0 20 50) 35,
;;  (0 100 100) 16,
;;  (100 100 0) 16,
;;  (100 0 100) 16,
;;  (0 50 20) 16,
;;  (100 20 0) 35,
;;  (100 0 50) 35,
;;  (100 50 0) 16,
;;  (100 0 20) 16}

;;  deval each side equally (20 each)
(deftest same-number-trial-sides-deval
  (is (all-eq (map #(f %) (filter #(= (deval-type %) :deval) (keys f))))))

(deftest same-number-trial-sides-learn-20
  (is (every? #{20} (map #(f %) (filter #(= (deval-type %) :deval) (keys f))))))

;; if we had additional reversals or uneven block trial counts we could check here
;; but all are equal (24 trials per prob permutation)
(deftest same-number-trial-sides-learn
  (is (every? #{24} (map #(f %) (filter #(= (deval-type %) :learn) (keys f))))))

(deftest vis-type 
  (is (= :mountain (vis-type-from-url {:anchor "mountain-anything" }) ))
  (is (= :desert (vis-type-from-url {:anchor "anything" }) ))
  (is (= :desert (vis-type-from-url {})))
  (is (= :desert (vis-type-from-url {:anchor nil})))
  ;; test page unlikely to have anchor and less likey to have 
  (is (= :desert (vis-type-from-url (-> js/window .-location .-href url/url)))))

(deftest photodiode
  (is (photodiode-from-url {:anchor "mountain&photodiode"}))
  (is (not (photodiode-from-url {:anchor "desert"})))
  (is (not (photodiode-from-url {}))))
