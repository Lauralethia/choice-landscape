(ns landscape.model.points
  (:require [landscape.utils :refer [distance]]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]))
   

;; points that float up to the total score
;points-floating{:pos :points}
(defrecord pos [x y])
(defrecord point-floating [points progress start-distance ^pos pos ^pos dest])
(defn new-point-floating
  "progress defaults to 0, and start-distance is calcuated"
  [points ^pos pos ^pos dest]
  (->point-floating points 0 (distance pos dest) pos dest))
(defn points-floating-init "init to empty array" [] [])

(defn move-one-dim
  "move x or y closer to dest value.
  default step size of 10"
  ([cur dest]
   (move-one-dim cur dest 10))
  ([cur dest step]
   (let [Δ (- dest cur)
         sign (if (> 0  Δ) -1 1)
         minmax  (if (= sign 1) #'min #'max)]
     (minmax (+ cur (* sign step)) dest))))
(defn move-toward
  "move a point closer to dest. also default to stepsize of 10"
  ([^pos cur ^pos dest] (move-toward cur dest 10))
  ([^pos cur ^pos dest step]
   (->pos (move-one-dim (:x cur) (:x dest) step)
          (move-one-dim (:y cur) (:y dest) step))))

(defn point-keep [{:keys [^pos pos ^pos dest] :as ^point-floating p}]
  (not (= pos dest)))
(defn progress-calc
  "0 = no progress, 1 = there"
  [{:keys [^pos pos ^pos dest start-distance] :as ^point-floating p}]
  (- 1 (/ (distance pos dest) start-distance)))

(defn point-floating-step [^point-floating p step]
        (let [moved (assoc-in p [:pos] (move-toward (:pos p) (:dest p) step))
              newprog (progress-calc moved)]
          (assoc moved :progress newprog)))

(defn points-floating-update
        "move points toward goal. fade as they get closer. remove if close enough"
        [{:keys [points-floating] :as state} step]
        (assoc state :points-floating
               (->> (map #(point-floating-step % step) points-floating)
                    (filter #'point-keep))))
