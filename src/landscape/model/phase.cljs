(ns landscape.model.phase
  (:require [landscape.model.avatar :as avatar]
            [landscape.settings :as settings]
            [landscape.key :as key]
            [landscape.http :as http]
            [landscape.model.wells :as wells]
            [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
            ))


;; 20211007 current phases
;; :instruction :chose :waiting :feedback :iti
(defn set-phase-fresh [name time-cur]
  {:name name :scored nil :hit nil :picked nil :sound-at nil :start-at time-cur})

;; TODO: actually use (remove nil)
(defn phase-done-or-next-trial [{:keys [trial time-cur well-list] :as state}]
  (if (>= trial (count well-list)) (set-phase-fresh :survey time-cur)
      (set-phase-fresh :chose time-cur)))

(defn send-identity
  "POST json of :record but return input state so we can use in pipeline"
  [{:keys [record] :as state}]
  (do (http/send-resp record)
      state))

;; ":record" has per trial vector of useful state info
;; [{:trial #
;;   :chose-time # :waiting-time # :feedback-time #
;;   :picked #  :score ?
;;   :$side-$info .... # wells/wide-info info for each well
;; }]
;; TODO: this doesn't need to return state. can just update :record
(defn phone-home
  "update :record for sending state
  send state to server right before feedback"
  [{:keys [trial wells phase] :as state}
   {:keys [name start-at] :as next-phase}]
  (let [time-key (keyword (str name "-time"))
        trial0 (dec trial)
        state-time (assoc-in state [:record trial0 time-key]  start-at)
        ;; NB. about to change to :feedback when :waiting, so use cur not next
        picked (get phase :picked)]
    (case name

      ;; iti is start of trial and task (prev will be :instruction)
      :iti
      (-> state-time
          (assoc-in [:record trial0 :trial] trial))

      :chose
      (-> state-time
          (update-in [:record trial0] #(merge % (wells/wide-info wells))))

      :waiting
      (-> state-time (assoc-in [:record trial0 :picked] picked)
          ;; add picked and avoided
          (update-in [:record trial0]
                     #(merge % (wells/wide-info-picked wells picked))))

      :feedback
      (-> (assoc-in state-time [:record trial0 :score] (get phase :scored))
          (send-identity))

      ;;  TODO!
      :survey                 ; finished survey about to be done
      (-> state-time (println "TODO:  SHOULD PHONE HOME ABOUT DONE. also upload survey results"))
      ;; if no match, default to doing nothing
      state-time
      )))

;; TODO: well-list update also done by 
;; wells/wells-update-which-open. not sure which is a better place
(defn update-next-trial-on-iti
  "when iti, update to the next well info and trial"
  [{:keys [trial well-list] :as state} next-name ]
  (let [ntrials (count well-list)
        trial (max 1 (inc trial))       ; if we are updating, its at iti and moving to next trial
        trial0 (dec (min trial ntrials))]
    (if (= next-name :iti)
      (-> state
          ;; (assoc state :wells (get (dec trial) well-list))
          (assoc :trial trial))
      state)))

(defn clear-key-before-chose
  "key pushes during other states linger into :chose
  clear all keys when we are leaving iti/entering chose"
  [{:keys [phase] :as state} next]
  (if (= next :chose) ;; (= (:name phase) :iti)
     (assoc state :key (key/key-state-fresh))
    state))

(defn phase-update
  "update :phase of STATE when phase critera meet (called by model/step-task).
  looks in phase for :picked & :hit, avatar location in state
  when updated calls phone-home to update :record (and maybe http/send)

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
                     (phase-done-or-next-trial state)

                     ;; no change if none needed
                     :else phase)
        ]
    ;; push current phase onto stack of events (historical record)
    ;; update phase
    (if (not= (:name phase) (:name phase-next))
      (-> state
          (update-next-trial-on-iti (:name phase-next))
          (clear-key-before-chose (:name phase-next))
          (phone-home phase-next)
          (assoc :phase phase-next))
      state)))

