(ns landscape.view
  (:require
   [landscape.sprite :as sprite]
   [landscape.utils :as utils]
   [landscape.model :as model]
   [landscape.instruction :as instruction]
   [cljsjs.react]
   [cljsjs.react.dom]
   [sablono.core :as sab :include-macros true :refer-macros [html]]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
   [cljs.core.async :refer [<! chan sliding-buffer put! close! timeout]])
  (:require-macros [devcards.core :refer [defcard]]))

;;
(def ^:export DEBUG
  "show phase edn? display will no longer be pixel perfect
  use in javscript console like: landscape.view.DEBUG = true"
  false)

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
                           :transform (str "translate(" x "px, " y "px")}}
         inner]))

;; 
;; scene components
(defn water-fill [fill]
  (html [:img#water {:src "imgs/water.png" :style {:scale (str fill "%")}}]))

(defn water [state]
  (let [fill (get-in state [:water :scale])]
    (water-fill fill)))


(def bucket (html [:img {:src "imgs/bucket.png" :style {:transform "translate(20px, 30px)"}}]))

(defn well-show "draw single well. maybe animate sprite"
        [{:keys [time-cur] :as state}
         {:keys [active-at score open] :as well}]
        (let [tstep (sprite/get-step time-cur active-at (:dur-ms sprite/well))
              css (sprite/css sprite/well tstep)
              ;; if not score, move bg down to get the failed well offset
              v-offset (if score {}
                           {:background-position-y
                            (str "-" (:height sprite/well) "px")}
                           )]
          (html [:div.well {:style (merge css v-offset)}
                 (if open bucket)])))

(defn avatar-disp "draw avatar traveling a direction"
    [{:keys [time-cur sprite-picked] :as state}
     {:keys [direction active-at] :as avatar}]
  (let [sprite (if (nil? sprite-picked) sprite/avatar (get sprite/avatars sprite-picked))
        tstep (sprite/get-step time-cur active-at (:dur-ms sprite))
        css (sprite/css sprite tstep)
        y-offset (sprite/y-offset sprite direction)
        css (merge css {:background-position-y (* -1 y-offset)})]
    (html [:div.well {:style css}])))

(defn instruction-disp "show instruction text box" [state]
  (let [inst (:instruction state)]
    (if inst
      (position-at (:pos inst)
                   (html [:div#instruction (:text inst) ])))))


(defn well-side
  "side is :left :up :right"
  [{:keys [wells] :as state} side]
  (position-at (get-in wells [side :pos])
               (well-show state (side wells))))

(defn well-show-all
  "3 wells not all equadistant. sprite for animate"
  [{:keys [wells] :as state}]
  (html [:div.wells
         (well-side state :left)
         (well-side state :up)
         (well-side state :right)]))


(defn instruction-view [{:keys [phase] :as state}]
  (let [idx (or (:idx phase) 0)
        instr (get instruction/INSTRUCTION idx)
        pos (or (:pos instr) {:x 100 :y 100})]
    (position-at pos
                 (html [:div#instruction (:text instr)]))))

(defn display-state
  "html to render for display. updates for any change in display"
  [{:keys [phase avatar] :as state}] (sab/html
           [:div#background
            (if DEBUG [:div {:style {:color "white"}} (str phase)])
            (water state)
            (well-show-all state)
            (position-at (get-in state [:avatar :pos])
                         (avatar-disp state avatar))
            ;; TODO?
            ;; draw arrows
            ;; draw blocked
            ;; draw feedback

            ;; instructions on top so covers anything else
            ;; -- maybe we want them under?
            (if (= :instruction (:name phase)) (instruction-view state))
            ]))


;; 
;; debug/devcards
(defcard well-score-nil
  "well animation by steps. should animate with js/animate"
  (fn [state owner]
    (utils/wrap-state state (well-show @state @state)))
  {:time-cur 100 :active-at 100})
(defcard well-score1
  "well animation by steps. should animate with js/animate"
  (fn [state owner]
    (utils/wrap-state state (well-show @state @state)))
  {:time-cur 100 :active-at 100 :score 1})

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
           [:select {:on-change #(swap! state assoc :sprite-picked (-> % .-target .-value keyword))}
            (map #(html [:option { :value (name %)} (name %)]) (keys sprite/avatars))]
           [:br]
           (str @state)
           ]))
  {:time-cur 100 :active-at 100 :direction :left})
