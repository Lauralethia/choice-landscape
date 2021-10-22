(ns landscape.model.water)


(defn water-state-fresh
  "state of water/lake display. KLUDGE: also records total score"
  [] {:level 10 :scale 10 :active-at 0 :score 0})

;; TODO: .75 success-rate is a guess.
;; should have some exponential or sigmoid decay if progress is close to 100?
(defn step-size-fixed
  "water step size from state
  TODO should exp. decay as level approaches 100 w.r.t. total trials"
  [{:keys [water well-list trial] :as state}]
  (let
      [success-rate .75
       total-trials (count well-list)
                                        ;trials-left  (- total-trials trial)
                                        ;level (:level water)
       max-success (* total-trials success-rate)
       step-size (/ max-success 90)]    ; 100% max but start at 10%
      step-size))
(defn step-size
  "water step size from state
  go quick at first, and then linear.
  but increase is greater when have lost a lot in a row"
  [{:keys [water well-list trial] :as state}]
  (let
      [
       total-trials (count well-list)
       trials-left  (- total-trials trial)
       level-cur (:scale water)
       ;; half full after ~15 trials. then slowly increassing from there
       level-want (* 100 (+ (/ trial (* 2 (+ 1 trial)))
                            (/ trial (* 2 total-trials))))
       step-size (- level-want level-cur)]
      step-size))

(defn water-inc
  "increase water level. should probably only happen when well is hit"
  [water time-cur inc-step]
  (-> water
      (update-in [:level] #(+ inc-step %))
      (update-in [:score] inc)
      (assoc-in [:active-at] time-cur)))

(defn water-pulse-water
  "if active-at is not zero. modulate water level with a sin wave.
  will set active-at to zero when pulsed long enough"
  [water time-cur]
  (let [sin-dur 500                   ; ms
        npulses 1                     ; n times to go up and back down
        dur-total (* npulses sin-dur)
        mag 2                         ; % scale increase
        time-at (:active-at water)
        dur-cur (- time-cur time-at)
        active-at (if (>= dur-cur dur-total) 0 (:active-at water))
        level (:level water)
        scale (if (not= active-at 0)
                (+ 10 level (js/Math.sin (* 2 js/Math.PI (/ dur-total dur-cur))))
                level)]
    (-> water
        (assoc-in [:scale] scale)
        (assoc-in [:active-at] active-at))))

(defn water-pulse [{:keys [water time-cur] :as state}]
  (update state :water #(water-pulse-water % time-cur)))


(defn water-pulse-water-forever
  "if active-at is not zero. modulate water level with a sin wave.
  copy of water-pulse-water but never sets active-at to zero
  ... must be stopped by something else (used in instructions)"
  [water time-cur]
  (let [sin-dur 1000                  ; ms
        npulses 1                     ; n times to go up and back down
        dur-total (* npulses sin-dur)
        mag 10                          ; % scale increase
        time-at (:active-at water)
        dur-cur (- time-cur time-at)
        active-at (:active-at water)
        level (:level water)
        scale (if (not= active-at 0)
                (+ 20 level (js/Math.sin (* 2 js/Math.PI (/ dur-total (mod dur-cur dur-total)))))
                level)]
    (-> water
        (assoc-in [:scale] scale))))

(defn water-pulse-forever [{:keys [water time-cur] :as state}]
  (update state :water #(water-pulse-water-forever % time-cur)))
