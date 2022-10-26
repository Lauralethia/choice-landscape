(ns landscape.utils
   (:require [sablono.core :as sab :include-macros true :refer-macros [html]]))

(defn inc-time [state-atom inc]
  (swap! state-atom update :time-cur  #(+ inc %)))
(defn wrap-state [state-atom chunk]
  (html [:div.wrapped 
         chunk
         [:div
          [:button {:onClick (fn [_] (inc-time state-atom -100))} "-100ms"]
          [:button {:onClick (fn [_] (inc-time state-atom 100))} "+100ms"]
          (str " time: " (:time-cur @state-atom))]]))

(defn now [] (.getTime (js/Date.)))


(defn prob-gets-points?
  [prob]
  "prob is between 0 and 100. rand-int can return 0"
  ;   0    >= 1-100
  ;  100   >= 1-100
  (>= prob (inc (rand-int 99))))
(defn distance
  "eclidian distance"
  [a b ]
  (.pow js/Math
        (+ (.pow js/Math (- (:x b) (:x a)) 2)
           (.pow js/Math (- (:y b) (:y a)) 2))
        .5))
(defn collide?
  "euclidian: sqrt sum of squares"
  [a b]
  (let [thres 10]
    (< (distance a b) 
        thres)))

 ;(with-redefs [landscape.settings/current-settings (atom (-> @landscape.settings/current-settings (assoc-in [:nTrials :pairsInBlock] 1) (assoc-in [:nTrials :devalue] 1)(assoc-in [:nTrials :devalue-good] 1)))] (landscape.core/gen-well-list))
(defn cumsum [v]
  (loop [in v
         out []]
    (if (= (count in) 0)
      out
      (recur (rest in)
             (conj out (+ (first in) (last out) ))))))

(defn iti-ideal-end
  "add :iti-ideal-end to well list for MR timing
  (map :iti-ideal-end (iti-ideal-end 2 [{:iti-dur 1} {:iti-dur 2} {}] 1))
  (1 5 8)
  TODO: if :catch-dur, ideal-trial-time will be different (no feedback or walk)
  IGNORE: need to add iti+end to last ideal-end -- handled by phase "
  [ideal-trial-time well-list default-iti-time]
  (let
      [iti-durs  (map #(or (:iti-dur %) default-iti-time) well-list)
       end-times (cumsum (map #(+ % ideal-trial-time) iti-durs))
       ;; first iti happens before ideal-trial-time
       ;; rm that from all elements
       end-times (map #(- %  ideal-trial-time) end-times)]
      (vec (map #(merge %1 {:iti-ideal-end %2}) well-list end-times))))
