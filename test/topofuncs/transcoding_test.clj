(ns topofuncs.transcoding-test
  (:require [clojure.test :refer :all])
  (:require [topofuncs.transcoding :as tf])
  (:import (java.io InputStream OutputStream)))


(tf/def-transcoding-graph convert)

(tf/def-transcoding-graph-impl convert
  {:inputs #{"image/png" "image/jpeg"}
   :output "image/png"}
  [^InputStream input-stream ^OutputStream output-stream options]
  )

(tf/def-transcoding-graph-impl convert
  {:inputs #{"application/pdf"}
   :output "image/png"}
  [^InputStream input-stream ^OutputStream output-stream options]
  )

(deftest def-transcoding-graph-test
  )
