(ns landscape.model.pile
  "grid with accumuating pile of 'gold'
a la https://jason.today/falling-sand
http://langintro.com/cljsbook/canvas.html
https://developer.mozilla.org/en-US/docs/Web/API/Canvas_API/Tutorial/Pixel_manipulation_with_canvas

  "
  (:require [quil.core :as q]))

;; (q/defsketch my-sketch-definition
;;   :host "canvas-id"
;;   :draw draw
;;   :size [300 300])

(defn ^:export clean-sketch [id]
  (q/with-sketch (q/get-sketch-by-id id)
    (q/background 255)))

(defn grid-make
  "create 'grid' as a 2d vector. likley intialize with empty (0 0 0 0)"
  [w h v & {:keys [func] :or {func to-array}}]
  (func (repeatedly w #(func (take h (repeat v))))))

(defn grid-add-box [g v x y & {:keys [w h] :or {w 10 h 10}}]
  (print x y w h)
  (doall
   (for [xi (range x (+ x w))
         yi (range y (+ y w))]
     (aset g xi yi v))))

(defn grid-swap
  "swap value. useful for \"moving down\" a pixel"
  [grid x1 y1 x2 y2]
  (let [a (aget grid x1 y1)
        b (aget grid x2 y2)]
    (aset grid x1 y1 b)
    (aset grid x2 y2 a)))


(defn pos-empty? [g x y] (= 0 (get-in g [x y])))

(defn grid-gravity
  "move onto pixels into empty spaced. TODO: check direction is correct"
  [g w h]
  (doall (for [x (range w)
               y (reverse (range (- h 1)))]
           (let [y_below (+ y 1)]
             (when (pos-empty? g x y_below)
               (grid-swap g x y x y_below))))))


;; dealing with images
(defn get-ctx [id]
  (.getContext (.getElementById js/document id) "2d"))

(defn val-to-color
  "0-100 to rgba value. TODO: implement"
  [] (to-array [255 255 (rand-int 100) 255]))

(defn image-make
  "grid into linearized 3d x,y,color
  2 2 grid [a b, c d] with rgba(r,g,b,a)
  ar ag rb aa br bg bb ba  cr cg cb ca  dr dg db da
  TODO: use for macro's :let and :when?
  "
  [g w h & {:keys [bg] :or {bg [0 0 0 0]}}]
  (flatten (let [img (to-array (repeat (* w h 4) 0))]
                       (for [x (range w)] 
                         (let [w_offset (* x h)]
                           (for [y (range h)]
                             (let [h (* 4 (+ w_offset y))
                                   v (aget g x y)]
                               (for [cidx (range 4)]
                                 (let [cval (get v cidx (get bg cidx))]
                                   (aset img (+ h cidx) cval))))))))))

(defn image-draw [ctx g w h]
  (let [img (js/Uint8ClampedArray. (image-make g w h))
         obj (.createImageData ctx w h)]
    ;; (set! (. obj -data) img) ; cannot replace in bulk. need to do every pixel
    ;; this is now slow?! maybe using quil wont be so bad!
    (doall (for [i (range (alength (. obj -data)))]
             (aset (. obj -data) i (aget img i))))
     (.putImageData ctx obj 0 0)))

(defn quil-empty-grid []
  (grid-make (/ (q/width) 5)
             (/ (q/height) 5)
             0
             :func vec))
(defn quil-reset [] (swap! (q/state-atom) assoc :g (quil-empty-grid)))

(defn quil-setup []
  (q/frame-rate 10)
  (q/background (q/color 255 0 0 255))
  (q/set-state! :sz 50
                :g (quil-empty-grid)))

(defn quil-new []
  (let [color 255 ;; (rand-int 255)
        ]
   (swap! (q/state-atom) assoc-in [:g 0 0] color)))

(defn quil-draw [{:keys [sz g]:as state}]
  (q/background (q/color 0 0 0 0))
  ;; (q/create-image (q/width) (q/height) :rgb)
  ;; (dotimes [x (/ (q/width) sz)]
  ;;   (dotimes [y (/ (q/height) sz)]
  ;;     (if-let [color (get-in g [x y])]
  ;;       (let [xc (* x sz)
  ;;             yc (* y sz)]
  ;;         (q/rect xc yc (+ xc sz) (+ yc sz))))))

  ;; (for [x (range (/  (q/width) sz))
  ;;       y (range (/  (q/height) sz))
  ;;       :let [c (get-in g [x y])
  ;;             xc (* sz x)
  ;;             yc (* sz x)]
  ;;       :when (not(= 0 c))]
  ;;   (do (q/rect xc yc (+ xc sz) (+ yc sz) c)
  ;;       [x y xc yc]))
  (q/rect 0 0 50 50))

(defn quil-gravity [grid] grid)


(q/defsketch pile-sketch
  :host "q-pile"
  :draw quil-draw
  :setup quil-setup
  ;; :update quil-gravity
  :size [50 100])



(defn ^:export clean-canvas [id]
  (q/with-sketch (q/get-sketch-by-id id)
    (q/background 255)))
