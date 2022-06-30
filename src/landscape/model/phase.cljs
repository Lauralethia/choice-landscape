(ns landscape.model.phase
  (:require [landscape.model.avatar :as avatar]
            [landscape.settings :as settings]
            [landscape.key :as key]
            [landscape.http :as http]
            [landscape.sound :as sound]
            [landscape.model.wells :as wells]
            [landscape.model.floater :as floater]
            [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
            ))


;; 20211007 current phases
;; :instruction :chose :waiting :feedback :iti
(defn set-phase-fresh [pname time-cur]
  {:name pname :start-at time-cur
   :scored nil :hit nil :picked nil :sound-at nil :iti-dur nil})

(defn phase-done-or-next-trial
  "reset to next trial (chose) or to done (:done :chose :forum)"
  [{:keys [trial time-cur well-list] :as state}]
  (let [where (get-in state [:record :settings :where])
        next (cond
               (<= trial (count well-list)) :chose  ; next trail
               ;; :survey okay forced, but w/:iti->:survey, not responsive to keys
               (contains? #{:mri :eeg} where) :done     ; TODO: :survey (buttonbox Qs)
               (contains? #{:online} where) :forum ; freeform text w/full keyboard
               :else :forum)]
    (set-phase-fresh next time-cur)))

(defn send-identity
  "POST json of :record but return input state so we can use in pipeline"
  [{:keys [record] :as state}]
  (do (http/send-resp record)
      state))

(defn gen-ttl [wells phase]
  (let [side (+ (if (get-in wells [:left :open])  1 0)
                (if (get-in wells [:up :open])    2 0)
                (if (get-in wells [:right :open]) 3 0))
        ;; should only exist in feedback. but hangs on into iti?
        picked (case (:picked phase)
                 :left 10
                 :up 11
                 :right 13
                 0)
        ;;                 left up right
        ;; left+up=3       13   14
        ;; left+right=4    15       17
        ;; up+right=5           16  18

        name (case (:name phase)
               :iti 10
               :chose 20 ;; side + picked adds up to 18 for all below
               :catch 50
               :timeout 70
               :waiting 150
               :feedback 200
               ;; survey, or otherwise unknown
               230)
        score (if (get phase :scored) 10 0)
        ;; feedback+score+left: left+up=213; left+right=215; up+right=216
        ;; feedback-score: left+up=203; left+right=205; up+right=206
        ]
    (+ name side picked score)))

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
   {:keys [start-at] :as next-phase}
   ;; NB. not pulling in name so we can use name function
   ]
  (if-let [url (:local-ttl-server @settings/current-settings)]
    (http/send-local-ttl url (gen-ttl (:wells state) phase) ))
  (let [time-key (str (name (:name next-phase)) "-time") ;chose-time waiting-time feedback-time
        trial0 (dec trial)
        state-time (assoc-in state [:record :events trial0 time-key]  start-at)
        ;; NB. about to change to :feedback when :waiting, so use cur not next
        picked (get phase :picked)]
    (case (:name next-phase)

      ;; iti is start of trial and task (prev will be :instruction)
      :iti
      (-> state-time
          (assoc-in [:record :events trial0 :trial] trial))

      :chose
      (-> state-time
          (update-in [:record :events trial0] #(merge % (wells/wide-info wells))))

      ;; :timeout just resturn state-time

      ;; if no response and catch
      ;;    :avoided, :picked, and the rest of wide-info will be keys with
      ;;    but with nil values
      :catch
      (-> state-time
          (assoc-in [:record :events trial0 :picked] picked)
          ; undo keypress move
          ;; TODO: still moves avatar toward chosen direction. disable in model/readkeys?
          (assoc-in [:avatar :destination] (avatar/avatar-dest state :down))
          (assoc-in [:avatar :pos] (avatar/avatar-dest state :down))
          ;; add picked and avoided
          (update-in [:record :events trial0]
                     #(merge % (wells/wide-info-picked wells picked)))
          ;; add floating zzz's
          (update-in [:zzz]
                     #(floater/zzz-new (-> state :avatar :pos) 6))
          )

      :waiting
      (-> state-time
          (assoc-in [:record :events trial0 :picked] picked)
          ;; add picked and avoided
          (update-in [:record :events trial0]
                     #(merge % (wells/wide-info-picked wells picked))))

      :feedback
      (-> (assoc-in state-time [:record :events trial0 :score] (get phase :scored))
          (assoc-in [:record :events trial0 :all-keys]
                    (-> state :key :all-pushes))
          (send-identity))

      ;;  TODO!
      ;; NB. :done is it's own state. using :forum for text based questions
      ;;     survey would work w/ buttonbox (20220331)
      :survey                       ; finished survey about to be done
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

(defn adjust-iti-time
  "for mr, we modeled variable iti w/mean RT of .58
  when rt is not that, modify iti-dur"
  [rt iti-dur]
  (let [mr?  ;(re-find #"^:mr" (str (get @settings/current-settings :timing-method)))
             (contains? #{:mri} (get-in @settings/current-settings [:where]))
        tmax (get-in @settings/current-settings [:times :choice-dur])
        timeout? (:enforce-timeout @settings/current-settings)
        texp settings/RT-EXPECTED]
    ;; no adjustment when not mr
    (if (and mr? (> rt 0))
      (- iti-dur (- rt texp))
      iti-dur)))

(defn get-rt [{:keys [trial record] :as state}]
        "current trials waiting-time - chose-time.
nil if catch trial or other weirdness"
        ;; [:record :events trial0 time-key]
        (let [t0 (dec trial)            ; zero based trial index
              trial (get-in record [:events t0])
              wait (get-in trial ["waiting-time"] 0)
              chose (get-in trial ["chose-time"] 0)
              catch (get-in trial ["catch-time"] 0)]
          (if (and (<= catch 0) (> wait 0) (> chose 0))
            (- wait chose)
            nil)))

(defn phase-update
  "update :phase of STATE when phase critera meet (called by model/step-task
  and by instruction on last instruction).
  looks in phase for :picked & :hit, avatar location in state
  when updated calls phone-home to update :record (and maybe http/send)

  :chose -> :waiting when :picked not nil
  :waiting -> :feedback when :hit not nil
  :feedback -> :iti when avatar is home
  :iti -> :chose after duration
  "
  [{:keys [phase time-cur trial] :as state}]
  (let [pname (get phase :name)
        trial0 (dec trial)
        hit (get phase :hit)
        picked (get phase :picked)
        time-since (- time-cur (:start-at phase))
        iti-dur (get-in state [:well-list trial0 :iti-dur] settings/ITIDUR)
        catch-dur (get-in state [:well-list trial0 :catch-dur] 0)
        rt-max (get-in @settings/current-settings [:times :choice-timeout])
        phase-next (cond

                     ;; phase=catch (isi) if response or timeout w/catch-dur
                     ;; use catch-dur (def 0) from well-list as both duration (later)
                     ;; and as flag for if trial is catch
                     (and (= pname :chose)
                          (> catch-dur 0)
                          (or (some? picked)
                              (and (:enforce-timeout @settings/current-settings)
                                   (>= time-since rt-max))))
                     (assoc phase :name :catch :start-at time-cur :catch-dur catch-dur)

                     ;; as soon as we pick, switch to waiting
                     (and (= pname :chose) (some? picked))
                     (assoc phase :name :waiting :start-at time-cur)

                     ;; or a choice was not made quick enough
                     (and (= pname :chose)
                          (>= time-since rt-max)
                          (:enforce-timeout @settings/current-settings))
                     (assoc phase :name :timeout
                            :start-at time-cur
                            :sound-at (sound/timeout-snd time-cur nil))

                     ;; as soon as we hit, switch to feedback (sound)
                     (and (= pname :waiting) (some? hit))
                     (assoc phase :name :feedback :sound-at nil :start-at time-cur)

                     ;; move onto iti or start the task: instruction -> iti
                     ;; might be comming from feedback, timeout, or instructions
                     (or (and (= pname :feedback) (avatar/avatar-home? state))
                         (= pname :instruction)
                         (and (= pname :catch)
                              (>= time-since catch-dur))
                         (and (= pname :timeout)
                              (>= time-since (get-in @settings/current-settings [:times :timeout-dur]))))

                     (assoc phase
                            :name :iti
                            :start-at time-cur
                            ;; 20220527 - clear trial info. untested change
                            ;; but nothing should depend on these being held over
                            :hit nil
                            :scored false
                            :picked nil
                            :iti-dur (adjust-iti-time (get-rt state) iti-dur))

                     ;; restart at chose when iti is over
                     (and (= pname :iti)
                          (>= time-since (:iti-dur phase)))
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

