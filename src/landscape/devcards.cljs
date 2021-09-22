(ns landscape.devcards
  (:require
   [devcards.core]
   [landscape.view]
   [sablono.core :as sab :include-macros true :refer-macros [html]])
  (:require-macros [devcards.core :refer [defcard]]))
(enable-console-print!)
(defcard testcard (html [:div "hi"]))
;; help from 
;; https://github.com/onetom/clj-figwheel-main-devcards
;; https://github.com/bhauman/devcards/issues/148
;; look to
;; http://localhost:9500/figwheel-extra-main/cards  ; auto
;; http://localhost:9500/cards.html                 ; manual
;(defcard example-card "hi")
(devcards.core/start-devcard-ui!)
