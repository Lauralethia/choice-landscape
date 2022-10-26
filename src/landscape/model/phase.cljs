(ns landscape.model.phase
  (:require [landscape.model.avatar :as avatar]
            [landscape.settings :as settings]
            [landscape.key :as key]
            [landscape.http :as http]
            [landscape.utils :as utils]
            [landscape.sound :as sound]
            [landscape.model.wells :as wells]
            [landscape.model.floater :as floater]
            [landscape.model.records :as records]
            ;[debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
            ))

(defn is-time [ideal-time first-onset]
  (let [total-dur (- (utils/now) first-onset)
        time? (and ideal-time first-onset ( >= total-dur ideal-time))]
       (when time? (println "\tovertime @" total-dur " (ideal:" ideal-time ", now  "(utils/now)")"))
       time?))

(defn adjust-iti-time
  "for mr, we modeled variable iti w/mean RT of .58
  when rt is not that, modify iti-dur

  this competes with iti-ideal-end. only one is needed.
  iti-ideal-end will also compensate for slow display

  also see adjust-over-iti -- used just for printing (20220727)"
  [rt-prev iti-dur]
  (let [mr? ;(re-find #"^:mr" (str (get @settings/current-settings :timing-method)))
        (contains? #{:mri} (get-in @settings/current-settings [:where]))
        tmax (get-in @settings/current-settings [:times :choice-timeout])
        can-timeout? (:enforce-timeout @settings/current-settings)
        rt (or rt-prev tmax)
        texp settings/RT-EXPECTED]      ;; 580ms
    ;; no adjustment when not mr
    (if (and mr? can-timeout?)
      (do
        (let [adjust (- iti-dur (- rt texp))]
          (println "rt: " rt "orig dur:" iti-dur "will be" adjust)
          adjust))
      iti-dur)))

(defn time-of-response
  "get the timestamp of event after a button push"
  [{:keys [trial] :as state}]
  (let [trial0 (dec trial)
         trial-record (get-in state [:record :events trial0])]
     (or
      (get trial-record "waiting-time")
      (get trial-record "catch-time")
      (get trial-record "timeout-time"))))

(defn get-rt [{:keys [trial record] :as state}]
  "current trials waiting-time - chose-time.
nil if missing chose or any of the next events (waiting, catch)
nil if timout"
  ;; [:record :events trial0 time-key]
  (let [t0 (dec trial)                  ; zero based trial index
        ;first trial has no prev RT. fake as expected for rt-adjust calc
        trial (if (or (< t0 0) (= :instruction (get-in state [:phase :name]))) {"chose-time" 0 "waiting-time" settings/RT-EXPECTED}
                (get-in record [:events t0]))
        chose (get-in trial ["chose-time"])
        wait (get-in trial ["waiting-time"])
        catch (get-in trial ["catch-time"])
        ;; timeout (get-in trial ["timeout-time"])
        next (or wait catch)]
    (if (and next chose)
      (- next chose)
      nil)))

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
               (contains? #{:mri :eeg :practice} where) :done
               ; TODO: could also send to :survey (for buttonbox Qs)
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

        ;; iti has side but don't report it
        ;; confusing to report it. but might help identify trial if something goes wrong
        ;; side (if (=  (:name phase) :iti) 0 side)

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

(defn ttl-get-send [wells next-phase]
 (if-let [url (:local-ttl-server @settings/current-settings)]
   (let [ttl (gen-ttl wells next-phase)]
     (http/send-local-ttl url ttl)
     ttl)))
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
  [{:keys [trial wells phase time-cur] :as state}
   {:keys [start-at] :as next-phase}
   ;; NB. not pulling in name so we can use name function
   ]
  ;; 20220729 TODO: looks like maybe we are sending the prev state

  (let [time-key (str (name (:name next-phase)) "-time") ;chose-time waiting-time feedback-time
        trial0 (dec trial)
        state-time (assoc-in state [:record :events trial0 time-key]  start-at)
        ;; nil if not sending ttl (valued only for s/eeg)
        ttl (ttl-get-send (get-in state [:well-list trial0]) next-phase)
        state-time (if ttl
                     (assoc-in state-time
                               [:record :events trial0 :ttl (:name next-phase)]
                               {:code ttl :onset (utils/now)})
                     state-time)
        ;; NB. about to change to :feedback when :waiting, so use cur not next
        picked (get phase :picked)]
    (case (:name next-phase)

      ;; iti is start of trial and task (prev will be :instruction)
      :iti
      (-> state-time
          (assoc-in [:record :events trial0 :trial] trial)
          (assoc-in [:record :events trial0 :iti-orig]
                    (get-in state [:well-list trial0 :iti-dur], settings/ITIDUR))
          (assoc-in [:record :events trial0 :iti-ideal-end]
                    (get-in state [:well-list trial0 :iti-ideal-end]))
          )

      :chose
      (-> state-time
          (update-in [:record :events trial0] #(merge % (wells/wide-info wells)))
          ;; iti-dur is in phase after switch to iti
          (assoc-in [:record :events trial0 :iti-dur] (get phase :iti-dur))
          )

      ;; if no response and catch
      ;;    :avoided, :picked, and the rest of wide-info will be keys with
      ;;    but with nil values
      :catch
      (-> state-time
          (assoc-in [:record :events trial0 :picked] picked)
          ; undo keypress move
          (assoc-in [:record :events trial0 :rt] (get-rt state-time))
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
          (assoc-in [:record :events trial0 :rt] (get-rt state-time))
          ;; add picked and avoided
          (update-in [:record :events trial0]
                     #(merge % (wells/wide-info-picked wells picked))))
      :timeout
      (-> state-time
          ;; picked and avoided are null. make that explicit (unnecessary) 
          (assoc-in [:record :events trial0 :picked] nil)
          (assoc-in [:record :events trial0 :rt] nil)
          (update-in [:record :events trial0]
                     #(merge % (wells/wide-info-picked wells picked))))

      :feedback
      (-> state-time
          (assoc-in [:record :events trial0 :score] (get phase :scored))
          (assoc-in [:record :events trial0 :all-keys]
                    (-> state :key :all-pushes))
          (send-identity))

      ;; mr and eeg see :done. online goes to forum
      :done
      (-> state-time
          (assoc-in [:record :end-time] (records/make-start-time time-cur)))
      :forum
      (-> state-time
          (assoc-in [:record :end-time] (records/make-start-time time-cur)))

      ;;     survey would work w/ buttonbox (20220331)
      ;; 20220701 - survey is not used
      :survey                       ; finished survey about to be done
      (-> state-time
          (assoc-in [:record :end-time] (records/make-start-time time-cur))
          (println "TODO:  SHOULD PHONE HOME ABOUT DONE. also upload survey results"))
      ;; if no match, default to doing nothing
      state-time
      )))

;; TODO: well-list update also done by
;; wells/wells-update-which-open. not sure which is a better place
(defn update-next-trial-on-iti
  "iti is start of new trial: update to the next well info and trial.
  dont need to update to next trial if coming from instructions"
  [{:keys [trial well-list] :as state} next-name ]
  (let [ntrials (count well-list)
        trial (max 1 (inc trial)) ; if we are updating, its at iti and moving to next trial
        trial0 (dec (min trial ntrials))]
    (if (and (= next-name :iti)
             (not (= :instruction (get-in state [:phase :name]))))
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

(defn adjust-over-iti
  "can be 30ms over when flipping for iti.
  pretend that didn't happen by adjusting the time-cur used by phase swap.
  might cause other issues that look to see if time-start is time-cur.
  if we update choice start time after actual display, RT will be wrong!
  20220713 - this just prints. does not change any values"
  [{:keys [trial time-cur] :as state}]
  (let [trial0 (dec trial)
        iti-time (get-in state [:record :events trial0 "iti-time"])
        this-iti (- time-cur iti-time)
        iti-exp (or (get-in state [:well-list trial0 :iti-dur ]) settings/ITIDUR )
        iti-over (- this-iti iti-exp)
        adjust (if (>= trial0 0) iti-over 0)]
          
    (println "iti" trial0
             "\n iti_to_now(have)\t" this-iti
             "\n orig_wanted\t" iti-exp
             "\n would adjust\t" adjust)
    ;; (assoc  state :time-cur (- time-cur adjust))
    state
    ))

(defn mr-end-time-or-0 [state trial]
  (let [ntrials (count (:well-list state))
        mr-end (get-in state [:record :settings :iti+end])]
    (if (> trial ntrials) mr-end 0)))

(defn check-iti-time [state phase trial time-since]
  (let [is-mr (contains? #{:mri} (get-in @settings/current-settings [:where]))
        mr-extra-end (mr-end-time-or-0 state trial)
        ideal-end (get-in state [:well-list (dec trial) :iti-ideal-end])
        abs-start (get-in state [:record :start-time :browser])
        past-ideal (and is-mr (is-time ideal-end abs-start))]

    ;; 20221026 - reports 2-12ms difference
    ;; (when past-ideal (println "# iti past ideal"
    ;;                           ideal-end (- (utils/now) abs-start ideal-end)))

    (or past-ideal
      (>= time-since (+ (:iti-dur phase) mr-extra-end)))))
   
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
        iti-default (get-in @settings/current-settings [:times :iti-dur])
        ;; iti-dur for first trial0==-1
        ;; 20220726 - confirmed first trial is 0. state-fresh starts with 0
        ;; 20220727 - remove (get @settings/current-settings :iti-first)
        ;;            iti is start of next trial. use trial as next
        iti-dur (if (< trial0 0)
                  (do (println "WARNING: @ trail" trial) iti-default)
                  (get-in state [:well-list trial :iti-dur], iti-default))

        catch-dur (if (< trial0 0) 0  (get-in state [:well-list trial0 :catch-dur] 0))
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

                     ;; instruction update gets first iti
                     (= pname :instruction)
                     (assoc phase :name :iti :start-at time-cur :hit nil :scored false :picked nil
                            :iti-dur (get-in state [:well-list 0 :iti-dur],
                                             (get-in @settings/current-settings [:times :iti], settings/ITIDUR)))

                     ;; move onto iti or start the task: instruction -> iti
                     ;; might be comming from feedback, timeout, or instructions
                     (or (and (= pname :feedback) (avatar/avatar-home? state))
                         (and (= pname :catch)
                              (>= time-since catch-dur))
                         (and (= pname :timeout)
                              (>= time-since (get-in @settings/current-settings [:times :timeout-dur]))))

                     (do
                       (println "update from trial " trial " with phase: " phase)
                       (assoc phase
                              :name :iti
                              :start-at time-cur
                              ;; 20220527 - clear trial info. untested change
                              ;; but nothing should depend on these being held over
                              :hit nil
                              :scored false
                              :picked nil
                              :iti-dur (adjust-iti-time (get-rt state) iti-dur)))

                     ;; restart at chose when iti is over
                     (and (= pname :iti) (check-iti-time state phase trial time-since))
                     (phase-done-or-next-trial state)
                     ;; alternative use adjust-over-iti to print how far off we are
                     ;; (phase-done-or-next-trial (adjust-over-iti state))

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

