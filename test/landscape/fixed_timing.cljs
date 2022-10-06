(ns landscape.fixed-timing-test
  (:require [landscape.fixed-timing :as ft]
            [landscape.utils :as utils]
            [clojure.test :refer [is deftest]]))
(deftest ideal-trial-time-test
  (is (= [1 5 8]
         (map :iti-ideal-end (utils/iti-ideal-end 2 [{:iti-dur 1} {:iti-dur 2} {}] 1)))))

(defn cnt-reps [vec]  (reduce (fn[x y] (update x y inc)) {} vec))
(defn get-choices [timing]
  (map (fn[t] (filter (fn[s] (get-in t [s :open]))
                      [:left :up :right]))
       timing))
(defn get-lens [tid]
  (-> ft/trials tid get-choices cnt-reps))
(deftest check-sides-represented
  (let [ideal [34 34 34]]
    (is (= (vals (get-lens :mrA1)) ideal))
    ;; (is (= (vals (get-lens :mrA2)) ideal))
    (is (= (vals (get-lens :mrB1)) ideal))
    ;; (is (= (vals (get-lens :mrB2)) ideal))
    ))

(defn block-counts
  "get count of block(up prob value) and open (left u right) like
   50truefalsetrue 12,
   50truetruefalse  11,
   100falsetruetrue  13,
   100truetruefalse  11, ..." 
  [tid]
  (reduce
   (fn [m [k v]] (assoc m k (count v)))
   {}
   (group-by (fn [x]
               (str
                (get-in x [:up :prob])
                (get-in x [:left :open])
                (get-in x [:up :open])
                (get-in x [:right :open])))
             (tid ft/trials))))

(deftest nearly-balenced 
  (is (= #{11 12} (set (vals (block-counts :mrA1))))) 
  (is (= #{11 12} (set (vals (block-counts :mrB1))))))
