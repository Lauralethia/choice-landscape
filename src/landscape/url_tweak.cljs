(ns landscape.url-tweak
  (:require
   [cemerick.url :as url]))

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

(defn task-parameters-url
  "setup parameterization of task settings based on text in the url"
  ([settings] (task-parameters-url settings (get-url-map)))
  ([settings u]
   (-> settings 
       (update-settings u #"photodiode"  [:use-photodiode?] true)
       (update-settings u #"mx95"  [:prob :high] 95)
       (update-settings u #"nofar"  [:step-sizes 1] 0)
       (update-settings u #"VERBOSEDEBUG"  [:debug] true)
       (update-settings u #"NO_TIMEOUT"  [:enforce-timeout] false)
       ;; currently broken. need to get TIME?
       ;; (update-settings u #"noinstruction" [:show-instructions] false)

       (update-settings u #"nocaptcha"  [:skip-captcha] true)

       ;; always have one forced deval so 0 is actually 1
       (update-settings u #"fewtrials"  [:nTrials :pairsInBlock] 1)
       (update-settings u #"fewtrials"  [:nTrials :deval] 0))))
