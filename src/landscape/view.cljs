(ns landscape.view
  (:require
   [landscape.sprite :as sprite]
   [landscape.utils :as utils]
   [landscape.model :as model]
   [landscape.instruction :as instruction]
   [landscape.model.survey :as survey]
   [landscape.key :as key]
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


(defn well-side
  "side is :left :up :right"
  [{:keys [wells] :as state} side]
  (position-at (get-in wells [side :pos])
               (html [:div {:on-click #(key/sim-key side)}
                      (well-show state (side wells))])))

(defn well-show-all
  "3 wells not all equadistant. sprite for animate"
  [{:keys [wells] :as state}]
  (html [:div.wells
         (well-side state :left)
         (well-side state :up)
         (well-side state :right)]))

(defn button-keys [] (html [:div.bottom
                               [:button {:on-click #(key/sim-key :left)} "< "]
                               [:button {:on-click #(key/sim-key :up)} "^"]
                               [:button {:on-click #(key/sim-key :right)} " >"]]))
(defn instruction-view [{:keys [phase] :as state}]
        (let [idx (or (:idx phase) 0)
              instr (get instruction/INSTRUCTION idx)
              pos-fn (or (:pos instr) (fn[_] {:x 0 :y 0}))]
          (position-at (pos-fn state)
                       (html [:div#instruction
                              [:div.top (str (inc idx) "/" (count instruction/INSTRUCTION))]
                              [:br]
                              ((:text instr) state)
                              [:br]
                              (button-keys)]))))
(defn survey-view [{:keys [phase] :as state}]
  (let [qi (or  (:qi phase) 0)
        ci (or  (:choice-i phase) 0)
        quest (get-in survey/SURVEYS [qi :q])
        choices (get-in survey/SURVEYS [qi :answers])
        cur-choice (or (nth choices ci) "ALL DONE")
        ]
    (position-at {:x 100 :y 10}
                 (html [:div#insturctions 
                   [:div#instruction
                    [:div.top (str (inc qi) "/" (count survey/SURVEYS))]
                    [:h3 quest]
                    [:ul#pick (mapv
                               #(html [:li { ;; :id  TODO
                                            :class
                                            (if (= cur-choice %) "picked" "ignored")} %])
                               choices)]
                    (button-keys)]]))))

(defn done-view [state]
  (position-at {:x 100 :y 10}
               (html [:div#instruction "You finished! Thank you!"])))

(defn display-state
  "html to render for display. updates for any change in display"
  [{:keys [phase avatar] :as state}]
  (let [avatar-pos (get-in state [:avatar :pos])]
    (sab/html
     [:div#background
      (if DEBUG [:div {:style {:color "white"}} (str phase)])
      (water state)
      (well-show-all state)
      (position-at avatar-pos
                   ;; NB. this conditional is only for display
                   ;; we're waiting regardless of whats shown
                   (if (= :iti (:name phase))
                     (html [:div.iti "+"])
                     (sprite/avatar-disp state avatar)))

      ;; TODO?
      ;; draw arrows
      ;; draw feedback

      ;; instructions on top so covers anything else
      ;; -- maybe we want them under?
      (case (:name phase) 
        :instruction  (instruction-view state)
        :survey (survey-view state)
        :done (done-view state)
        nil)])))



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
           (utils/wrap-state state (sprite/avatar-disp @state @state))
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
