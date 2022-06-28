(ns landscape.phase-test
  (:require
   [landscape.model.phase :as phase]
   [landscape.model.timeline :refer [gen-wells]]
   [clojure.test :refer [is deftest]]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
   )
  )
;; TODO: check phase-update between instruction and first trial
;; are we sending twice?

(deftest update-phase
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
  (let [state-onl {:phase {:name :none} :trial 2 :well-list [1 2]
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
