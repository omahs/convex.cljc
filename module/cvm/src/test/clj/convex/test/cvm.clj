(ns convex.test.cvm

  {:author "Adam Helinski"}

  (:require [clojure.test :as T]
            [convex.cell  :as $.cell]
            [convex.cvm   :as $.cvm]))


;;;;;;;;;; From expansion to execution


(T/deftest execution

  (let [form ($.cell/* (if true 42 0))]
    (T/is (= ($.cell/* 42)
             (->> form
                  ($.cvm/eval ($.cvm/ctx))
                  ($.cvm/result))
             (->> form
                  ($.cvm/eval ($.cvm/ctx))
                  ($.cvm/result))
             (->> form
                  ($.cvm/expand ($.cvm/ctx))
                  ($.cvm/compile)
                  ($.cvm/exec)
                  ($.cvm/result))
             (->> form
                  ($.cvm/expand-compile ($.cvm/ctx))
                  ($.cvm/exec)
                  ($.cvm/result))))))


;;;;;;;;;;


(T/deftest exception

  (T/testing
    "Without exception"

    (T/is (nil? ($.cvm/exception ($.cvm/ctx))))

    (T/is (false? ($.cvm/exception? ($.cvm/ctx)))))

  (T/testing
    "With exception"

    (let [code    ($.cell/* :code)
          message ($.cell/* :message)
          ctx     ($.cvm/exception-set ($.cvm/ctx)
                                       code
                                       message)
          ex      ($.cvm/exception ctx)]

      (T/is (= code
               ($.cvm/exception-code ex)))

      (T/is (= message
               ($.cvm/exception-message ex)))

      (T/is ($.cvm/exception? ctx))

      (T/is (-> ctx
                ($.cvm/exception-clear)
                ($.cvm/exception)
                (nil?))))))
