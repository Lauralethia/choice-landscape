(ns landscape.model.survey
  (:require
   [landscape.model.phase :as phase]
   [landscape.key :as key]
   [sablono.core :as sab :include-macros true :refer-macros [html]]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]))
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

;;;;;;;;
;; forum for when keyboard is avaliable
; disable read-keys so we get the keycodes we want
;; !debugging note! to see section, run
;;   (swap! landscape.model/STATE assoc-in [:phase :name] :forum)
;;   (get-in @landscape.model/STATE [:record :survey])
(defrecord forum-data [age feedback fun device done])
;; UGLY -- use forum-atom as global!
(def forum-atom (atom (->forum-data nil nil nil nil nil)))
(defn add-forum-watcher [main-atom forum-atom]
  (add-watch :forum-watcher forum-atom
             (fn [_key _atom old new]
               (swap! main-atom assoc-in [:record :survey] new))))
(defn evtval [evt] (.. evt -target -value))
(defn update-forum-atom [key evt] (swap! forum-atom assoc key (evtval evt)))
(defn update-forum-atom-select [key evt] (swap! forum-atom assoc key (.. evt -target -value)))
(defn view-questions []
  (html [:div  {:id "instruction"}
         [:h3 "Almost Done!"]
         [:br] "Tell us about yourself and how you feel about the game!"
         [:br] [:br]
         [:div {:style {:text-align :left}}
          [:forum
           [:label {:for "age"} "Your age in years:"]
           [:br] [:input {:name "age" :type "text" :size 2
                          :value (:age @forum-atom)
                          :on-change #(update-forum-atom :age %)}]

           [:br] [:br][:label {:for "fun"}
                       "How fun was this (from 1 to 5: not fun to very fun)"]
           [:br][:input {:name "fun" :size 1 :type "text"
                         :on-change #(update-forum-atom :fun %)
                         :value (:fun @forum-atom)}]

           [:br] [:br][:label {:for "understand"}
                       "How well do you think you understood the instructions (1=\"not at all\" to 5=\"entirely\")"]
           [:br][:input {:name "understand" :size 1 :type "text"
                         :on-change #(update-forum-atom :understand %)
                         :value (:understand @forum-atom)}]

           ; 20220315 - no one is doing this on a phone or tablet
           ;[:br] [:br][:label {:for "device"}
           ;            "What kind of device are you using"]
           ;[:br][:select {:name "device" :id "device"
           ;               :on-change #(update-forum-atom-select :device %)
           ;               :value (:device @forum-atom)}
           ;      [:option {:value "computer"} "computer"]
           ;      [:option {:value "phone"} "phone"]
           ;      [:option {:value "tablet"} "tablet"]
           ;      [:option {:value "other"} "other"]]

           [:br] [:br][:label {:for "feedback"}
                       "Any feedback will be very helpful!"
                       [:br]"What were some things you didn't like? "
                       "Things you did like? Anything that was confusing? "
                       "Did you have a strategy?"]
           [:br][:textarea {:style {:width "80%"}
                            :name "feedback"
                            :on-change #(update-forum-atom :feedback %)
                            :value (:feedback @forum-atom)}]
           [:br] [:br] [:input {
                                :value "Finished!"
                                :type :submit
                                :on-click #(swap! forum-atom assoc :done true)}]]]]))
(defn step-forum [state time]
  (let [done-at (if (:done @forum-atom) time nil)]
    (-> state
        (assoc-in [:record :survey] @forum-atom)
        (assoc-in [:phase :name] (if done-at :done :forum)))))

