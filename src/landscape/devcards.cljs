(ns landscape.devcards
  (:require
   [devcards.core]
   [landscape.utils :as utils]
   [landscape.view :as view]
   [landscape.http :as http]
   [landscape.model.survey :as survey]
   [landscape.model :as model]
   [landscape.model.avatar :as avatar]
   [landscape.model.points :as points]
   [landscape.model.floater :as floater]
   [landscape.model.phase :as phase]
   [landscape.model.pile :as pile]
   [sablono.core :as sab :include-macros true :refer-macros [html]])
  (:require-macros [devcards.core :refer [defcard]]))
(enable-console-print!)

(defcard move
  "positioning to destition"
  (fn [state own]
     (html [:div
            (view/position-at (-> @state :avatar :pos) (html [:div "*"]))
            [:br]
            (str @state)
            [:br]
            [:button {:on-click (fn[] (reset!
                                      state
                                      (-> @state 
                                          avatar/move-avatar
                                          (update-in [:time-cur] #(+ 100 %)))))}
             "inc" ]
            [:button {:on-click (fn[](swap! state assoc-in [:avatar :pos :x] 0))}
             "reset"]]))
    
  {:avatar  {:pos {:x 0 :y 0}
             :destination {:x 100 :y 0}
             :active-at 0
             :last-move 0}
   :time-cur 0})


;; (def water-atom-devcard
;;   (let [state (atom {:water {:level 5 :active-at 10} :time-cur 10})]
;;     (js/setInterval (fn[] (swap! state update :time-cur inc) ;; (reset! state
;;                          ;;          (-> @state
;;                          ;;              (update :time-cur #(mod (inc %) 2000))
;;                          ;;              (model/water-pulse)))
;;                       ))))

(defcard water-pulse
  "run through osilating water pulsing"
  (fn[state o]
       (html [:div (view/water @state)
           [:br]
           [:p (str @state)]
           [:button {:on-click (fn [_] (swap! state (fn[_] {:water {:level 5 :active-at 10} :time-cur 0} )))} "reset"]]
          ))
  {:water {:level 5 :active-at 10} :time-cur 10})

;; moving scoring point around
(defcard single-point-float-card
"what one point looks like"
  (let [point (points/new-point-floating 1 (points/->pos 10 10) (points/->pos 0 0))]
    (landscape.view/show-point-floating point)))
(defcard points-float-card
"score a point, float up"
  (let [state
        {:points-floating
         [(points/new-point-floating 1 (points/->pos 10 0) (points/->pos 0 0))
          (points/new-point-floating 2 (points/->pos 40 10) (points/->pos 0 0))]}]
    (landscape.view/show-points-floating state)))


;; catch trial zzzs
(defcard floater-one-devcard
  "single floating z"
  (fn [state o]
     (html [:div
            ;; [:div (str (into {} @state))]
            [:button {:on-click (fn[] (swap! state floater/move-up ))} "step" ]
            [:button {:on-click (fn[] (reset! state(floater/->floater 1 100 0 15 "z" (floater/->pos 20 -20))))} "reset" ]
           
            (view/show-zzz-floating @state)]))
   (atom  (floater/->floater 1 100 0 15 "z" (floater/->pos 20 -20))))

(defcard floater-devcard
  "catch trial 'z's floating and fading"
  (fn [state o]
    (html [:div
           [:button {:on-click (fn[] (swap! state floater/update-state))} "step" ]
           [:button {:on-click (fn[] (reset! state {:zzz (floater/zzz-new (floater/->pos 0 -30) 3) } ))} "reset" ]
           (view/show-all-zzz @state)
           [:div (str @state)]]))
  (atom {:zzz (floater/zzz-new (floater/->pos 0 0) 3) }))

(def coin-empty-state {:coins {} :record{:settings{:pile-y 30}}})
(defcard coin-pile-floaters
  "coins floater into pile"
  (fn [state o]
    (html [:div
           [:button {:on-click (fn[] (swap! state floater/coin-addto-state))} "new" ]
           [:button {:on-click (fn[] (swap! state floater/coin-update-state))} "step" ]
           [:button {:on-click (fn[] (reset! state coin-empty-state))} "reset" ]
           (view/show-all-floating-coins @state)]))
  (atom coin-empty-state)
  {:inspect-data true})

(defcard ttl-devcard
  "send ttl test"
  (fn [state o]
    (html [:div
           [:button
            {:on-click (fn[] (http/send-local-ttl
                              "http://localhost:8888"
                              (phase/gen-ttl (:phase @state) (:wells @state)) ))}
            "trigger" ]
           (str @state)
           [:div (str @state)]]))
  (atom {:phase {:name :chose}
         :wells {:left {:open true} :up {:open true} :right {:open false}}}))

;; 
(defcard pile-card
  "show sand pile advancing"
  (fn [state o]
    (let [{:keys [g w h]} @state]
      (html [:div
             [:canvas {:id "pile-example" :width w :height h :style {:border "solid black 5px"}}]
             [:br]
             [:button {:onClick (fn [_]
                                  (pile/grid-add-box g (pile/val-to-color) 0 0)
                                  (swap! state assoc :g g))}
              "add"]
             [:button {:onClick (fn [_]
                                  (pile/grid-gravity g w h)
                                  (swap! state assoc :g g)
                                  )}
              "inc"]
             [:button {:onClick (fn[_]
                                  (pile/image-draw (pile/get-ctx "pile-example") g w h))}
              "draw"]
             [:button {:onClick (fn[_] (swap! state assoc :g (pile/grid-make w h 0)))}
              "reset"]])))
  (atom {:w 20 :h 50 :g (pile/grid-make 200 300 0) })
  {:inspect-data false}
  )

(defcard pile2-quil "making the pile with quil"
  (fn [state _] 
    (let [{:keys [w h g]} @state]
      (html [:div [:canvas {:id "q-pile-dc" :height h :width w
                            :style {:border "solid 1px black"}}]])) )
  (atom {:w 200 :h 500 :g (pile/grid-make 200 300 0) }))

;; moved card from survey.cljs to avoid warnings
(defcard survey-forum
  "what does the survey look like. TODO: working forum"
  (fn [fa o]
    (html [:div (survey/view-questions)
           [:b (:age @fa)]
           [:button {:onclick (fn [_](reset! fa @survey/forum-atom))} "reset"]])
    )
  (atom {:age 10 :understand 0 :done false}))
;; help from 
;; https://github.com/onetom/clj-figwheel-main-devcards
;; https://github.com/bhauman/devcards/issues/148
;; look to
;; http://localhost:9500/figwheel-extra-main/cards  ; auto
;; http://localhost:9500/cards.html                 ; manual
;(defcard example-card "hi")
(devcards.core/start-devcard-ui!)
