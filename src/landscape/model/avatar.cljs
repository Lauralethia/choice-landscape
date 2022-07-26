(ns landscape.model.avatar
  (:require [landscape.model.wells :as wells]
            [landscape.utils :as utils]
            [landscape.sprite :as sprite]
            ;; [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
            [landscape.settings :refer [current-settings]]))

;;avatar
(defn which-dir [cur dest]
  (cond
    (= cur dest) :none
    (> (:x cur) (:x dest)) :left
    (< (:x cur) (:x dest)) :right
    (< (:y cur) (:y dest)) :down
    (> (:y cur) (:y dest)) :up
    :else :none))

(defn move-closer [cur dest step dir]
  (case dir
    :none  cur
    :left  (merge cur {:x (max (- (:x cur) step) (:x dest))})
    :right (merge cur {:x (min (+ (:x cur) step) (:x dest))})
    :up    (merge cur {:y (max (- (:y cur)
                                  (* step (:top-scale @current-settings))) (:y dest))})
    :down  (merge cur {:y (min (+ (:y cur)
                                  (* step (:top-scale @current-settings))) (:y dest))})))

(defn move-active-time [a b now time]
        (cond
            (= a b) 0                     ;; stop animating
            (and (= time 0) (not= a b)) now ;; start now
            (and (not= time 0) (not= a b)) time ;; maintain prev starting time
            true 0))

(defn move-avatar [{:keys [avatar time-cur] :as state}]
  (let [step-size (get @current-settings :avatar-step-size 10)
        ;; time-step 30 ;; settings/SAMPLERATE
        ;; time-delta (- time-cur (get avatar :last-move (- time-cur SAMPLERATE)))
        cur (:pos avatar)
        dest (:destination avatar)
        dir (which-dir cur dest)
        moved (move-closer cur dest step-size dir)]
      (-> state
        (assoc-in [:avatar :pos] moved)
        (assoc-in [:avatar :last-move] time-cur)
        (update-in [:avatar :direction] #(if (= :none dir) % dir))
        (update-in [:avatar :active-at]
                  (partial move-active-time dest moved time-cur)))))

(defn top-center-pos
  "use state to get top of avatar. created for making putting Zs for isi/catch"
  [{:keys [sprite-picked avatar] :as state}]
  (let [pos (:pos avatar)
              h (get-in landscape.sprite/avatars [sprite-picked :height] 0)
              w (get-in landscape.sprite/avatars [sprite-picked :width] 0)]
          (-> pos
              (update :y #(- % h))
              (update :x #(+ % (/ w 2))))))

(defn avatar-dest
  "set new avatar destitionation. used by readkeys on keypush"
  [{:keys [wells avatar] :as state} dir]
        (let[cx (-> avatar :pos :x)
             cy (-> avatar :pos :y)]
          (case dir
            :left  (-> wells :left :pos) ;{:x 0   :y cy}
            :right (-> wells :right :pos) ;{:x 400 :y cy}
            :up    (-> wells :up :pos) ;{:x cx :y 100}
            :down (:avatar-home @current-settings)
            (-> avatar :pos))))

(defn avatar-home? [state]
  (utils/collide? (get-in state [:avatar :pos]) (:avatar-home @current-settings)))
