(ns landscape.model-test
(:require
 [landscape.model :refer [wells-fire-hits]]
 [landscape.model.wells :refer [activate-well hit-now]]
 [landscape.model.avatar :refer [move-closer]]
 [landscape.model.water :refer [water-pulse-water]]
 [clojure.test :refer [is]]))

;; moving right changes x
(is (= {:x 2} (move-closer {:x 1} {:x 3} 1 :right)))

;; update active-at when there is a collision
(is (= 10 (:active-at (activate-well
                    {:x 1 :y 1} 10
                    {:left {:pos {:x 1 :y 1}} :active-at 0}))))

;; used to id when we've just hit (cur time matches active-at time)
(is (= :right (first (hit-now {:left {:active-at 10} :right {:active-at 11}} 11))))


(is (= 0 (:water (wells-fire-hits {:water 0 :time-cur 10 :wells {:left {:active-at 10 :score false} :right {:active-at 2}}}))))
(is ( < 0 (:water (wells-fire-hits {:water 0 :time-cur 10 :wells {:left {:active-at 10 :score false} :right {:active-at 2}}}))))

(is (= 10 (:scale
           (water-pulse-water {:active-at 0 :level 10 :scale 10} 10))))

; water-pulse held when not active, increases when is, deactivates
(is (= 10 (:scale
           (water-pulse-water {:active-at 0 :level 10 :scale 10} 10))))
(is (< 10 (:scale
           (water-pulse-water {:active-at 10 :level 10 :scale 10} 11))))

(is (= 0 (:active-at
           (water-pulse-water {:active-at 10 :level 10 :scale 10} 999999))))
