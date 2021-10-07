(ns landscape.model.phase
  (:require [landscape.model.avatar :as avatar]
            [landscape.settings :as settings]))

;; 20211007 current phases
;; :instruction :chose :waiting :feedback :iti
(defn set-phase-fresh [name time-cur]
  {:name name :scored nil :hit nil :picked nil :sound-at nil :start-at time-cur})

(defn phase-update [{:keys [phase time-cur] :as state}]
  (let [pname (get phase :name)
        hit (get phase :hit)
        picked (get phase :picked)
        phase-next (cond
                     ;; as soon as we pick, switch to waiting
                     (and (= pname :chose) (some? picked))
                     (assoc phase :name :waiting :start-at time-cur)

                     ;; as soon as we hit, switch to feedback (sound)
                     (and (= pname :waiting) (some? hit))
                     (assoc phase :name :feedback :sound-at nil :start-at time-cur)

                     ;; move onto iti
                     (and (= pname :feedback) (avatar/avatar-home? state))
                     (assoc phase
                            :name :iti
                            :start-at time-cur
                            ;; TODO: this should pull from somewhere else
                            :iti-dur settings/ITIDUR)
                     
                     ;; restart at chose when iti is over
                     (and (= pname :iti)
                          (>= (- time-cur (:start-at phase))
                              (:iti-dur phase)))
                     (set-phase-fresh :chose time-cur)

                     ;; no change if none needed
                     :else phase)
        ]
    ;; TODO - push current phase onto stack of events (historical record)
    (assoc state :phase phase-next)))
