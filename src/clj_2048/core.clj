(ns clj-2048.core
  (:require [merpg.2D.core :refer :all]
            [merpg.2D.make-game :refer :all]
            [clj-2048.state :refer [getin!
                                    W H]])
  (:import  [java.awt Color Font]))

(alter-var-root #'*out* (constantly *out*))

(defn render [& _]
  (dotimes [x W]
    (dotimes [y H]
      (let [tile (getin! x y)
            color (if (= tile false)
                    "#888888"
                    (Color. (int (mod tile 255))
                            (int (mod (* tile Math/PI) 255))
                            (int (mod (* tile Math/PI 2) 255))))]
        (with-color color
          (Rect (+ (* 100 x) 10)
                (+ (* 100 y) 10)
                90 90 :fill? true))
        (if tile
          (with-color Color/WHITE
            (Draw (str tile) [(+ (* 100 x)
                                 30)
                              (- (* 100 (inc y))
                                 45)
                              ])))
        ))))

(merpg.2D.make-game/make-game {}
             :window-width 800
             :window-height 800
             :mouse-clicked (fn [[x y]] )
             :update identity 
             :title "CLJ-2048 by Feuer   -   inspired by Gabriele Cirulli"
             :pre-drawqueue #'render             
             :post-drawqueue (fn [& _]) )
