(ns landscape.sprite
  (:require [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]))

(defn sprite-create
  [{ :keys [ width height frame-size frames url dur-ms] :as sprit-info }]
  (assoc sprit-info
         :total-size (* frames frame-size)
         :half-width (/ width 2)))

(def well (sprite-create {:width 53 :height 46
                          :frame-size 53 :frames 8
                          :dur-ms 500
                          :url "imgs/well_sprites.png"}))

(def avatar (sprite-create {:width 70 :height 70
                          :frame-size 70 :frames 3
                          :dur-ms 500
                          :url "imgs/lizard_blue.png"}))


(defn get-step
  "0 started to 1 finished"
  [cur start-at dur]
  (let[ delta (- cur start-at)
       time (mod delta dur)]
    (if (= start-at 0) 0 (/ time dur))))

(defn bg-pos [{:keys [frame-size frames] :as sprite } step]
        (let [frame-num (int  (.toFixed (* step frames) 0))]
          (* -1 frame-size frame-num)))

(defn css [{:keys [url width height half-width] :as sprite-info} step]
  {:width (str width "px")
   :height (str  height "px")
   :background-image (str  "url("url")")
   :background-position-x (str (bg-pos sprite-info step) "px")
   :background-position-y "0px"         ;; to be overwritten if image includes more than one sprite
   :background-repeat "no-repeat"})
