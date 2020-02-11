(ns topofuncs.transcoding
  (:require [missing.topology :as top]
            [missing.core :as miss]
            [clojure.java.io :as io])
  (:import (java.io OutputStream PipedOutputStream PipedInputStream InputStream)))

(defn pipestream-combinator
  ([]
   (fn [^InputStream input-stream ^OutputStream output-stream _]
     (with-open [is input-stream os output-stream] (io/copy is os))))
  ([pipe1 pipe2]
   (fn [^InputStream input-stream ^OutputStream output-stream options]
     (with-open
       [sink   output-stream
        source (let [input  (PipedInputStream.)
                     output (PipedOutputStream.)]
                 (.connect input output)
                 (future
                   (with-open [source input-stream
                               sink   output]
                     (pipe1 source sink options)))
                 input)]
       (pipe2 source sink options)))))

(defmacro def-transcoding-graph [symbol]
  `(let [state# (atom {})]
     (def ~symbol
       (with-meta
         (fn [input# output#]
           (let [s# (-> state# deref :dispatch)]
             (or (get s# [input# output#])
                 (let [msg# (format "No path from %s to %s is defined." input# output#)]
                   (throw (ex-info msg# {:input input# :output output#}))))))
         {::state state#}))))

(defmacro def-transcoding-graph-impl [graph attrs bindings & body]
  `(let [dispatch#   ~graph
         attributes# ~attrs
         state#      (-> dispatch# meta ::state)
         fun#        (fn ~bindings ~@body)
         g-part#     (top/inverse {(:output attributes#) (set (:inputs attributes#))})
         w-part#     (into {} (for [i# (set (:inputs attributes#))]
                                [[i# (:output attributes#)] (:weight attributes# 1)]))
         f-part#     (into {} (for [i# (set (:inputs attributes#))]
                                [[i# (:output attributes#)] fun#]))
         mutator#    (fn [s#]
                       (let [g# (top/union (:graph s# {}) g-part#)
                             w# (merge (:weights s# {}) w-part#)
                             f# (merge (:functions s# {}) f-part#)
                             d# (miss/map-vals
                                  (fn [p#]
                                    (->> (if (= 1 (count (:path p#)))
                                           (take 2 (cycle (:path p#)))
                                           (:path p#))
                                         (partition 2 1)
                                         (map f#)
                                         (reduce pipestream-combinator)))
                                  (->> (top/shortest-paths g# (fn [a# b#] (get w# [a# b#])))
                                       (miss/filter-vals (comp pos? :distance))))]
                         (assoc s# :graph g# :weights w# :dispatch d# :functions f#)))]
     (swap! state# mutator#)
     dispatch#))
