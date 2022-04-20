(ns landscape.instruction-test
(:require
 [landscape.instruction :as instruction]
 [landscape.settings :as settings]
 [clojure.test :refer [is deftest]]))

;; moving right changes x
(deftest well-mine
  (swap! settings/current-settings  assoc :vis-type "mountain")
  (is (= (instruction/item-name :well) "mine")))

(deftest well-well
  (swap! settings/current-settings  assoc :vis-type "desert")
  (is (= (instruction/item-name :well) "well")))

(deftest mri-block-test
  (let [state {:phase {:name :instruction :idx 2}
               :key {:have 39}}
        last-instruction (dec(count instruction/INSTRUCTION))
        txt-fnc (get-in instruction/INSTRUCTION [last-instruction :text])
        state-mri (-> state
                      (assoc-in [:key :have] 52)
                      (assoc-in [:record :settings :where] :mri)
                      (assoc-in [:keycodes] settings/mri-glove-keys))]
    ;; move forward
    (is (= (-> state instruction/read-keys :phase :idx) 3))
    ;; no move
    (is (= (-> state
               (assoc-in [:key :have] 38 )
               instruction/read-keys :phase :idx) 2))
    ;; move back
    (is (= (-> state
               (assoc-in [:key :have] 37 )
               instruction/read-keys
               :phase :idx) 1))
    ;; start new
    (is (= (-> state
               (assoc-in [:phase :idx] last-instruction)
               instruction/read-keys :phase :name) :iti))

    ;; captcha when default (online)
    ;; TODO: should be in own test, but haven't moved out
    (is (not (instruction/pass-captcha state "notcat")))
    (is (instruction/pass-captcha state "cat"))

    ;; original-push-to-go text 
    (is (re-find #"Push the right arrow to start!" (str (txt-fnc state))))

    ;; when we're using mri
    ;; only trigger will advance
    (with-redefs
      [settings/current-settings
       (-> @settings/current-settings
           (assoc :keycodes settings/mri-glove-keys)
           (assoc :where :mri)
           atom)]

      ;; no captcha -- passes when wrong phrase or nil
      ;; mri doesn't have keyboard so cant respond
      ;; like above, this should move into it's own test
      (is (instruction/pass-captcha state-mri "notcat"))
      (is (instruction/pass-captcha state-mri nil))

      ;; have MRI specific text
      (is (re-find #"Waiting for scanner" (str (txt-fnc state-mri))))

      ;; move normally on most instructions
      (is (= (-> state-mri
                 (assoc-in [:phase :idx] 1)
                 instruction/read-keys :phase :idx) 2))
      ;; can't move forward
      (is (= (-> state-mri
                 (assoc-in [:phase :idx] last-instruction)
                 instruction/read-keys :phase :name) :instruction))
      ;; can't move back
      (is (= (-> state-mri
                 (assoc-in [:key :have] (:left settings/mri-glove-keys))
                 (assoc-in [:phase :idx] last-instruction)
                 instruction/read-keys :phase :idx) last-instruction))
      ;; but trigger will start task
      (is (= (-> state-mri
                 (assoc-in [:key :have] (:trigger settings/mri-glove-keys))
                 (assoc-in [:phase :idx] last-instruction)
                 instruction/read-keys :phase :name) :iti)))))
