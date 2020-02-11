(ns topofuncs.middleware
  (:require [missing.topology :as top]
            [missing.core :as miss]))

(defmacro def-middleware-graph [symbol]
  `(let [state# (atom {})]
     (def ~symbol
       (with-meta
         (fn [mw#]
           (let [stacks# (-> state# deref :stacks)]
             (or (get stacks# mw#)
                 (let [msg# (format "No middleware stack for %s exists." mw#)]
                   (throw (ex-info msg# {:mw mw#}))))))
         {::state state#}))))

(defmacro def-middleware-graph-impl [graph attrs bindings & body]
  `(let [dispatch#   ~graph
         attributes# ~attrs
         state#      (-> dispatch# meta ::state)
         fun#        (fn ~bindings ~@body)
         mw-key#     (:name attributes#)
         after#      {mw-key# (set (:after attributes# #{}))}
         before#     (top/inverse {mw-key# (set (:before attributes# #{}))})
         mutator#    (fn [s#]
                       (let [g#       (-> (:graph s# {}) (top/union after#) (top/union before#))
                             d#       (assoc (:dispatch s# {}) mw-key# fun#)
                             closure# (top/transitive-closure g#)
                             stacks#  (into {} (for [name# (keys d#)
                                                     :let [part# (conj (get closure# name# #{}) name#)]]
                                                 [name# (->> (top/filterg part# g#)
                                                             (top/union {name# #{}})
                                                             (top/topological-sort)
                                                             (map d#)
                                                             (reduce (miss/flip comp)))]))]
                         (assoc s# :graph g# :dispatch d# :stacks stacks#)))]
     (swap! state# mutator#)
     dispatch#))


