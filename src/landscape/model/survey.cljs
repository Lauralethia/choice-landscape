(ns landscape.model.survey
  (:require [landscape.model.phase :as phase]
            [landscape.key :as key]))
(def SURVEYS
  [{:q "how many switches" :a ["1" "2-4" "5+"]}])

(defn phase-fresh [time] {:name :survey :start-at time :qi 0 :choice 0})

;;; todo work
(defn next-choice [{:keys [qi choice] :as phase}]
  (let [answers (get-in SURVEYS [qi :a]) 
        ntot  (count answers)
        next-i (mod (dec qi) ntot)]
    (nth answers next-i)))

(defn mv-idx-dir
  ^{:test (fn[]
            (assert (= 0 (mv-idx-dir 0 :left 2)))
            (assert (= 0 (mv-idx-dir 1 :left 2)))
            (assert (= 1 (mv-idx-dir 0 :right 2)))
            (assert (= 1 (mv-idx-dir 1 :up 2))))
    :doc "move given index by keypress direction"}
  [i dir max]
  (let [lastidx (dec max)]
    (case dir
        :left (max 0 (dec i))
        :right (min lastidx (inc i))
        ;; :up or :down, no change
        i)))

(defn read-keys [{:keys [key phase] :as state}]
  (let [dir (case (:have key)
              37 :left
              38 :up
              39 :right
              40 :down ;; maybe disallow
              nil)
        i-cur (:qi phase)
        i-next (if dir (mv-idx-dir i-cur dir (count SURVEYS)) i-cur)]
    (cond
      ;; if instruction has special plans for keypushes
      (not= i-cur i-next)
      (-> state
          (assoc-in [:phase :qi] i-next)
          (assoc-in [:key :have] nil)
          (mv-idx-dir i-cur i-next))

      ;; move choice
      ;; (dir in :up )* (mod (inc ci ) total)
      
      ;; we want to go past the end (are "ready")
      (and dir (= i-cur i-next) (= i-cur (dec (count SURVEYS))))
      (-> state
          ;; NB. maybe bug?
          ;; we move to "home" at start and dont want to intercept any
          ;; keys until we are there.
          ;; START TASK buy moving :phase :name to iti
          ;; TODO pull iti from somewhere?
          (assoc :phase (merge {:iti-dur 2000} (phase/set-phase-fresh :iti (:time-cur state))))
          ;; wells normally turned off on :chose->:waiting flip
          ;; here we skip right over that into the first :iti so explicitly close
          (assoc :key (key/key-state-fresh)))

      ;; otherwise no change
      :else
      state)))

(defn step [state time]
  (read-keys state))

