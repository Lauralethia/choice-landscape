(ns landscape.model.records (:require [landscape.utils :as utils]))
   


;; start time records both actual timestamp and position in animation clock
;; in model/STATE as [:record :start-time]
(defrecord start-time [animation browser])
(defn make-start-time [ani-time-cur] (->start-time ani-time-cur (utils/now)))

;; what data we will send to the server
;; :events has majority of data: 
;;    onset times, choice, score. see [phase/phone-home]
;;
;; in model/STATE as :record
(defrecord record          [events url avatar ^start-time start-time survey mturk])
(defn record-init [] (->record  []  {}    nil                     {}     [] {:code "WXYZ1"}))
