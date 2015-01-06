(ns merpg.2D.core
  (:require [seesaw.core :as seesaw]
            [clojure.string :as s]
            [clojure.java.io :refer [file]])
  (:import  [java.awt Color Font]
            [java.awt.event KeyEvent]
            [java.awt.image BufferedImage]
            [javax.imageio ImageIO]))

;;;;;; (def ^:dynamic *draw-queue* (atom nil)) ;Frames are used as keys...
(def ^:dynamic *buffer*  nil)
(def ^:dynamic *current-color* Color/WHITE)
(def ^:dynamic key-up? "Fn's to read the keyboard state will be bound to these vars when inside drawqueue-fns or update-fn." nil)
(def ^:dynamic key-down? "Fn's to read the keyboard state will be bound to these vars when inside drawqueue-fns or update-fn." nil)

(defn get-class
  "A type-hack that enables to dispatch differently on merpg-objects"
  [obj]
  (if (map? obj)
    (cond
     (nil? (:animated-object? obj)) (class obj)
     (:animated-object? obj) :animated-object
     :t :static-object)
    (class obj)))


(defmulti location get-class)
(defmulti position-at (fn [this [x y]] (get-class this)))
(defmulti move (fn [this how-much] (get-class this)))
(defmulti Dimensions get-class)

(defmacro with-handle
  "Brings a handle-var, which is (.getGraphics *buffer*), into the containing forms. Also handles setting the *current-color*."
  [& forms]
  `(try
     (let [~'handle (.getGraphics *buffer*)
           old-color# (.getColor ~'handle)]
       (try
         (cond
          (= java.awt.Color (class *current-color*))
            (.setColor ~'handle *current-color*)
          (string? *current-color*)
            (.setColor ~'handle (Color/decode *current-color*))
          :t
            (throw (Exception. (str "*current-color* = " *current-color*))))
         (.setFont ~'handle (Font. Font/SANS_SERIF Font/PLAIN 20))
         ~@forms
         (finally
           (.setColor ~'handle old-color#))))
     (catch NullPointerException ex#
       (println "*buffer* " (if-not (nil? *buffer*) "not" " nil"))
       (println "*color* " (if-not (nil? *current-color*) "not") " nil"))))

(defmacro draw-to-surface
  "Returns the surface, not whatever the last of forms returns"
  [surface & forms]
  `(binding [*buffer* ~surface]
     ~@forms
     *buffer*))

(defmacro with-color [color & other-forms]
    `(binding [*current-color* ~color]
       ~@other-forms))

(defmacro def-primitive-draw
  "Defines a function to do both Drawing and Filling a primitive shape. You have to define the exact procedures of doing these.

Introduces following bindings into the namespace:
x      - x-component of the location where to draw the primitive
y      - y-component of the location where to draw the primitive
width  - desired with of the primitive
height - desired height of the primitive
fill?  - to fill the primitive or to not? You shouldn't need this parameter when writing the function, but this is essential for callers.

handle - The Graphics handle to which you draw stuff

Example implementation of Rect: (def-primitive-draw Rect  :doc-string \"Here be dragons\"   :fill (.fillRect handle x y width height) :draw (.drawRect handle x y width height))"

  [name & {:keys [doc-string] :or {doc-string ""}}]
  (let [fill-name (symbol (str ".fill" (s/capitalize name)))
        draw-name (symbol (str ".draw" (s/capitalize name)))]
    `(defn ~name
       ~doc-string
       [~'x ~'y ~'width ~'height & {:keys [~'fill?] :or {~'fill? false}}]
       (with-handle
         (if ~'fill?
           (~fill-name ~'handle ~'x ~'y ~'width ~'height)
           (~draw-name ~'handle ~'x ~'y ~'width ~'height))))))

(def-primitive-draw Rect)

(def-primitive-draw Oval)

(defn Triangle [x1 y1 x2 y2 x3 y3]
  (with-handle
    (.fillPolygon handle (int-array [x1 x2 x3]) (int-array [y1 y2 y3]) 3)))

;(defn Rect [x y width height & {:keys [fill?] :or {fill? false}}]
;  (with-handle
;    (if fill?
;      (.fillRect handle x y width height)
;      (.drawRect handle x y width height))))
   

(defn Line
  ([[x1 y1][x2 y2]]
     (with-handle
       (.drawLine handle x1 y1 x2 y2)))
  ([x1 y1 x2 y2]
     (Line [x1 y1] [x2 y2])))

(defn image
  ([path]
     (ImageIO/read (file path)))
  ([width height]
     (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)))
  

(defmulti Draw
  (fn [this & rest]
;    (when (map? this)
;      (println "Drawing a map...")
;      (println (get-class this))
;      (println this))
    [(get-class this) (count rest)]))
;(defmulti   (Draw [this [x y]])) ;;Draw'll handle the Draw cases also

(defmethod Draw [java.lang.String 1] ; the dispatch value
  [this [x y]] ;params
      (with-handle
        (.drawString handle this x y)
        this))

(defmethod Draw [BufferedImage 0] [this]
  (with-handle
    (try
      (.drawImage handle this 0 0 nil)
      this
      (catch IllegalArgumentException ex
        (println "Error [bfimg 0]ssa")
        (throw ex)))))

(defmethod Draw [BufferedImage 1] [this [x y]]
  (let [x (int x)
        y (int y)]
    (with-handle
      (try
        (.drawImage handle this x y nil)
        this
        (catch IllegalArgumentException ex
          (println "Handle: " handle)
          (println "Classes of location: " [(class x) (class y)])
          (println "Error [bfimg 1]ssä")
          (throw ex))))))



(defn object [path-or-img & {:keys [x y angle visible?] :or {x 0 y 0 angle 0 visible? true}}]
  {:pre [(<= 0 x 360)]}
  
  {:x x :y y :img (ref (if (string? path-or-img)
                       (image path-or-img)
                       (if (nil? path-or-img) ;; If called by merpg.2D.animation/animation, img-field is best kept empty
                         nil
                         path-or-img)))
   :Angle angle
   :visible? visible?
   :animated-object? false})

(defmethod location :static-object [this]
  ;(println "Returning location")
  {:x (:x this) :y (:y this)})


(defn map-to-vec [map]
  (if (vector? map)
    map
    (-> map
        vals
        vec)))

(defmethod Draw [:static-object 0]
  [this]
  ;(println "Drawing " [:static-object 0])
  (Draw @(:img this) (map-to-vec (location this))))

(defmethod Draw [:static-object 2]
  [this coordinates]
  ;(println "Drawing " [:static-object 2])
  (Draw @(:img this) (map-to-vec coordinates)))

(defmethod position-at :static-object [this [x y]]
  (let [toret (-> this
                  (assoc :x x)
                  (assoc :y y))]
    toret))

(defn cos [angle]
  (Math/cos (Math/toRadians angle)))

(defn sin [angle]
  (Math/sin (Math/toRadians angle)))

(defmethod move :static-object [this how-much]
  (let [{x :x y :y} (location this)]
      (position-at this [(+ x (* how-much (cos (:Angle this))))
                       (+ y (* how-much (sin (:Angle this))))])))
(defmethod Dimensions :static-object [this]
  [(.getWidth @(:img this)) (.getHeight @(:img this))])

(defn in? 
  "true if seq contains elm"
  [seq elm]  
  (some #(= elm %) seq))

(defn merpg-object? [obj]
  (in? [:static-object :animated-object] (get-class obj)))

(defn width
  ([surface]
     {:pre [(instance? java.awt.Image surface)]} 
     (.getWidth surface))
  ([]
       (width *buffer*)))

(defn height
  ([surface]
     {:pre [(instance? java.awt.Image surface)]}
     (.getHeight surface))
  ([]
     (height *buffer*)))
     
(defn subimage
  ([surface x y w h]
     ;(println (map class [x y w h]))
     (let [[x y w h] [(int x) (int y) (int w) (int h)]]
       (.getSubimage surface x y w h)))
  ([x y w h]
     (subimage *buffer* x y w h)))




