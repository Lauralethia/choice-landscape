(ns landscape.phase-test
  (:require
   [landscape.model :as model :refer [add-key-to-state]]
   [landscape.model.phase :as phase]
   [landscape.model.wells :as wells]
   [landscape.model.records :as records]
   [landscape.model.timeline :refer [gen-wells]]
   [landscape.fixed_timing :as fixed]
   [landscape.utils :as utils]
   [landscape.settings :as settings]
   [landscape.instruction :refer [instruction-finished]]
   [landscape.loop :as loop]
   [clojure.test :refer [is deftest]]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
   )
  )
;; TODO: check phase-update between instruction and first trial
;; are we sending twice?
(def initial-state
  (let [
        state  (model/state-fresh)
        ;; :left on first trial is open and 100%
        wells (:practice fixed/trials)
        ;;                  trial wells default-iti
        ;;                  1000        2000
        time-trial (+ settings/WALKTIME settings/RT-EXPECTED)
        ;; ime-trial 1500
        wells (utils/iti-ideal-end time-trial wells settings/ITIDUR)
        state (assoc state :well-list (vec (wells/list-add-pos wells)))
        state (assoc state :phase {:name :instruction :idx 17})
        ;; state (assoc state :trial 0)
        ;; TODO: what is suppose to open the first well?!
        ;state (assoc state :wells (first wells))
        ;; state (assoc-in state [:wells :left :open] true)
        ]
    state))

(defn print-state [{:keys [time-cur trial phase key avatar wells well-list] :as state}]
    (println trial time-cur "@" (:name phase) "/" (:start-at phase)
             " = "  "x,y:" (:pos avatar) "->"  (:destination avatar) "\n"
              "\t" (select-keys phase [:score :hit :picked :iti-dur]) "\n"
             "\t" (select-keys key [:have :time]) "\n"
             "\t" (select-keys wells [:left]) "\n"
             "\t" "iti-end" (get-in well-list [(dec trial) :iti-ideal-end]) "\n"
             ;; "\t" (:left (nth well-list t0))
             ))

(def  global-time (atom 5000))
(defn in-global-time [step] (swap! global-time #(+ % step)))

(defn add-key-once
  "add a key push only if there aren't any on this trial"
  ;[{:keys [time-cur phase key] :as state} key]
  [state key]
  (let [cur-key (get-in state [:key :all-pushes 0 :key])
        phase   (get-in state [:phase :name])
        start? (> (:time-cur state) (get-in state [:phase :start-at]))
        ]

  ;(let [cur-key (get-in key [:all-pushes 0 :key])
  ;     phase   (get-in phase [:name])
  ;     past-start? ;; (> time-cur (get-in phase [:start-at]))
  ;     ]
    (if (and key start? (not cur-key) (= phase :chose ))
      (model/add-key-to-state state #js{:keyCode key})
      state)))

(defn step-state
    "collect transition times. could use phone home"
    [state & {:keys [step simkey ntrials maxtime] :or {simkey nil step 1000 ntrials 2 maxtime 90000}}]
    (with-redefs [global-time (atom step)
                 landscape.utils/now  #(in-global-time step)]

      ;; use redef utils/now to update :browser start-time
      ;; needed for absolute time.
      ;; done by instruction-finish but for inintial-state before 'now' is redefeind
      ;; (swap! state assoc-in [:record :start-time] (records/make-start-time (:time-cur state)))
      (swap! state instruction-finished 0)

      (while (and (>= ntrials  (:trial @state)) (< (:time-cur @state) maxtime))
        (let [time (:time-cur @state)
              state-key  (add-key-once @state simkey)]
          (print-state state-key)
          ;;  time-update->next-step->step-task->(read-keys, phase-update, etc)
          ;; (phase/phase-update (update @state :time-cur (partial #'+ step)))
          (reset! state
                  (loop/time-update (+ time step) state-key))))
      (:record @state)))

;; (loop [state initial-state] (let [t (:time-cur state)] (println (select-keys state [:wells :trial :phase :events :time-cur])) (if (< t 1000) (recur (landscape.loop/time-update (+ t 30) state)) state))))

(deftest iti-phase-test
  (let [state (atom initial-state)]
    ;; how we start (for documentation more than testing)
    (is (= 1    (-> @state :trial)))
    (is (= :iti (-> @state (instruction-finished 0) :phase :name)))
    (is (= 1000 (-> @state (instruction-finished 0) :phase :iti-dur)))
    (is (= 0    (-> @state (instruction-finished 0) :phase :start-at)))

    ;; ideal trial time (2000) + iti wait time (2000)
    (is (= 1000 (-> @state (instruction-finished 0) (get-in  [:well-list 0 :iti-ideal-end]))))
    ;; go through phases until we get to next trail (iti-time 1 is start of 2nd trial)
    ;; 1. timeout
    (is (= 4500) (get-in @state [:well-list 1 :iti-ideal-end]))
    (is (= 3500 (let [record (:events (step-state state :step 500))]
                  (get-in record [1 "iti-time"]))))
    ;; 2. pushed key. using 43 to off from expected 30ms betwen frames for tests
    (is (= 8505 (let [record (:events (step-state state :simkey 37 :step 43))]
                  (get-in record [1 "iti-time"]))))

    ;;  check out second trial
    ;; 1. timeout
    (is (= 5000 (get-in @state [:well-list 2 :iti-ideal-end])))
    (is (< 7000 (let [record (:events (step-state state :ntrials 2 :step 43))]
                  (get-in record [2 "iti-time"]))))
    ;; 2. pushed key
    (is (> 20000 (let [record (:events (step-state state :step 43 :ntrials 2 :simkey 37))]
             (get-in record [2 "iti-time"]))))

    ;; third
    ;; (is (= (get-in @state [:well-list 3 :iti-ideal-end])
    ;;        (let [record (:events (step-state state :simkey 37 :step 43 :ntrials 3))]
    ;;               (get-in record [3 "iti-time"]))))
    ;; (is (= (get-in @state [:well-list 3 :iti-ideal-end])
    ;;        (let [record (:events (step-state state :step 43 :ntrials 3))]
    ;;               (get-in record [3 "iti-time"]))))
    ))

(deftest phase-update-test
  (let [phase_iti (assoc (phase/set-phase-fresh :iti 10) :iti-dur 100)
        ;; 6 wells
        wells (gen-wells {:prob-low 100 :prob-high 100 :reps-each-side 1 :side-best :left})
        state_iti {:trial 1 :time-cur 10 :phase phase_iti :well-list wells}
        state_chose {:trial 1 :time-cur 10 :phase (assoc phase_iti :name :chose :picked :left)}
        state_nochose (-> state_chose
                          (assoc :time-cur 99999999)
                          (assoc-in [:phase :picked] nil))]
    ;; iti transitions
    (is (= :iti (-> (phase/phase-update state_iti) :phase :name)))
    (is (= :chose (-> state_iti (assoc :time-cur 200) phase/phase-update :phase :name)))
    (is (= :forum (-> state_iti (assoc :trial 7 :time-cur 200) phase/phase-update :phase :name)))
    ;; chose
    (is (= :chose (-> (assoc-in state_chose [:phase :picked] nil)
                      phase/phase-update :phase :name)))
    (is (= :waiting (-> state_chose   phase/phase-update :phase :name)))
    (is (= :timeout (-> state_nochose phase/phase-update :phase :name)))))

(deftest update-phase-catch
  (let [
        ;; vec/take for easy dbgn
        wells (->
               (vec (take 1 (gen-wells {:prob-low 100 :prob-high 100 :reps-each-side 1 :side-best :left})))
               (assoc-in [0 :catch-dur] 5))
        phase_chose (assoc (phase/set-phase-fresh :chose 10) :picked :left)
        state_chose {:trial 1 :time-cur 10 :phase phase_chose :well-list wells}
        state_nochose (-> state_chose
                          (assoc :time-cur 99999999)
                          (assoc-in [:phase :picked] nil))]

    ;; confirm catch-dur is where it should be (testing test setup)
    (is (= 5 (get-in state_chose [:well-list 0 :catch-dur] 0)))

    ;; should go to catch instead of waiting or timeout
    (is (= :catch (-> (phase/phase-update state_chose) :phase :name)))
    (is (= :catch (-> (phase/phase-update state_nochose) :phase :name)))

    ;; from catch to iti once enough time passes
    (is (= :catch (-> state_nochose
                      (phase/phase-update)
                      (assoc :time-cur 13)
                      (phase/phase-update)
                      :phase :name)))
    (is (= :iti (->  state_chose
                     (phase/phase-update)
                     (assoc :time-cur 17)
                     (phase/phase-update)
                     :phase :name)))

    ;; phone-home
    (is (nil? (-> (phase/phone-home state_nochose
                                    (-> state_nochose (phase/phase-update) :phase))
                      (get-in [:record :events 0])
                      :avoided)))
    (is (= :left (-> (phase/phone-home state_chose
                                    (-> state_chose (phase/phase-update) :phase))
                      (get-in [:record :events 0])
                      :picked)))))

(deftest next-phase-test
  "check end is advanced to approprate section"
  (let [state-onl {:phase {:name :none} :trial 3 :well-list [1 2]
                   :record {:settings {:where :online}}}
        state-mri (assoc-in state-onl [:record :settings :where] :mri)
        state-unf (assoc-in state-onl [:trial] 1)]
    ;; online goes to textbox form
    (is (= (-> state-onl phase/phase-done-or-next-trial :name) :forum))
    ;; survey if mri
    (is (= (-> state-mri phase/phase-done-or-next-trial :name) :done))
    ;; next trial
    (is (= (-> state-unf phase/phase-done-or-next-trial :name) :chose))))

(deftest ttl-examples-test
"ttl for events"
  (let [
        wells_rc {:left {:open true}  :up {:open true}  :right {:open false}}
        wells_uc {:left {:open true}  :up {:open false} :right {:open true}}
        wells_lc {:left {:open false} :up {:open true}  :right {:open true}}]
    (is (= 23 (phase/gen-ttl wells_rc {:name :chose :scored nil})))
    (is (= 63 (phase/gen-ttl wells_rc {:name :catch :picked :left :scored nil})))
    (is (= 223 (phase/gen-ttl wells_rc {:name :feedback :scored true :picked :left})))))

(deftest iti-dur-adjust-test
  "mr itertrial interval duration modified by reaction time"
  (with-redefs
    [landscape.settings/RT-EXPECTED 580 ; current value in 20220630
     landscape.settings/current-settings
      (atom (-> @landscape.settings/current-settings
                (assoc-in [:where] :mri)
                (assoc-in [:times :choice-timeout] 2000)
                (assoc-in [:enforce-timeout] true)))]
    (is (= 1000 (phase/adjust-iti-time 580 1000)))
    (is (= 999  (phase/adjust-iti-time 581 1000)))
    ;; worst case: no response on shortest iti
    (is (= 80 ;; (- landscape.settings/RT-EXPECTED=580 500)
           (phase/adjust-iti-time 2000 1500)))
    ;; no rt means timeout. use max
    (is (= 80 (phase/adjust-iti-time nil 1500)))))

(deftest iti-dur-adjust-no-mr-test
  "no iti change when not MR"
  (with-redefs
    [landscape.settings/current-settings
      (atom (-> @landscape.settings/current-settings
                (assoc-in [:where] :eeg)))]
    (is (= 1000 (phase/adjust-iti-time  580 1000)))
    (is (= 1000 (phase/adjust-iti-time  581 1000)))
    (is (= 1500 (phase/adjust-iti-time 2000 1500)))
    (is (= 1500 (phase/adjust-iti-time  nil 1500)))))

(deftest get-rt-test
  "more demonstration of where values are stored than actual test"
  (is (= settings/RT-EXPECTED (phase/get-rt {:trial 0 :record {:events [{"chose-time" 1000 "waiting-time" 1100}]} })))
  (is (= settings/RT-EXPECTED (phase/get-rt {:trial 1 :phase {:name :instruction} :record {:events [{"chose-time" 1000 "waiting-time" 1100}]} })))
  (is (= 100 (phase/get-rt {:trial 1 :record {:events [{"chose-time" 1000 "waiting-time" 1100}]} })))
  (is (= 100 (phase/get-rt {:trial 1 :record {:events [{"chose-time" 1000 "catch-time" 1100}]} })))
  (is (nil? (phase/get-rt {:trial 1 :record {:events [{"chose-time" 1000 "timeout-time" 1100}]} }))))


(deftest is-time-test
  (let [step 30
        now (utils/now)
        early (- now step)
        next (+ now step)]
    (is (phase/is-time step early))
    (is (not (phase/is-time step next)))
    (is (not (phase/is-time step nil)))
    (is (not (phase/is-time nil early)))))
