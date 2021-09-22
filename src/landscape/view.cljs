(ns landscape.view
  (:require
   [landscape.sprite :as sprite]
   [cljsjs.react]
   [cljsjs.react.dom]
   [sablono.core :as sab :include-macros true :refer-macros [html]]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
   [cljs.core.async :refer [<! chan sliding-buffer put! close! timeout]])
  (:require-macros [devcards.core :refer [defcard]]))

(def CHANGE-DOM-ID "id of element where updates will go"
  "main-container")
(defn change-dom
  "replace body w/ sab/html element"
  [reactdom]
    (let [node (.getElementById js/document CHANGE-DOM-ID)]
      (.render js/ReactDOM reactdom node)))


;; 
;; scene components
(defn water [fill]
  (html [:img#water {:src "imgs/water.png" :style {:scale (str fill "%")}}]))



(defn well-one "draw single well. maybe animate sprite"
    [{:keys [time-cur] :as state}
     {:keys [name center-distance active-at] :as well}]
  (let [step (sprite/get-step time-cur active-at (:dur-ms sprite/well))
        css (sprite/css sprite/well step)]
    (html [:div.well {:style css}])))

(defn avatar-disp "draw avatar traveling a direction"
    [{:keys [time-cur] :as state}
     {:keys [direction active-at] :as avatar}]
  (let [step (sprite/get-step time-cur active-at (:dur-ms sprite/avatar))
        css (sprite/css sprite/avatar step)
        y-offset (case direction :down 0 :left 70 :right 140 :up 210 0)
        css (merge css {:background-position-y (* -1 y-offset)})]
    (html [:div.well {:style css}])))

(defn well-all
  "3 wells not all equadistant. sprite for animate"
  [state wells]
  (well-one state {:name :left :center-distance 2 :active-at nil }))

(defn position-at
  [{:keys [x y] :as pos} inner]
  (html [:div.abs {:style {:position "absolute"
                           :top (str x "px ")
                           :left (str y "px")}}
         inner]))

(defn display-state
  "html to render for display. updates for any change in display"
  [state] (sab/html
           [:div#background
            ;; draw wells
            ;; draw avatar
            (position-at (get-in state [:avatar :pos])
                         (avatar-disp state (:avatar state)))
            ;; draw arrows
            ;; draw blocked
            ;; draw feedback
            (water 50)
            ]))


(defn inc-time [state-atom]
  (swap! state-atom update :time-cur  #(+ 100 %)))
(defn wrap-state [state-atom chunk]
  (html [:div.wrapped 
         chunk
         [:div
          [:button {:onClick (fn [_] (inc-time state-atom))} "+100ms"]
          (str " time: " (:time-cur @state-atom))]]))

(defcard well0
  "well animation by steps. should animate with js/animate"
  (fn [state owner]
    (wrap-state state (well-one @state @state)))
  {:time-cur 100 :active-at 100})

(defcard avatar
  "step through avatar"
  (fn [state owner]
    (html [:div
           (wrap-state state (avatar-disp @state @state))
           [:button {:on-click (fn [] (swap! state assoc :direction :left))} "left"]
           [:button {:on-click (fn [] (swap! state assoc :direction :right))} "right"]
           [:button {:on-click (fn [] (swap! state assoc :direction :up))} "up"]
           [:button {:on-click (fn [] (swap! state assoc :direction :down))} "down"]
           [:br]
           (str @state)
           ]))
  {:time-cur 100 :active-at 100 :direction :left})
