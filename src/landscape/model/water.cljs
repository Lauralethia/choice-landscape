(ns landscape.model.water)


(defn water-state-fresh [] {:level 10 :scale 10 :active-at 0})

(defn water-inc
  "increase water level. should probably only happen when well is hit"
  [water time-cur inc-step]
  (-> water
      (update-in [:level] #(+ inc-step %))
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
