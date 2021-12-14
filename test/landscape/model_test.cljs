(ns landscape.model-test
(:require
 [landscape.model :refer [wells-fire-hits]]
 [landscape.model.wells :refer [activate-well hit-now]]
 [landscape.model.avatar :refer [move-closer]]
 [landscape.model.water :refer [water-pulse-water]]
 [clojure.test :refer [is deftest]]))

;; moving right changes x
(deftest move
  (is (= {:x 2} (move-closer {:x 1} {:x 3} 1 :right))))

(deftest hitwell
  ;; update active-at when there is a collision
  (is (= 10 (:active-at (activate-well
                         {:x 1 :y 1} 10
                         {:left {:pos {:x 1 :y 1}} :active-at 0}))))

  ;; used to id when we've just hit (cur time matches active-at time)
  (is (= :right (first (hit-now {:left {:active-at 10} :right {:active-at 11}} 11)))))


(deftest water-pulses
  (let
      [wells_now {:water 0 :time-cur 10 :wells {:left {:active-at 10 :score false} :right {:active-at 2}}}
       well_hit (wells-fire-hits wells_now)]
      (is (= 0 (:water well_hit))) ;; did not score - no change in water (NB. what would score look like!?)
      (is (= :left (get-in well_hit [:phase :hit]))) ;; update phase with hit side
      ;; TODO: check destination is center-x bottom-y from settings/BOARD
      )
  ; water-pulse held when not active, increases when is, deactivates
  (is (= 10 (:scale
             (water-pulse-water {:active-at 0 :level 10 :scale 10} 10))))
  (is (< 10 (:scale
             (water-pulse-water {:active-at 10 :level 10 :scale 10} 11))))
  (is (= 0 (:active-at
            (water-pulse-water {:active-at 10 :level 10 :scale 10} 999999)))))
