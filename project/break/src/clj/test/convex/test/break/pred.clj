(ns convex.test.break.pred

  "Tests Convex core type predicate. 
  
   Specialized predicates such as `contains-key?` or `fn?` are located in relevant namespace."

  {:author "Adam Helinski"}

  (:require [clojure.test                  :as T]
            [clojure.test.check.generators :as TC.gen]
            [clojure.test.check.properties :as TC.prop]
            [convex.break                  :as $.break]
            [convex.cell                   :as $.cell]
            [convex.eval                   :as $.eval]
            [convex.gen                    :as $.gen]
            [convex.std                    :as $.std]
            [helins.mprop                  :as mprop]))


;;;;;;;;;;


(defn- -prop

  ;; Used by [[pred-data-false]] and [[pred-data-true]].

  [f-cell result? gen]

  (TC.prop/for-all [x gen]
    (result? ($.eval/result $.break/ctx
                            ($.cell/* (~f-cell (quote ~x)))))))



(defn prop-false

  "Like [[pred-data-true]] but tests for negative results."

  [f-cell gen]

  (-prop f-cell
         $.std/false?
         gen))



(defn prop-true

  "Tests if a value generated by `gen` passes a data predicate on the CVM.
  
   If `f-clojure` is given, also ensures that the very same value produces the exact same result
   in Clojure."

  [f-cell gen]

  (-prop f-cell
         $.std/true?
         gen))


;;;;;;;;;;


(mprop/deftest account?--false

  {:ratio-num 10}

  (prop-false ($.cell/* account?)
              (TC.gen/such-that (comp not
                                      $.std/address?)
                                $.gen/any)))



(mprop/deftest address?--false

  {:ratio-num 10}

  (prop-false ($.cell/* address?)
              (TC.gen/such-that (comp not
                                      $.std/address?)
                                $.gen/any)))



(mprop/deftest address?--true

  {:ratio-num 50}

  (prop-true ($.cell/* address?)
             $.gen/address))



(mprop/deftest blob?--false

  {:ratio-num 10}

  (prop-false ($.cell/* blob?)
              (TC.gen/such-that (comp not
                                      $.std/blob?)
                                $.gen/any)))



(mprop/deftest blob?--true

  {:ratio-num 50}

  (prop-true ($.cell/* blob?)
             ($.gen/blob)))



(mprop/deftest boolean?--false

  {:ratio-num 10}

  (prop-false ($.cell/* boolean?)
              (TC.gen/such-that (comp not
                                      $.std/boolean?)
                                $.gen/any)))



(T/deftest boolean?--true

  (let [assertion #($.eval/true? $.break/ctx
                                 ($.cell/* (boolean? ~($.cell/boolean %))))]
    (T/is (assertion false))
    (T/is (assertion true))))



(mprop/deftest coll?--false

  {:ratio-num 10}

  (prop-false ($.cell/* coll?)
              $.gen/scalar))



(mprop/deftest coll?--true

  {:ratio-num 10}

  (prop-true ($.cell/* coll?)
             $.gen/any-coll))



(mprop/deftest keyword?--false

  {:ratio-num 10}

  (prop-false ($.cell/* keyword?)
              (TC.gen/such-that (comp not
                                      $.std/keyword?)
                                $.gen/any)))



(mprop/deftest keyword?--true

  {:ratio-num 50}

  (prop-true ($.cell/* keyword?)
             $.gen/keyword))



(mprop/deftest list?--false

  {:ratio-num 15}

  (prop-false ($.cell/* list?)
              (TC.gen/such-that (comp not
                                      $.std/list?)
                                $.gen/any)))



(mprop/deftest list?--true

  {:ratio-num 10}

  (prop-true ($.cell/* list?)
             $.gen/any-list))



(mprop/deftest long?--false

  {:ratio-num 10}

  (prop-false ($.cell/* long?)
              (TC.gen/such-that (comp not
                                      $.std/long)
                                $.gen/any)))



(mprop/deftest long?--true

  {:ratio-num 50}

  (prop-true ($.cell/* long?)
             $.gen/long))



(mprop/deftest map?--false

  {:ratio-num 15}

  (prop-false ($.cell/* map?)
              (TC.gen/such-that (comp not
                                      $.std/map?)
                                $.gen/any)))



(mprop/deftest map?--true

  {:ratio-num 10}

  (prop-true ($.cell/* map?)
             $.gen/any-map))



(mprop/deftest nil?--false

  {:ratio-num 10}

  (prop-false ($.cell/* nil?)
              (TC.gen/such-that some?
                                $.gen/any)))



(T/deftest nil?--true

  (T/is ($.eval/true? $.break/ctx
                      ($.cell/* (nil? nil))))

  (T/is ($.eval/true? $.break/ctx
                      ($.cell/* (nil? (do nil))))))



(mprop/deftest number?--false

  {:ratio-num 10}

  (prop-false ($.cell/* number?)
              (TC.gen/such-that (comp not
                                      $.std/number?)
                                $.gen/any)))



(mprop/deftest number?--true

  {:ratio-num 50}

  (prop-true ($.cell/* number?)
             $.gen/number))



(mprop/deftest set?--false

  {:ratio-num 10}

  (prop-false ($.cell/* set?)
              (TC.gen/such-that (comp not
                                      $.std/set?)
                                $.gen/any)))



(mprop/deftest set?--true

  {:ratio-num 10}

  (prop-true ($.cell/* set?)
             $.gen/any-set))



(mprop/deftest str?--false

  {:ratio-num 10}

  (prop-false ($.cell/* str?)
              (TC.gen/such-that (comp not
                                      $.std/string?)
                                $.gen/any)))



(mprop/deftest str?--true

  {:ratio-num 10}

  (prop-true ($.cell/* str?)
             ($.gen/string)))



(mprop/deftest symbol?--false

  {:ratio-num 10}

  (prop-false ($.cell/* symbol?)
              (TC.gen/such-that (comp not
                                      $.std/symbol?)
                                $.gen/any)))



(mprop/deftest symbol?--true

  {:ratio-num 50}

  (prop-true ($.cell/* symbol?)
             $.gen/symbol))



(mprop/deftest vector?--false

  {:ratio-num 10}

  (prop-false ($.cell/* vector?)
              (TC.gen/such-that (comp not
                                      $.std/vector?)
                                $.gen/any)))



(mprop/deftest vector?--true

  {:ratio-num 10}

  (prop-true ($.cell/* vector?)
             $.gen/any-vector))
