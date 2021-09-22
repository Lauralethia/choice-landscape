(ns landscape.devcards
  (:require
   [devcards.core]
   [landscape.utils :as utils]
   [landscape.view :as view]
   [landscape.model :as model]
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
                                          model/move-avatar
                                          (update-in [:time-cur] #(+ 100 %)))))}
             "inc" ]
            [:button {:on-click (fn[](swap! state assoc-in [:avatar :pos :x] 0))}
             "reset"]]))
    
  {:avatar  {:pos {:x 0 :y 0}
             :destination {:x 100 :y 0}
             :active-at 0
             :last-move 0}
   :time-cur 0})

;; help from 
;; https://github.com/onetom/clj-figwheel-main-devcards
;; https://github.com/bhauman/devcards/issues/148
;; look to
;; http://localhost:9500/figwheel-extra-main/cards  ; auto
;; http://localhost:9500/cards.html                 ; manual
;(defcard example-card "hi")
(devcards.core/start-devcard-ui!)
