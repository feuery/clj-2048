(ns clj-2048.state
  (:require [clojure.pprint :refer [pprint]]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.java.io :as io]
            [clj-2048.macros.multi :refer :all]
            [seesaw.core :refer [alert]]))

(defn get-settings []
  (let [conf-file-name (-> (System/getProperty "user.home")
                           (str "/.clj-2048"))]
    (when-not (-> conf-file-name
                  io/file
                  .exists)
      (spit conf-file-name ";; This config file is just runnable clojure. You can modify it to do stuff - just make sure to leave this map at the tail position\n {:w 4\n :h 4\n :screen-w 800\n :screen-h 600\n}")
      (alert (str "Created a conf file at " conf-file-name ". Try to edit it and reload the game, if you dare!")))
    (let [toret (-> conf-file-name
        slurp
        read-string
        eval)]
      (println "Config: " toret)
      toret)))

(def settings (get-settings))

(def W (:w settings))
(def H (:h settings))

(defn vectorify-2d [seq]
  (->> seq
       (map vec)
       vec))

;; Can I apply automatic transformation on values trying to be set on this atom?
;; Or do I have to vectorify-2d this atom in the tail-position of every swap!?
(def world (atom (->>  false
                       (repeat W)
                       (repeat H)
                       vectorify-2d)))

(def lost? (atom false))

(defn getin [vec x y]
  (-> vec
      (nth y)
      (nth x)))

(defn getin! [x y]
  (getin @world x y))

(defn pretty-print-world! []
  (pprint  @world))

(defn width [world]
  (count world))

(defn height [world]
  (count (first world)))

(defn spawn-randomly [world]
  "Returns a new world with a 2 or 4 - tile spawned randomly"
  (let [new-tile (if (< (rand-int 10) 5)
                   2
                   4)
        coord-pairs (for [x (range 0 (width world))
                          y (range 0 (height world))]
                      [x y])
        viable-coord-pairs (->> coord-pairs
                                (filter (complement (partial get-in world))))
        ;;Nth returns what it's supposed to and get returns garbage - wtf clj?
        final-coord-pair (nth viable-coord-pairs (rand-int (count viable-coord-pairs))
                              :you-lost)]
    (if (= final-coord-pair :you-lost)
      (do
        (reset! lost? true)
        world)
      (->> new-tile
           (assoc-in world final-coord-pair)
           vectorify-2d))))

(defn spawn-randomly! []
  "Changes the current state of the game. Never returns nothing but a vec"
  (swap! world spawn-randomly))

(def-real-multi do-move [_ dir] dir)

(defn filter-false-tiles [horizontal-row]
  (->>  horizontal-row
        (partition-by (partial = false))
        (filter #(not= false (first %)))
        flatten))

(defmethod do-move :right [horizontal-row dir]
  (let [significant-row (filter-false-tiles horizontal-row)
        number-of-falses (- (count horizontal-row)
                            (count significant-row))]
    (concat (repeat number-of-falses false)
            significant-row)))    
    

(defmethod do-move :left [horizontal-row dir]
  (let [significant-row (filter-false-tiles horizontal-row)
        number-of-falses (- (count horizontal-row)
                            (count significant-row))]
    (concat significant-row
            (repeat number-of-falses false))))

(defn transpose [seq-of-seqs]
  {:pre [(not (empty? seq-of-seqs))]}
  (try
    (apply mapv vector seq-of-seqs)
    (catch clojure.lang.ArityException ex
      (println "ArityEx @ transpose")
      (println "seq-of-seqs: " seq-of-seqs)
      (throw ex))))

(def-real-multi do-combine-tiles [horizontal-row dir] dir)

(defmethod do-combine-tiles :left [horizontal-row dir]
  (let [original-w (count horizontal-row)
        toret (->> horizontal-row
                   (partition-by identity)
                   (map (fn [[a b & rest :as params]]
                          (if (every? (partial = false) params)
                            params
                            (concat (vector
                                     (if (and a (nil? b)) a
                                         (if (and a b)
                                           (+ a b)))) rest))))
                   flatten
                   vec)
        recur? (->> toret
                    (partition-by identity)
                    (some #(and (not (every? (partial = false) %))
                                (> (count %) 1))))
        tiles-to-add (- original-w
                        (count toret))]
    (if recur?
      (concat (do-combine-tiles toret dir)
              (repeat tiles-to-add false))
      (concat toret
              (repeat tiles-to-add false)))))

(defmethod do-combine-tiles :right [horizontal-row dir]
  (-> horizontal-row
      reverse
      (do-combine-tiles :left)
      reverse))

(defn combine-tiles [world dir]
  {:pre [(or
          (= dir :left)
          (= dir :right)
          (= dir :up)
          (= dir :down))]}
        (if (or
             (= dir :left)
             (= dir :right))
          (map #(do-combine-tiles % dir) world)
          (let [direction (case dir
                            :up :left
                            :down :right
                            :error)]
            (-> world
                transpose
                (combine-tiles direction)
                transpose))))

(defn can-move? [world dir]
  (if (or (= dir :left)
          (= dir :right))
       (->> world
       (map (fn [horizontal-row]
              (let [significant-row (filter-false-tiles horizontal-row)]
                    (- (count horizontal-row)
                       (count significant-row)))))
       (reduce +)
       pos?)
       (let [dir (case dir
                            :up :left
                            :down :right
                            :error)]
         (if-not (nil? dir)
           (recur (transpose world) dir)
           false))))
          
(defn move [world dir]
  {:pre [(or
          (= dir :left)
          (= dir :right)
          (= dir :up)
          (= dir :down))]}
  (if (can-move? world dir)
    (let [moved-world
          (if (or
               (= dir :left)
               (= dir :right))
            (vectorify-2d (map #(do-move % dir) world))
            (let [direction (case dir
                              :up :left
                              :down :right
                              :error)]
              (-> world
                  transpose
                  (move direction)
                  transpose)))]
      (vectorify-2d (combine-tiles moved-world dir)))))
  

(defn move! [dir]
  (swap! world move dir)
  (if-not @lost?
    (spawn-randomly!)
    (pretty-print-world!)))

(spawn-randomly!)
