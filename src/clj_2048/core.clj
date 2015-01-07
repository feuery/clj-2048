(ns clj-2048.core
  (:require [merpg.2D.core :refer :all]
            [merpg.2D.make-game :refer :all]
            
            [clj-2048.state :refer [getin! move! lost?
                                    settings
                                    W H]])
  (:import  [java.awt Color Font])
  (:gen-class))

(alter-var-root #'*out* (constantly *out*))

(defn render [& _]
  (dotimes [x W]
    (dotimes [y H]
      (let [tile (getin! x y)]
        (when-not (nil? tile)
          (let [color (if (= tile false)
                    "#888888"
                    (Color. (int (mod tile 255))
                            (int (mod (* tile Math/PI) 255))
                            (int (mod (* tile Math/PI 2) 255))))]
        (with-color color
          (Rect (+ (* 100 x) 10)
                (+ (* 100 y) 10)
                90 90 :fill? true)))
        (if tile
          (with-color Color/WHITE
            (Draw (str tile) [(+ (* 100 x)
                                 30)
                              (- (* 100 (inc y))
                                 45)
                              ])))))))
  {})

(defn render-lost [_]
  (when @lost?
    (with-color "#FF0000"
      (Draw "YOU LOST" [400 300])))
  {})

(defn -main [& _]
  (merpg.2D.make-game/make-game {}
             :window-width (:screen-w settings)
             :window-height (:screen-h settings)
             :update (fn [_]
                       (when-not @lost?
                         (let [direction (cond
                                           (key-down? :down) :down
                                           (key-down? :up) :up
                                           (key-down? :left) :left
                                           (key-down? :right) :right)]
                           (when-not (nil? direction)
                             (if (= (move! direction) :you-lost)
                               (reset! lost? true)))))
                       {})
                             
             :title "CLJ-2048 by Feuer   -   inspired by Gabriele Cirulli"
             :pre-drawqueue #'render             
             :post-drawqueue #'render-lost ))
