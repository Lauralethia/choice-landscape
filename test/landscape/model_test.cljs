(ns landscape.model-test
(:require
 [landscape.model :refer [wells-fire-hits]]
 [landscape.model.wells :refer [activate-well hit-now]]
 [landscape.model.avatar :refer [move-closer]]
 [landscape.model.points :as points]
 [landscape.model.water :refer [water-pulse-water]]
 [landscape.model.survey :as survey ]
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

(deftest point-move-one
  "move toward goal without overshooting"
  (is (= 20  (points/move-one-dim 10 100 10)))
  (is (= 100 (points/move-one-dim 95 100 10)))
  (is (= 10  (points/move-one-dim 20   0 10)))
  (is (= 0   (points/move-one-dim 2    0 10))))
(deftest point-move-twoard
  "move both x and y independently"
  (is (= (points/->pos 9 90)
         (points/move-toward {:x 0 :y 100} {:x 9 :y 0} 10))))
(deftest point-floating-create
  "set alpha and start-distance"
  (let [new (points/new-point-floating 1 (points/->pos 0 0) (points/->pos 10 10) )]
    (is (= (:progress new) 0))
    (is (> (:start-distance new) 10))))
(deftest point-floating-move
  "move point"
  (let [init (points/new-point-floating 1 (points/->pos 0 0) (points/->pos 10 10) )
        moved (points/point-floating-step init 10)]
    (is (> (:start-distance init) 10))
    (is (= .5 (points/progress-calc (assoc init :pos (points/->pos 5 5)))))
    (is (= (:progress moved) 1))
    (is (= (:pos moved) (:dest init)))))
(deftest test-points-floating-update
  "move many points that are floating"
  (let [state {:points-floating
              [(points/new-point-floating 1 (points/->pos 0 0) (points/->pos 10 10))
               (points/new-point-floating 1 (points/->pos 0 0) (points/->pos 100 100))]}
       next (points/points-floating-update state 50)
       ;; make sure we dont die when there's nothing to do
       next2 (-> next (points/points-floating-update 50)
                 (points/points-floating-update 50))]
   (is (= 2 (-> state :points-floating count)))
   (is (= 50 (-> next :points-floating last :pos :x)))
   (is (= 1 (-> next :points-floating count)))
   (is (= 0 (-> next2 :points-floating count)))))


(def alert-val (atom ""))
(deftest validate-form-test-bad
  "not validate"
  (with-redefs [js/alert (fn[m] (reset! alert-val m))]
    (survey/validate-form)
    (is (not (:done @survey/forum-atom)))
    (is (re-matches #"(?s).*Please.*age.*0.*" @alert-val))))
(deftest validate-form-test-bad-bu-have-age
  "not validate, but no age message"
  (with-redefs [js/alert (fn[m] (reset! alert-val m))
                survey/forum-atom (atom (survey/->forum-data "10" "" "" ""  false))          ]
    (survey/validate-form)
    (is (not (:done @survey/forum-atom)))
    (is (re-matches #"(?s).*Please.*" @alert-val))
    (is (not (re-matches #"(?s).*age.*0.*" @alert-val)))))
(deftest validate-form-test-good
  "does validate"
  (reset! alert-val "")
  (with-redefs
    [js/alert (fn[m] (reset! alert-val m))
     survey/forum-atom (atom (survey/->forum-data "10" "feedback" "2" "3"  false))]
    (survey/validate-form)
    (is (= "" @alert-val))
    (is (:done @survey/forum-atom))))
;; (deftest validate-form-test-good
;;   "validate"
;;   (let [fa (atom (survey/->forum-data 1 2 3 "feedback" false))]
;;     (do (validate-form fa)
;;         (is (not (:done @fa))))))
