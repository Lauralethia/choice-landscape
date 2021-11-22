(ns landscape.model.survey
  (:require
   [landscape.model.phase :as phase]
   [sablono.core :as sab :include-macros true :refer-macros [html]]
   [landscape.key :as key]))
(defrecord survey [q answers])
(defrecord survey [q answers shortname])
(def SURVEYS
  [
   (->survey (html [:div
                    "One last thing!" [:br]
                    [:span {:style {:font-size "smaller" :font-weight "normal"}}
                     "Please answer these questions." [:br]
                     "Use the up arrow to change your answer" [:br]
                     " and the right arrow to go to the next question"]])
             ["If I must" "OK!"]
             "start")
   (->survey "Which was best at first?"
             ["left" "up" "right"]
             "best_1st")
   (->survey "Which was best at the end?"
             ["left" "up" "right"]
             "best_end")
   (->survey "If all wells were just as likely to give water, which would you pick?"
             ["left" "up" "right"]
             "overall_side_pref")
   (->survey "Do you think wells changed how often they gave water?"
             ["Always the same" "1-2 switch" "3-4 switches" "5+ switches"]
             "num_changes")
   (->survey  "The farther well was difficult/annoying to go to:"
              ["not at all" "a little" "a lot"]
              "far_annoying")
   (->survey "What would you guess the likelihood of water was for a well at its worst?"
             ["almost never <10%" "infrequent <30%"
              "near 50/50 (40-60%)" "good (>60%)"]
             "worst_prob")
   ])

(defrecord survey-phase [name start-at qi choice-i])
(defn phase-fresh [time] (->survey-phase :survey time 0  0))
(defn phone-home-init [surveys]
  (mapv #(hash-map :ci 0 :answer "" :q (:q %) ) surveys))

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
        i-cur (or  (:qi phase) 0)
        i-next (if dir (mv-idx-dir i-cur dir nsurvey) i-cur)
        this-q (get SURVEYS i-cur)]
    (cond
      ;; if instruction has special plans for keypushes
      (not= i-cur i-next)
      (-> state
          (assoc-in [:phase :qi] i-next)
          (assoc-in [:key :have] nil))

      ;; move choice
      (contains? #{:up :down} dir)
      (let [n-ans (count (:answers this-q))
            mvfnc (if (= dir :down) inc dec)]
        (-> state
            (update-in [:phase :ci] #(mod (mvfnc (or % 0))  n-ans))
            (assoc :key (key/key-state-fresh))))
      ;; TODO phone-home
      
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

