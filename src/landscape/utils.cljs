(ns landscape.utils
   (:require [sablono.core :as sab :include-macros true :refer-macros [html]]))

(defn inc-time [state-atom]
  (swap! state-atom update :time-cur  #(+ 100 %)))
(defn wrap-state [state-atom chunk]
  (html [:div.wrapped 
         chunk
         [:div
          [:button {:onClick (fn [_] (inc-time state-atom))} "+100ms"]
          (str " time: " (:time-cur @state-atom))]]))

(defn now [] (.getTime (js/Date.)))


(defn prob-gets-points?
  [prob]
  "prob is between 0 and 100. rand-int can return 0"
  ;   0    >= 1-100
  ;  100   >= 1-100
  (>= prob (inc (rand-int 99))))
