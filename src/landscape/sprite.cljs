(ns landscape.sprite
  (:require
   [sablono.core :as sab :include-macros true :refer-macros [html]]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]))

(defn sprite-create
  [{ :keys [ width height frame-size frames url dur-ms] :as sprit-info }]
  (assoc sprit-info
         :total-size (* frames frame-size)
         :half-width (/ width 2)))

(def well (sprite-create {:width 53 :height 47
                          :frame-size 53 :frames 7
                          :dur-ms 500
                          :url "imgs/well_sprites.png"}))
(def mine (sprite-create {:width 53 :height 47
                          :frame-size 53 :frames 7
                          :dur-ms 500
                          :url "imgs/mine_sprites.png"}))

(def avatars
  {:lizard (sprite-create {:width 68 :height 70
                           :frame-size 68 :frames 2
                           :dur-ms 500
                           :url "imgs/lizard_blue.png"})
  :astro (sprite-create {:width 50 :height 75
                         :frame-size 50 :frames 3
                         :dur-ms 500
                         :url "imgs/astronaut.png"})
  :alien (sprite-create {:width 47 :height 48
                         :frame-size 47 :frames 2
                         :dur-ms 500
                         :url "imgs/alien-green.png"})
  :robot (sprite-create {:width 32 :height 32
                         :frame-size 32 :frames 2
                         :dur-ms 500
                         :url "imgs/robot-blue.png"})
  :shark (sprite-create {:width 83 :height 80
                         :frame-size 83 :frames 2
                         :dur-ms 500
                         :url "imgs/shark.png"})
})

(def avatar (:lizard avatars))

(defn y-offset [{:keys [height] :as sprite} direction]
  (* height (case direction :down 0 :left 1 :right 2 :up 3 0)))

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


(defn avatar-disp "draw avatar traveling a direction"
    [{:keys [time-cur sprite-picked] :as state}
     {:keys [direction active-at] :as avatar}]
  (let [sprite (if (nil? sprite-picked) avatar (get avatars sprite-picked))
        tstep (get-step time-cur active-at (:dur-ms sprite))
        this-css (css sprite tstep)
        this-y-offset (y-offset sprite direction)
        this-css (merge this-css {:background-position-y (* -1 this-y-offset)})]
    (html [:div.avatar {:style this-css}])))
