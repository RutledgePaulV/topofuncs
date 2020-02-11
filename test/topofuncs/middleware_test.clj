(ns topofuncs.middleware-test
  (:require [clojure.test :refer :all])
  (:require [topofuncs.middleware :as tm]))


(tm/def-middleware-graph middleware)

(tm/def-middleware-graph-impl middleware
  {:name :query-params}
  [handler]
  (fn [request] (handler (assoc request :qp {}))))

(tm/def-middleware-graph-impl middleware
  {:name  :authentication
   :after #{:query-params :form-params}}
  [handler]
  (fn [request] (handler (assoc request :auth (select-keys request [:qp :fp])))))

(tm/def-middleware-graph-impl middleware
  {:name :form-params}
  [handler]
  (fn [request] (handler (assoc request :fp {}))))

(deftest def-middleware-graph-test
  )
