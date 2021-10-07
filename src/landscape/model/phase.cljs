(ns landscape.model.phase
  (:require [landscape.model.avatar :as avatar]))

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

                     ;; restart at chose when avatar's back home
                     (and (= pname :feedback) (avatar/avatar-home? state))
                     (set-phase-fresh :chose time-cur)

                     ;; no change if none needed
                     :else phase)
        ]
    ;; TODO - push current phase onto stack of events (historical record)
    (assoc state :phase phase-next)))
