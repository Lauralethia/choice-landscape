(ns landscape.fixed-timing
"hardcoded timings. ostensibly for fixed MR task timing but useful for debugging
see core/gen-well-list")

 ;(with-redefs [landscape.settings/current-settings (atom (-> @landscape.settings/current-settings (assoc-in [:nTrials :pairsInBlock] 1) (assoc-in [:nTrials :devalue] 1)(assoc-in [:nTrials :devalue-good] 1)))] (landscape.core/gen-well-list))

;; TODO: set position at time of display using current settings if not set here
;; TODO: populate MR specific
(def trials
  "fixed timing list of trials. timing for isi and iti can be specified
  position of well shouldn't be hardcoded but current is"
  {
:debug
 [{:left {:step 1, :open true, :prob 20, :active-at 0, :pos {:x 180, :y 395}},
   :up   {:step 1, :open true, :prob 50, :active-at 0, :pos {:x 250, :y 348.8}},
   :right{:step 2, :open false, :prob 100, :active-at 0, :pos {:x 320, :y 395}},
   :catch-dur 2000,
   :iti-dur 200}
  {:left {:step 1, :open true, :prob 50, :active-at 0, :pos {:x 180, :y 395}},
   :up   {:step 1, :open false, :prob 20, :active-at 0, :pos {:x 250, :y 348.8}},
   :right{:step 2, :open true, :prob 100, :active-at 0, :pos {:x 320, :y 395}},
   :catch-dur 1000,
   :iti-dur 500}
  {:left {:step 1, :open false, :prob 100, :active-at 0, :pos {:x 180, :y 395}},
   :up   {:step 1, :open true, :prob 100, :active-at 0, :pos {:x 250, :y 348.8}},
   :right{:step 2, :open true, :prob 100, :active-at 0, :pos {:x 320, :y 395}},
   :iti-dur 1000}
  {:left {:step 1, :open false, :prob 75, :active-at 0, :pos {:x 180, :y 395}},
   :up   {:step 1, :open true, :prob 75, :active-at 0, :pos {:x 250, :y 348.8}},
   :right{:step 2, :open true, :prob 75, :active-at 0, :pos {:x 320, :y 395}},
   :iti-dur 1000}],
 :mrA1 [],
 :mrA2 [],
 :mrB1 [],
 :mrB2 []})
