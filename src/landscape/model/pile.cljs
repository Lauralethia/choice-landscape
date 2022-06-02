(ns landscape.model.pile
"grid with accumuating pile of 'gold'
a la https://jason.today/falling-sand "
)


(defn grid-make
  "create 'grid' as a 1d vector"
  [w h v]
  (to-array (repeatedly w #(to-array (take h (repeat v))))))

(defn grid-set
  "set a value. not needed. but in javascript example"
  [grid x y v]
  (aset grid x y v))

(defn grid-swap
  "set a value"
  [grid x1 y1 x2 y2]
  (let [a (get-in grid [x1 y1])
        b (get-in grid [x2 y2])]
    (aset grid x1 y1 b)
    (aset grid x2 y2 a)))


(defn pos-empty? [grid x y] (= 0 (get-in grid [x y])))

(defn color [canvas color])
