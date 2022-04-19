(ns landscape.url-tweak
  (:require
   [cemerick.url :as url]
   [landscape.settings :as settings]
   [clojure.string]))

(defn get-url-map [] (-> js/window .-location .-href url/url))

(defn vis-type-from-url [u]
  "desert or mountain from url :anchor. default to desert"
  (let [anchor (get u :anchor "desert")]
    (if (re-find #"mountain" (or anchor "desert")) :mountain :desert)))

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

(defn task-parameters-url
  "setup parameterization of task settings based on text in the url"
  ([settings] (task-parameters-url settings (get-url-map)))
  ([settings u]
   (-> settings 
       (assoc :path-info (url-path-info u))
       (update-settings u #"photodiode"  [:use-photodiode?] true)
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

       ;; MRI settings
       (update-settings u #"where=mr" [:where] :mri)
       (update-settings u #"where=mr" [:keycodes] settings/mri-glove-keys)

       ;; always have one forced deval so 0 is actually 1
       (update-settings u #"fewtrials"  [:nTrials] {:pairsInBlock 1 :devalue 0 :devalue-good 1}))))
