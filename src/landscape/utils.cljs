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
