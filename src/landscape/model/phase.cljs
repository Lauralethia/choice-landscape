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
  (and ideal-time first-onset ( >= (- (utils/now) first-onset) ideal-time)))

(defn adjust-iti-time
  "for mr, we modeled variable iti w/mean RT of .58
  when rt is not that, modify iti-dur
TODO: subtract settings/WALKTIME vs timeout time if did timeout"
  [rt iti-dur]
  (let [mr?  ;(re-find #"^:mr" (str (get @settings/current-settings :timing-method)))
             (contains? #{:mri} (get-in @settings/current-settings [:where]))
        tmax (get-in @settings/current-settings [:times :choice-timeout])
        can-timeout? (:enforce-timeout @settings/current-settings)
        rt (or rt tmax)
        texp settings/RT-EXPECTED]
    ;; no adjustment when not mr
    (println "rt: " rt " orig dur:" iti-dur "will be" (- iti-dur (- rt texp)))
    (if (and mr? can-timeout?)
      (- iti-dur (- rt texp))
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
        trial (get-in record [:events t0])
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
  [{:keys [trial wells phase time-cur] :as state}
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
          (update-in [:record :events trial0] #(merge % (wells/wide-info wells)))
          ;; dont see iti when it happens. need to go backwards
          ;; first iti is lost (not part of a trial. avail in settings as :iti-first)
          (assoc-in [:record :events (max (dec trial0) 0) :iti-dur] (get phase :iti-dur)))

      ;; :timeout just resturn state-time

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
    ;; (let [rt-at (time-of-response state)
    ;;                            iti (get-in state [:record :events trial0 "iti-time"])
    ;;                            this-iti (- time-cur fbk) 
    ;;                            exp (or (get-in state [:well-list trial0 :iti-dur ]) settings/ITIDUR )]
    ;;                        (println "iti" trial0
    ;;                                 "\n _to_now(have)\t" this-iti
    ;;                                 "\n recorded_as\t" exp
    ;;                                 "\n orig_wanted\t" iti-dur
    ;;                                 "\n diff_have-rec\t" (- this-iti exp)))
          
    (println "iti" trial0
             "\n iti_to_now(have)\t" this-iti
             "\n orig_wanted\t" iti-exp
             "\n adjust\t" adjust)
    ;; (assoc  state :time-cur (- time-cur adjust))
    state
    ))
   

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
        ;; iti-dur for first trial0==-1
        ;; 20220726 - confirmed first trial is 0. state-fresh starts with 0
        ;; 20220727 - remove (get @settings/current-settings :iti-first)
        iti-dur (if (< trial0 0)
                  (do (println "WARNING: @ trail" trial) settings/ITIDUR)
                  (get-in state [:well-list trial0 :iti-dur], settings/ITIDUR))

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
                     (or 
                      (is-time (get-in state [:well-list trial0 :iti-ideal-end])
                               (get-in state [:record :start-time :browser]))
                      ;; TODO: do we want to skip dur check below if we have iti-ideal-end time?
                      ;;       dur check might be true before iti ideal
                      (and (= pname :iti)
                           (>= time-since (+ (:iti-dur phase)
                                            ;; TODO: if timeout and MR, append walktime
                                            ;; if last iti, add a iti+end
                                            (if (> trial (count (:well-list state)))
                                              (get-in state [:record :settings :iti+end])
                                              0)))))
                     (phase-done-or-next-trial (adjust-over-iti state))

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

