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
  (let [idea [34 34 34]]
    (is (= (vals (get-lens :mrA1)) [34 34 34]))
    (is (= (vals (get-lens :mrA2)) [34 34 34]))
    (is (= (vals (get-lens :mrB1)) [34 34 34]))
    (is (= (vals (get-lens :mrB2)) [34 34 34]))))
