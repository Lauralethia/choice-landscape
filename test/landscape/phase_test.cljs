(ns landscape.phase-test
(:require
 [landscape.settings :refer [BOARD]]
 [landscape.model.phase :as phase]
 [landscape.model.timeline :refer [gen-wells]]
 [clojure.test :refer [is deftest]]))
;; TODO: check phase-update between instruction and first trial
;; are we sending twice?

(deftest update-phase
  (let [phase_iti (assoc (phase/set-phase-fresh :iti 10) :iti-dur 100)
        ;; 6 wells
        wells (gen-wells {:prob-low 100 :prob-high 100 :reps-each-side 1 :side-best :left})
        state_iti {:trial 0 :time-cur 10 :phase phase_iti :well-list wells}
        state_chose {:trial 0 :time-cur 10 :phase (assoc phase_iti :name :chose :picked :left)}
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
