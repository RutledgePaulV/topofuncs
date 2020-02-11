A collection of macros and combinators for defining composable function graphs. Graph
traversals are precomputed during macroexpansion so there is no runtime performance penalty.


Transcoding - for file format conversions.

```clojure
(require '[topofuncs.transcoding :as tf])

(tf/def-transcoding-graph convert)

(tf/def-transcoding-graph-impl convert
  {:inputs #{"image/png" "image/jpeg"}
   :output "image/jpeg"}
  [^InputStream input-stream ^OutputStream output-stream options]
  )

(tf/def-transcoding-graph-impl convert
  {:inputs #{"application/pdf"}
   :output "image/png"}
  [^InputStream input-stream ^OutputStream output-stream options]
  )

(def pdf->jpeg (convert "application/pdf" "image/jpeg"))
(pdf->jpeg input-stream output-stream {})
```


Middleware - for dependent middleware.

```clojure 
(require '[topofuncs.middleware :as tm])

(tm/def-middleware-graph middleware)

(tm/def-middleware-graph-impl middleware
  {:name :query-params}
  [handler]
  (fn [request] (handler (assoc request :qp {}))))

(tm/def-middleware-graph-impl middleware
  {:name  :authentication
   :after #{:query-params :form-params}}
  [handler]
  (fn [request]
    (if (or (contains? (:qp request) :authenticated)
            (contains? (:fp request) :authenticated))
      (handler request)
      {:status 401 :body "Unauthorized!"})))

(tm/def-middleware-graph-impl middleware
  {:name :form-params}
  [handler]
  (fn [request] (handler (assoc request :fp {}))))

(def endpoint-mw (middleware :authentication))
(def app (endpoint-mw (fn [request] {:status 200 :body "Authenticated!"}))

```


