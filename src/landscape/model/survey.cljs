(ns landscape.model.survey
  (:require [landscape.model.phase :as phase]
            [landscape.key :as key]))
(defrecord survey [q answers])
(def SURVEYS [  
   (->survey "Which was best at first?"
             ["left" "up" "right"])
   (->survey "Which was best at the end?"
             ["left" "up" "right"])
   (->survey "If all wells were just as likely to give water, which would you pick?"
             ["left" "up" "right"]) 
   (->survey "Do you think wells changed how often they gave water?"
             ["Always the same" "1-2 switch" "3-4 switches" "5+ switches"])
   (->survey  "The farther well was difficult/annoying to go to:"
              ["not at all" "a little" "a lot"])
   (->survey "What would you guess the likelihood of water was for a well at its worst?"
             ["almost never <10%" "infrequent <30%"
              "near 50/50 (40-60%)" "good (>60%)"])
   ])

(defrecord survey-phase [name start-at qi choice-i])
(defn phase-fresh [time] (->survey-phase :survey time 0  0))

(defn next-choice [{:keys [qi choice-i] :as phase}]
  (let [answers (get-in SURVEYS [qi :answers]) 
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
  [i dir tot]
  (let [lastidx (dec tot)]
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
        nsurvey (count SURVEYS)
        i-cur (:qi phase)
        i-next (if dir (mv-idx-dir i-cur dir nsurvey) i-cur)]
    (cond
      ;; if instruction has special plans for keypushes
      (not= i-cur i-next)
      (-> state
          (assoc-in [:phase :qi] i-next)
          (assoc-in [:key :have] nil))

      ;; move choice
      ;; (dir in :up )* (mod (inc ci ) total)
      
      ;; we want to go past the end totally done with task
      (and dir (= i-cur i-next) (= i-cur (dec nsurvey)))
      (-> state
          (assoc :phase {:name :done})
          (assoc :key (key/key-state-fresh)))

      ;; otherwise no change
      :else
      state)))

(defn step [state time]
  (read-keys state))

