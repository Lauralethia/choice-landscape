(ns landscape.url-tweak-test
  (:require
   [landscape.url-tweak :refer
    [vis-type-from-url task-parameters-url url-path-info path-info-to-id]]
   [landscape.settings :refer [current-settings]]
   [cemerick.url :as url]
   [clojure.test :refer [is deftest]])
)

(deftest vis-type 
  (is (= :mountain (vis-type-from-url {:anchor "mountain-anything" }) ))
  (is (= :desert (vis-type-from-url {:anchor "anything" }) ))
  (is (= :desert (vis-type-from-url {})))
  (is (= :desert (vis-type-from-url {:anchor nil})))
  ;; test page unlikely to have anchor and less likey to have  "mountain" in it
  (is (= :desert (vis-type-from-url (-> js/window .-location .-href url/url)))))

(deftest url-photodiode
  (is (:use-photodiode? (task-parameters-url {} {:anchor "mountain&photodiode"}))
  (is (not (:use-photodiode? (task-parameters-url @current-settings {:anchor "desert"})))))
  (is (not (:use-photodiode? (task-parameters-url @current-settings {})))))

(deftest url-fewtrials
  (is 10 (get-in (task-parameters-url {} {:anchor ""}) [:nTrials :devalue]))
  (is 1 (get-in (task-parameters-url {} {:anchor ""}) [:nTrials :pairsInBlock]))
  (is 0  (get-in (task-parameters-url {} {:anchor "fewtrials"}) [:nTrials :devalue])))

(deftest url-blocks-tweaks
  (is (= 12 (get-in (task-parameters-url {} {:anchor "devalue2=75"}) [:nTrials :devalue-good])))
  (is (= 80 (get-in (task-parameters-url {} {:anchor "devalue2=100_80"}) [:prob :devalue-good :good])))
  (is (= 75 (get-in (task-parameters-url {} {:anchor "devalue2=75"}) [:prob :devalue-good :good]))))

(deftest url-path-info-test
  (is (= (url-path-info (url/url "domain.path/id/task/timepoint/run/?junk"))
         {:run "run", :timepoint "timepoint",:task  "task" , :id "id" })))

(deftest url-to-id-test
  (let [url-map (url-path-info {:path "url/i/t/v/r"})]
    (is (= "i_t_v_r" (path-info-to-id url-map)))
    (is (= "unlabeled_run" (path-info-to-id nil)))))
