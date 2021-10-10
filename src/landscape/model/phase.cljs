(ns landscape.model.phase
  (:require [landscape.model.avatar :as avatar]
            [landscape.settings :as settings]
            [landscape.http :as http]
            [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
            ))


;; 20211007 current phases
;; :instruction :chose :waiting :feedback :iti
(defn set-phase-fresh [name time-cur]
  {:name name :scored nil :hit nil :picked nil :sound-at nil :start-at time-cur})

(defn send-identity
  "POST json of :record but return input state so we can use in pipeline"
  [{:keys [record] :as state}]
  (do (http/send-resp record)
      state))

;; ":record" has per trial vector of useful state info
;; [{:trial #
;;   :chose-time # :waiting-time # :feedback-time #
;;   :picked #  :score ?
;;   :wells {}
;; }]
;; but we probably want to express wells as wide instead of nested
;; and want to be able to easily get at picked and avoided
;; like
;;   :pick-prob # :picked-far? ?
;;   :avoid-prob # :avoid-far? ?
;;   :left-prob # :up-prob # :right-prob #
;;   :left-on ? :up-on ? :right-on ?
;;   :left-far ? :up-far ? :right-far ?
;; this could be fn as wells/wide-info
(defn on-phase-change
  "update :record for sending state
  send state to server right before feedback"
  [{:keys [trial wells phase] :as state}
   {:keys [name start-at] :as next-phase}]
  (let [time-key (keyword (str name "-time"))
        trial (max 1 (if (= name :chose) (inc trial) trial))
        trial0 (dec trial)
        state-time (assoc-in state [:record trial0 time-key]  start-at)]
    (case name

      :chose
      (-> state-time
          (assoc :trial trial)
          (assoc-in [:record trial0 :trial] trial)
          (assoc-in [:record trial0 :wells] wells))

      :waiting
      (assoc-in state-time [:record trial0 :picked] (get phase :picked))

      :feedback
      (-> (assoc-in state-time [:record trial0 :score] (get phase :scored))
          (send-identity))

      :iti
      state-time
      ;; if no match, default to doing nothing
      state-time)))

(defn phase-update
  "update :phase of STATE when phase critera meet (called by model/step-task).
  looks in phase for :picked & :hit, avatar location in state
  when updated calls on-phase-change to update :record (and maybe http/send)

  :chose -> :waiting when :picked not nil
  :waiting -> :feedback when :hit not nil
  :feedback -> :iti when avatar is home
  :iti -> :choose after duration
  "
  [{:keys [phase time-cur] :as state}]
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
                            ;; for MR we likey want predefined expodential distr.
                            :iti-dur settings/ITIDUR)
                     
                     ;; restart at chose when iti is over
                     (and (= pname :iti)
                          (>= (- time-cur (:start-at phase))
                              (:iti-dur phase)))
                     (set-phase-fresh :chose time-cur)

                     ;; no change if none needed
                     :else phase)
        ]
    ;; push current phase onto stack of events (historical record)
    ;; update phase
    (if (not= (:name phase) (:name phase-next))
      (-> state
          (on-phase-change phase-next)
          (assoc :phase phase-next))
      state)))
