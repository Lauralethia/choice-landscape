(ns landscape.view
  (:require
   [landscape.sprite :as sprite]
   [landscape.utils :as utils]
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


(defn position-at
  [{:keys [x y] :as pos} inner]
  (html [:div.abs {:style {:position "absolute"
                           :left (str x "px ")
                           :top (str y "px")}}
         inner]))

;; 
;; scene components
(defn water [fill]
  (html [:img#water {:src "imgs/water.png" :style {:scale (str fill "%")}}]))



(defn well-show "draw single well. maybe animate sprite"
    [{:keys [time-cur] :as state}
     {:keys [active-at] :as well}]
  (let [step (sprite/get-step time-cur active-at (:dur-ms sprite/well))
        css (sprite/css sprite/well step)]
    (html [:div.well {:style css}])))

(defn avatar-disp "draw avatar traveling a direction"
    [{:keys [time-cur] :as state}
     {:keys [direction active-at] :as avatar}]
  (let [step (sprite/get-step time-cur active-at (:dur-ms sprite/avatar))
        css (sprite/css sprite/avatar step)
        y-offset (case direction :down 0 :left 70 :right 140 :up 211 0)
        css (merge css {:background-position-y (* -1 y-offset)})]
    (html [:div.well {:style css}])))


(defn well-pos
  [side step]
  (let [center-x 250
        bottom-y 260
        step-size 100
        move-by (* step step-size)]
    (case side
      :left  {:x (- center-x move-by) :y bottom-y}
      :up    {:x center-x             :y (- bottom-y move-by)}
      :right {:x (+ center-x move-by) :y bottom-y}
      {:x 0 :y 0})))

(defn well-side
  "side is :left :up :right"
  [{:keys [wells] :as state} side]
  (position-at (well-pos side (get-in wells [side :step]))
               (well-show state (side wells))))

(defn well-show-all
  "3 wells not all equadistant. sprite for animate"
  [{:keys [wells] :as state}]
  (html [:div.wells
         (well-side state :left)
         (well-side state :up)
         (well-side state :right)]))


(defn display-state
  "html to render for display. updates for any change in display"
  [state] (sab/html
           [:div#background
            ;; draw wells
            ;; draw avatar
            ;; draw arrows
            ;; draw blocked
            ;; draw feedback
            (water 50)
            (well-show-all state)
            (position-at (get-in state [:avatar :pos])
                         (avatar-disp state (:avatar state)))
            ]))


(defcard well0
  "well animation by steps. should animate with js/animate"
  (fn [state owner]
    (utils/wrap-state state (well-show @state @state)))
  {:time-cur 100 :active-at 100})

(defcard avatar
  "step through avatar"
  (fn [state owner]
    (html [:div
           (utils/wrap-state state (avatar-disp @state @state))
           [:button {:on-click (fn [] (swap! state assoc :direction :left))} "left"]
           [:button {:on-click (fn [] (swap! state assoc :direction :right))} "right"]
           [:button {:on-click (fn [] (swap! state assoc :direction :up))} "up"]
           [:button {:on-click (fn [] (swap! state assoc :direction :down))} "down"]
           [:br]
           (str @state)
           ]))
  {:time-cur 100 :active-at 100 :direction :left})
