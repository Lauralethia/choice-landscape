(ns landscape.url-tweak
  (:require
   [cemerick.url :as url]
   [landscape.settings :as settings]
   [clojure.string]))

(defn get-url-map
  ([] (-> js/window .-location .-href get-url-map))
  ([url]  (url/url url)))

(defn vis-type-from-url [u]
  "desert or mountain from url :anchor. default to desert"
  (let [anchor (or (get u :anchor) "desert")]
    (cond
      (re-find #"desert" anchor) :desert
      (re-find #"mountain" anchor) :mountain
      (re-find #"wellcoin" anchor) :wellcoin
      (re-find #"ocean" anchor) :ocean
      :else    :desert)))

(defn pattern-in-url
  "do either url map's path or anchor contain a pattern.
  useful for checking parameter permutations set by server (path) or static (anchor)"
  ([patt] (pattern-in-url (get-url-map) patt))
  ([umap patt] (some some? (map #(re-find patt (str %))
                                (-> umap (select-keys [:path :anchor]) vals)))))

(defn update-settings [settings u pattern where value]
  (if (pattern-in-url u pattern) (assoc-in settings where value) settings))

(defn url-path-info
  "could be surving nested on another server?
  work path backwards to get id/task/timepoint/run/"
  [u]
  (-> u :path (clojure.string/split #"/") reverse (#(zipmap [:run :timepoint :task :id] %))))

(defn path-info-to-id
  "create a unique id using url-path-info output"
  [{:keys [id task timepoint run] :as url-map}]
  (if id
    (str "sub-" id "_task-" task "_ses-" timepoint "_run-" run)
    "unlabeled_run"))

(defn update-walktime
  "url_tweaks might update step-size and step-sizes.
  should adjust expect walk time to match"
  [s]
  (let [first-well-step (get-in s [:step-sizes 0], 70)
        avatar-step-size (get-in s [:avatar-step-size], 10)
        ;; MR walktime is whatever is hardcoded
        mr? (contains? #{:mri} (get-in s [:where]))
        new-walktime (if mr? settings/WALKTIME
                         (* 2 settings/SAMPLERATE (/ first-well-step avatar-step-size)))]
    ;; (println "# setting walktime to" new-walktime "hardcoded is" settings/WALKTIME "mr?" mr?)
    (-> s
        (assoc-in [:times :timeout-dur] new-walktime)
        (assoc-in [:times :walk-dur] new-walktime))))

(defn task-parameters-url
  "setup parameterization of task settings based on text in the url"
  ([settings] (task-parameters-url settings (get-url-map)))
  ([settings u]
   (-> settings 
       (assoc :path-info (url-path-info u))
       (update-settings u #"mx95"  [:prob :high] 95)
       (update-settings u #"nofar"  [:step-sizes 1] 0)
       (update-settings u #"yesfar"  [:step-sizes 1] 150)
       (update-settings u #"VERBOSEDEBUG"  [:debug] true)
       (update-settings u #"NO_TIMEOUT"  [:enforce-timeout] false)
       ;; currently broken. need to get TIME?
       (update-settings u #"noinstruction" [:show-instructions] false)

       (update-settings u #"nocaptcha"  [:skip-captcha] true)

       ;; where to submit finishing
       ;; 20220419 - this is unused!? handled by opener (within iframe)
       ;; TODO: remove
       (update-settings u #"mturk=sand"  [:post-back-url] "https://workersandbox.mturk.com/mturk/externalSubmit")
       (update-settings u #"mturk=live"  [:post-back-url] "https://mturk.com/mturk/externalSubmit")
       
       ;; 20220314 - blocks 3 or 4.
       ;; 4th block (devalue-good) has different probabilities for good well
       (update-settings u #"devalue1" [:nTrials] {:pairsInBlock 24 :devalue 10 :devalue-good 0})
       (update-settings u #"devalue2=.*"  [:nTrials] {:pairsInBlock 12 :devalue 12 :devalue-good 12})
       (update-settings u #"devalue2=75"     [:prob :devalue-good] {:good 75 :other  75})
       (update-settings u #"devalue2=100_80" [:prob :devalue-good] {:good 80 :other 100})

       (update-settings u #"norev" [:reversal-block] false)

       ;;; LOCATION settings
       (update-settings u #"where=practice" [:where] :practice)
       ;; MRI
       (update-settings u #"where=mr" [:where] :mri)
       (update-settings u #"where=mr" [:keycodes] settings/mri-glove-keys)
       (update-settings u #"where=mr" [:skip-captcha] true)
       ; 20220727 - first iti is on first trial. should be set by MR timing
       ;(update-settings u #"where=mr" [:iti-first] settings/MR-FIRST-ITI)
       (update-settings u #"where=mr" [:iti+end] settings/MR-END-ITI)
       ; unlike to be need. iti is specfied for all trials in well-list for mr when using timing=
       ; here for testing with randomly generated timings
       (update-settings u #"where=mr" [:times :iti-dur] 2000)
       ;; EEG
       (update-settings u #"where=eeg" [:where] :eeg)
       (update-settings u #"where=eeg" [:skip-captcha] true)
       ;; sEEG
       (update-settings u #"where=seeg" [:where] :seeg)
       (update-settings u #"where=seeg" [:skip-captcha] true)
       (update-settings u #"where=seeg" [:use-photodiode?] true)
       (update-settings u #"where=seeg" [:pd-type] :phasecolor) ; vs :whiteflash

       ;; EEG,sEEG: sending ttl (but could use for anywhere)
       (update-settings u #"ttl=local" [:local-ttl-server] "http://127.0.0.1:8888")
       (update-settings u #"ttl=none"  [:local-ttl-server] nil)

       ;; photodiode
       (update-settings u #"photodiode"       [:use-photodiode?] true)
       (update-settings u #"photodiode"       [:pd-type] :whiteflash) ; vs :phasecolor
       (update-settings u #"photodiode=none"  [:use-photodiode?] false)
       (update-settings u #"photodiode=phase" [:pd-type] :phasecolor)
       (update-settings u #"photodiode=flash" [:pd-type] :whiteflash)

       ;; EEG,sEEG: sending ttl (but could use for anywhere)
       ;; fixed timing
       (update-settings u #"timing=randomA" [:left-best] false) ; A=right
       (update-settings u #"timing=randomB" [:left-best] true)  ; B=left
       (update-settings u #"timing=debug" [:timing-method] :debug)
       (update-settings u #"timing=practice" [:timing-method] :practice)
       (update-settings u #"timing=mra10min" [:timing-method] :mrA) ; right best
       (update-settings u #"timing=mrb10min" [:timing-method] :mrB) ; left best
       (update-settings u #"timing=mra1-short" [:timing-method] :mrA1) ; right best
       (update-settings u #"timing=mra2-short" [:timing-method] :mrA2)
       (update-settings u #"timing=mrb1-short" [:timing-method] :mrB1) ; left best
       (update-settings u #"timing=mrb2-short" [:timing-method] :mrB2)
       (update-settings u #"timing=quickrandom" [:nTrials] {:pairsInBlock 2 :devalue 2 :devalue-good 0})


       ;; always have one forced deval so 0 is actually 1
       (update-settings u #"fewtrials"  [:nTrials] {:pairsInBlock 1 :devalue 0 :devalue-good 1})
       ;;; landscapes wellcoin and ocean are spread out
       ;; wellcoin should match ocean in postions
       (update-settings u #"landscape=wellcoin"  [:bottom-y] 300)
       (update-settings u #"landscape=wellcoin"  [:avatar-home :y] 300)
       (update-settings u #"landscape=wellcoin"  [:bar-pos :y] 400)
       (update-settings u #"landscape=wellcoin"  [:step-sizes] [140 0]) ;orig 70
       (update-settings u #"landscape=wellcoin"  [:avatar-step-size] 15)
       (update-settings u #"landscape=wellcoin"  [:pile-top] 130)
       ;; ocean doesn't have water/gold pile
       (update-settings u #"landscape=ocean"  [:bottom-y] 300)
       (update-settings u #"landscape=ocean"  [:avatar-home :y] 300)
       (update-settings u #"landscape=ocean"  [:bar-pos :y] 400)
       (update-settings u #"landscape=ocean"  [:step-sizes] [140 0]) ;orig 70
       (update-settings u #"landscape=ocean"  [:avatar-step-size] 15) 
       (update-settings u #"landscape=ocean"  [:pile] {:x 200 :y 150})
       ;; step precedence over landscape
       (update-settings u #"step=slow"  [:avatar-step-size] 1) 
       ;; update iti and walk dur based on new avatar and well step-sizes
       (update-walktime)
)))
