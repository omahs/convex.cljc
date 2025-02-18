(ns convex.test.break.coll

  "Testing core functions operating on collections.
  
   Articulates essentially 2 kind of tests:

   - Regular function calls
   - Suites that some collections must pass, testing for consistency between collection functions.
  
   Main suites must be passed by all types of collection, whereas other suites like [[suite-assoc]] are specialized
   on collection type."

  {:author "Adam Helinski"}

  (:require [clojure.test.check.generators :as TC.gen]
            [clojure.test.check.properties :as TC.prop]
            [convex.break                  :as $.break]
            [convex.break.gen              :as $.break.gen]
            [convex.cell                   :as $.cell]
            [convex.eval                   :as $.eval]
            [convex.gen                    :as $.gen]
            [convex.std                    :as $.std]
            [helins.mprop                  :as mprop]))


(declare ctx-main
         suite-kv+)


;;;;;;;;;; Reusing properties


(defn suite-new

  "Suite that all new collections created with constructor functions (eg. `list`). must pass."

  [ctx form-type?]

  (mprop/mult

    "Type is correct"

    ($.eval/true? ctx
                  ($.cell/* (~form-type? x)))


    "Same number of key-values"

    ($.eval/true? ctx
                  ($.cell/* (= (count kv+)
                               (count x))))


    "All key-values can be retrieved"

    ($.eval/true? ctx
                  ($.cell/* ($/every? (fn [[k v]]
                                        (= v
                                           (get x
                                                k)
                                           (x k)))
                                      kv+)))))


;;;;;;;;;; Creating collections from functions


(mprop/deftest blob-map--

  (TC.prop/for-all [kv+ ($.break.gen/kv+ ($.gen/blob)
                                         $.gen/any)]
    (suite-new ($.eval/ctx $.break/ctx
                           ($.cell/* (do
                                       (def kv+
                                            ~kv+)
                                       (def x
                                            (blob-map ~@(mapcat identity
                                                                kv+))))))
               ($.cell/* (fn [_] true)))))



(mprop/deftest hash-map--

  ;; TODO. Also test failing with odd number of items.

  {:ratio-size 2}

  (TC.prop/for-all [kv+ ($.break.gen/kv+ $.gen/any
                                         $.gen/any)]
    (suite-new ($.eval/ctx $.break/ctx
                           ($.cell/* (do
                                       (def kv+
                                            ~kv+)
                                       (def x
                                            (hash-map ~@(mapcat identity
                                                                kv+))))))
               ($.cell/* map?))))



(mprop/deftest hash-set--

  (TC.prop/for-all [x+ (TC.gen/vector-distinct ($.gen/quoted $.gen/any))]
    (suite-new ($.eval/ctx $.break/ctx
                           ($.cell/* (do
                                       (def kv+
                                            ~($.cell/vector (map (fn [x]
                                                                   ($.cell/* [~x true]))
                                                                 x+)))
                                       (def x
                                            (hash-set ~@x+)))))
               ($.cell/* set?))))



(defn- -add-index+
  [x+]
  ($.cell/vector (map (fn [i x]
                        ($.cell/vector [($.cell/long i)
                                        x]))
                      (range)
                      x+)))



(mprop/deftest list--

  (TC.prop/for-all [x+ (TC.gen/vector ($.gen/quoted $.gen/any))]
    (suite-new ($.eval/ctx $.break/ctx
                           ($.cell/* (do
                                       (def kv+
                                            ~(-add-index+ x+))
                                       (def x
                                            (list ~@x+)))))
               ($.cell/* list?))))



(mprop/deftest vector--

  (TC.prop/for-all [x+ (TC.gen/vector ($.gen/quoted $.gen/any))]
    (suite-new ($.eval/ctx $.break/ctx
                           ($.cell/* (do
                                       (def kv+
                                            ~(-add-index+ x+))
                                       (def x
                                            (vector ~@x+)))))
               ($.cell/* vector?))))


;;;;;;;;;; Main - Creating an initial context


(defn ctx-assoc

  "Creating a base context suitable for main suites and suites operating on types that support
   `assoc`.

   Relies on [[ctx-main]], where `x-2` becomes `x` with the added key-value."

  ([[x k v]]

   (ctx-assoc x
              k
              v))


  ([x k v]

   (ctx-main x
             ($.cell/* (assoc ~x
                              ~k
                              ~v))
             k
             v)))



(defn ctx-main

  "Creates a context by interning the given values (using same symbols as this signature).

   `k` and `v` are quoted."

  [x x-2 k v]

  ($.eval/ctx $.break/ctx
              ($.cell/* (do
                          (def k
                               ~k)
                          (def v
                               ~v)
                          (def x
                               ~x)
                          (def x-2
                               ~x-2)))))


;;;;;;;;;; Main - Different suites targeting different collection capabilities


(defn suite-assoc

  "See checkpoint."

  [ctx]

  (mprop/check

    "`assoc`"

    (mprop/mult
  
      "Associating existing value does not change anything"

      ($.eval/true? ctx
                    ($.cell/* (= x-2
                                 (assoc x-2
                                        k
                                        v))))

  
      "Consistent with `assoc-in`"

      ($.eval/true? ctx
                    ($.cell/* (= x-2
                                 (assoc-in x
                                           [k]
                                           v)))))))



(defn suite-dissoc

  "See checkpoint.
  
   Other `dissoc` tests based around working repeatedly with key-values are in [[suite-kv+]]."

  ;; TODO. Follow this issue, sets should pass this suite: https://github.com/Convex-Dev/convex/issues/178

  [ctx]

  (mprop/check

    "Suite revolving around `dissoc` and its consequences measurable via other functions."

    (let [ctx-2 ($.eval/ctx ctx
                            ($.cell/* (def x-3
                                           (dissoc x-2
                                                   k))))]
      (mprop/mult

        "Does not contain key anymore"

        ($.eval/true? ctx-2
                      ($.cell/* (not (contains-key? x-3
                                                    k))))


        "`get` returns nil"

        ($.eval/true? ctx-2
                      ($.cell/* (nil? (get x-3
                                           k))))


        "Using collection as function returns nil"

        ($.eval/true? ctx-2
                      ($.cell/* (nil? (x-3 k))))


        "`get` returns 'not-found' value"

        ($.eval/true? ctx-2
                      ($.cell/* (= :convex-sentinel
                                   (get x-3
                                        k
                                        :convex-sentinel))))


        "`get-in` returns nil"

        ($.eval/true? ctx-2
                      ($.cell/* (nil? (get-in x-3
                                              [k]))))


        "`get-in` returns 'not-found' value"

        ($.eval/true? ctx-2
                      ($.cell/* (= :convex-sentinel
                                   (get-in x-3
                                           [k]
                                           :convex-sentinel))))


        "Keys do not contain key"

        ($.eval/true? ctx-2
                      ($.cell/* (not (contains-key? (set (keys x-3))
                                                    k))))


        "All other key-values are preserved"

        ($.eval/true? ctx-2
                      ($.cell/* ($/every? (fn [k]
                                            (= (get x-3
                                                    k)
                                               (get x-2
                                                    k)))
                                          (keys x-3))))


        "Equal to original or count updated as needed"

        ($.eval/true? ctx-2
                      ($.cell/* (if (nil? x)
                                  (= {}
                                     x-3)
                                  (if (contains-key? x
                                                     k)
                                    (= (count x-3)
                                       (dec (count x)))
                                    (= x
                                       x-3)))))))))



(defn suite-hash-map

  "Suite containing miscellaneous tests for hash-maps."

  [ctx]

  (mprop/check

    "Using `hash-map` to rebuild map"

    ($.eval/true? ctx
                  ($.cell/* (= x-2
                               (apply hash-map
                                      (reduce (fn [acc [k v]]
                                                (conj acc
                                                      k
                                                      v))
                                              []
                                              x-2)))))))



(defn suite-kv+

  "See checkpoint."

  [ctx]

  (mprop/check

    "Suite for collections that support `keys` and `values` (currently, only map-like types)."

    (let [ctx-2 ($.eval/ctx ctx
                            ($.cell/* (do
                                        (def k+
                                             (keys x-2))
                                        (def kv+
                                             (vec x-2))
                                        (def v+
                                             (values x-2)))))]
      (mprop/mult

        "Keys contain new key"

        ($.eval/true? ctx-2
                      ($.cell/* (contains-key? (set k+)
                                               k)))


        "Order of `keys` is consistent with order of `values`"

        ($.eval/true? ctx-2
                      ($.cell/* ($/every-index? (fn [k+ i]
                                                  (and (= (get x-2
                                                               (get k+
                                                                    i))
                                                          (get v+
                                                               i))
                                                       (= (x-2 (k+ i))
                                                          (v+ i))))
                                                k+)))


        "`vec` correctly maps key-values"

        ($.eval/true? ctx-2
                      ($.cell/* ($/every? (fn [[k v]]
                                            (= v
                                               (get x-2
                                                    k)
                                               (x-2 k)))
                                          kv+)))


        "`vec` is consitent with `into`"

        ($.eval/true? ctx-2
                      ($.cell/* (= kv+
                                   (into []
                                         x-2))))


        "Order of `keys` is consistent with `vec`"

        ($.eval/true? ctx-2
                      ($.cell/* (= k+
                                   (map first
                                        kv+))))


        "Order of `values` is consistent with `vec`"

        ($.eval/true? ctx-2
                      ($.cell/* (= v+
                                   (map second
                                        kv+))))


        "Order of `mapv` is consistent with `vec`"

        ($.eval/true? ctx-2
                      ($.cell/* (= kv+
                                   (mapv identity
                                         x-2))))


        "Contains all its keys"

        ($.eval/true? ctx-2
                      ($.cell/* ($/every? (fn [k]
                                            (contains-key? x-2
                                                           k))
                                          k+)))


        "`assoc` is consistent with `count`"

        ($.eval/true? ctx-2
                      ($.cell/* (= x-2
                                   (reduce (fn [x-3 [k v]]
                                             (let [x-4 (assoc x-3
                                                              k
                                                              v)]
                                               (if (= (count x-4)
                                                      (inc (count x-3)))
                                                 x-4
                                                 (reduced false))))
                                           (empty x-2)
                                           kv+))))


       "Using `assoc` to rebuild map in a loop"

       ($.eval/true? ctx-2
                     ($.cell/* (let [rebuild (fn [acc]
                                               (reduce (fn [acc-2 [k v]]
                                                         (assoc acc-2
                                                                k
                                                                v))
                                                       acc
                                                       x-2))]
                                 (= x-2
                                    (rebuild (empty x-2))
                                    (rebuild x-2)))))


       "Using `assoc` with `apply` to rebuild map"

       (let [ctx-3 ($.eval/ctx ctx-2
                               ($.cell/* (def arg+
                                              (reduce (fn [acc [k v]]
                                                        (conj acc
                                                              k
                                                              v))
                                                      []
                                                      kv+))))]
         (mprop/mult

           "From an empty map"

           ($.eval/true? ctx-3
                         ($.cell/* (= x-2
                                      (apply assoc
                                            (empty x-2)
                                            arg+))))


           "On the map itself"

           ($.eval/true? ctx-3
                         ($.cell/* (= x-2
                                      (apply assoc
                                             x-2
                                             arg+))))))))))



(defn suite-main-mono

  "See checkpoint."

  [ctx]

  (mprop/check

    "Suite that all collections must pass (having exactly 1 item)."

    (let [ctx-2 ($.eval/ctx ctx
                            ($.cell/* (def x-3
                                           (conj (empty x-2)
                                                 (first x-2)))))]
      (mprop/mult

        "`cons`"

        ($.eval/true? ctx-2
                      ($.cell/* (= (list 42
                                         (first x-3))
                                   (cons 42
                                         x-3))))


        "`count` returns 1"

        ($.eval/true? ctx-2
                      ($.cell/* (= 1
                                   (count x-3))))


        "Not empty"

        ($.eval/true? ctx-2
                      ($.cell/* (not (empty? x-3))))


        "`first` and `last` are equivalent, consistent with `nth`"

        ($.eval/true? ctx-2
                      ($.cell/* (= (first x-3)
                                   (last x-3)
                                   (nth x-3
                                        0))))


        "`next` returns nil"

        ($.eval/true? ctx-2
                      ($.cell/* (nil? (next x-3))))


        "`second` is exceptional"

        ($.eval/exception ctx-2
                          ($.cell/* (second x-3)))))))



(defn suite-main-poly

  "See checkpoint."

  [ctx]

  (mprop/check

    "Suite that all collections must pass (having >= 1 item)."

    (mprop/mult

      "Contains key"

      ($.eval/true? ctx
                    ($.cell/* (contains-key? x-2
                                             k)))


      "`get` returns the value"

      ($.eval/true? ctx
                    ($.cell/* (= v
                                 (get x-2
                                      k))))


      "Using collection as function returns the value"

      ($.eval/true? ctx
                    ($.cell/* (= v
                                 (x-2 k))))


      "`get-in` returns the value"

      ($.eval/true? ctx
                    ($.cell/* (= v
                                 (get-in x-2
                                         [k]))))


      "Cannot be empty"

      ($.eval/true? ctx
                    ($.cell/* (not (empty? x-2))))


      "Count is at least 1"

      ($.eval/true? ctx
                    ($.cell/* (>= (count x-2)
                                  1)))


      "`first` is not exceptional"

      ($.eval/true? ctx
                    ($.cell/* (do
                                (first x-2)
                                true)))


      "`(nth 0)` is not exceptional"

      ($.eval/true? ctx
                    ($.cell/* (do
                                (nth x-2
                                     0)
                                true)))


      "`last` is is not exceptional"

      ($.eval/true? ctx
                    ($.cell/* (do
                                (last x-2)
                                true)))


      "`nth` to last item is not exceptional"

      ($.eval/true? ctx
                    ($.cell/* (do
                                (nth x-2
                                     (dec (count x-2)))
                                true)))


      "`nth` is consistent with `first`"

      ($.eval/true? ctx
                    ($.cell/* (= (first x-2)
                                 (nth x-2
                                      0))))


      "`nth` is consistent with `last`"

      ($.eval/true? ctx
                    ($.cell/* (= (last x-2)
                                 (nth x-2
                                      (dec (count x-2))))))


      "`nth` is consistent with second"

      ($.eval/true? ctx
                    ($.cell/* (if (>= (count x-2)
                                      2)
                                (= (second x-2)
                                   (nth x-2
                                        1))
                                true)))


      "Using `concat` to rebuild collection as a vector"

      ($.eval/true? ctx
                    ($.cell/* (let [as-vec (vec x-2)]
                                (= as-vec
                                   (apply concat
                                          (map vector
                                               x-2))))))


      "`cons`"

      (let [ctx-2 ($.eval/ctx ctx
                              ($.cell/* (def -cons
                                             (cons (first x-2)
                                                   x-2))))]
        (mprop/mult
          
          "Produces a list"

          ($.eval/true? ctx-2
                        ($.cell/* (list? -cons)))


          "Count is coherent compared to the consed collection"

          ($.eval/true? ctx-2
                        ($.cell/* (= (count -cons)
                                     (inc (count x-2)))))


          "First elements are consistent with setup"

          ($.eval/true? ctx-2
                        ($.cell/* (= (first -cons)
                                     (second -cons)
                                     (first x-2))))


          "Consistent with `next`"

          ($.eval/true? ctx-2
                        ($.cell/* (= (vec (next -cons))
                                     (vec x-2))))))


      "`cons` repeatedly reverse a collection"

      ($.eval/true? ctx
                    ($.cell/* (= (into (list)
                                       x-2)
                                 (reduce (fn [acc x]
                                           (cons x
                                                 acc))
                                         (empty x-2)
                                         x-2))))


      "`next` preserves types of lists, returns vectors for other collections"

      ($.eval/true? ctx
                    ($.cell/* (let [-next (next x-2)]
                                (if (nil? -next)
                                  true
                                  (if (list? x-2)
                                    (list? -next)
                                    (vector? -next))))))


      "`next` is consistent with `first`, `second`, and `count`"

      ($.eval/true? ctx
                    ($.cell/* (loop [x-3 x-2]
                                (let [n-x-3 (count x-3)]
                                  (if (zero? n-x-3)
                                    true
                                    (let [x-3-next (next x-3)]
                                      (if (> n-x-3
                                             1)
                                        (if (and (= (count x-3-next)
                                                    (dec n-x-3))
                                                 (= (second x-3)
                                                    (first x-3-next)))
                                          (recur x-3-next)
                                          false)
                                        (if (nil? x-3-next)
                                          (recur x-3-next)
                                          false))))))))


      "`empty?` is consistent with `count?`"

      ($.eval/true? ctx
                    ($.cell/* (let [-count-pos? (> (count x-2)
                                                   0)
                                    -empty?     (empty? x-2)]
                                (if -empty?
                                  (not -count-pos?)
                                  -count-pos?))))


      "`empty?` is consistent with `empty`"

      ($.eval/true? ctx
                    ($.cell/* (empty? (empty x-2)))))))



(defn suite-main

  "Gathering [[suite-main-mono]] and [[suite-main-poly]]."

  [ctx]

  (mprop/and (suite-main-poly ctx)
             (suite-main-mono ctx)))



(defn suite-map-like

  "See checkpoint."

  [ctx]

  (mprop/check

    "Suite for operations specific to map-like types (ie. blob-map, hash-map, and nil-."

    (mprop/mult

      "Count has been updated as needed"

      ($.eval/true? ctx
                    ($.cell/* (= (count x-2)
                                 (+ (count x)
                                    (if (or (= x-2
                                               x)
                                            (contains-key? x
                                                           k))
                                      0
                                      1)))))


      ;; TODO.Failing because of: https://github.com/Convex-Dev/convex/issues/103
      ;;
      ;; "Using `merge` to rebuild map"
      ;; ($.eval/true? ctx
      ;;               ($.cell/* (= x-2
      ;;                            (merge (empty x-2)
      ;;                                   x-2))))
      ;; 
      ;; "Merging original with new = new"
      ;; ($.eval/true? ctx
      ;;               ($.cell/* (= x-2
      ;;                            (merge x
      ;;                                   x-2))))


      "`conj` is consistent with `assoc`"

      ($.eval/true? ctx
                    ($.cell/* (if (map? x)
                                (= x-2
                                   (conj x
                                         [k v]))
                                true)))


      "`into` is consistent with `assoc`"

      ($.eval/true? ctx
                    ($.cell/* (if (map? x)
                                (= x-2
                                   (into x
                                         [[k v]]))
                                true)))


      "All other key-values are preserved"

      ($.eval/true? ctx
                    ($.cell/* ($/every? (fn [k]
                                          (= (get x
                                                  k)
                                             (get x-2
                                                  k)
                                             (x-2 k)))
                                        (keys (dissoc x
                                                      k)))))


      "Using `into` to rebuild map"

      (let [ctx-2 ($.eval/ctx ctx
                              ($.cell/* (do
                                          (def -empty
                                               (empty x-2))
                                          (def as-list
                                               (into (list)
                                                     x-2)))))]
        (mprop/mult

          "On empty map"

          ($.eval/true? ctx-2
                        ($.cell/* (= x-2
                                     (into -empty
                                           x-2)
                                     (into -empty
                                           as-list))))


          "Using `into` on map with this very same map does not change anything"

          ($.eval/true? ctx-2
                        ($.cell/* (= x-2
                                     (into x-2
                                           x-2)
                                     (into x-2
                                           as-list)))))))))



(defn suite-map

  "Combining all suites that a map-like type must pass, for ease of use."

  [ctx]

  (mprop/and (suite-assoc ctx)
             (suite-main ctx)
             (suite-dissoc ctx)
             (suite-kv+ ctx)
             (suite-map-like ctx)))



(defn suite-sequential

  "See checkpoint."

  [ctx]

  (mprop/check

    "Specific to sequential collections"

    (mprop/mult

      "`contains-key?` with indices"

      ($.eval/true? ctx
                    ($.cell/* ($/every-index? contains-key?
                                              x-2)))


      "`get` is consistent with `nth`"

      ($.eval/true? ctx
                    ($.cell/* ($/every-index? (fn [x-2 i]
                                                (= (get x-2
                                                        i)
                                                   (x-2 i)
                                                   (nth x-2
                                                        i)))
                                              x-2)))


      "Rebuilding sequential using `assoc` and `apply`"

      ($.eval/true? ctx
                    ($.cell/* (= x-2
                                 (apply assoc
                                        x-2
                                        (loop [acc []
                                               idx (dec (count x-2))]
                                          (if (< idx
                                                 0)
                                            acc
                                            (recur (conj acc
                                                         idx
                                                         (get x-2
                                                              idx))
                                                   (dec idx)))))))))))


;;;;;;;;;; Generative tests for main suites


(mprop/deftest main-blob-map

  {:ratio-num 5}

  (TC.prop/for-all* [($.gen/blob-map ($.gen/blob)
                                     ($.gen/quoted $.gen/any))
                     ($.gen/blob)
                     ($.gen/quoted $.gen/any)]
                    (comp suite-map
                          ctx-assoc)))



(mprop/deftest main-map

  {:ratio-num 5}

  (TC.prop/for-all [m ($.gen/quoted $.gen/any-map)
                    k ($.gen/quoted $.gen/any)
                    v ($.gen/quoted $.gen/any)]
    (let [ctx (ctx-assoc m
                         k
                         v)]
      (mprop/and (suite-map ctx)
                 (suite-hash-map ctx)))))



(mprop/deftest main-nil

  {:ratio-num 5}

  (TC.prop/for-all* [$.gen/nothing
                     ($.gen/quoted $.gen/any)
                     ($.gen/quoted $.gen/any)]
                    (comp suite-map
                          ctx-assoc)))



(mprop/deftest main-sequential

  {:ratio-num 8}

  (TC.prop/for-all [[coll
                     i]   (TC.gen/bind (TC.gen/such-that (comp not
                                                               $.std/empty?)
                                                         (TC.gen/one-of [$.gen/any-list
                                                                         $.gen/any-vector])
                                                         100)
                                       (fn [coll]
                                         (TC.gen/tuple (TC.gen/return coll)
                                                       (TC.gen/choose 0
                                                                      (dec ($.std/count coll))))))
                    v     ($.gen/quoted $.gen/any)]
    (let [ctx (ctx-assoc ($.cell/quoted coll)
                         ($.cell/long i)
                         v)]
      (mprop/and (suite-assoc ctx)
                 (suite-main ctx)
                 (suite-sequential ctx)))))



(mprop/deftest main-set

  {:ratio-num 8}

  (TC.prop/for-all [s (TC.gen/such-that (comp not
                                              $.std/empty?)
                                        $.gen/any-set
                                        100)]
    (suite-main (let [s-2 ($.cell/quoted s)
                      v   (first s)]
                  (ctx-main s-2
                            s-2
                            ($.cell/quoted v)
                            ($.cell/* true))))))


;;;;;;;;;; `assoc`


(defn- -assoc-fail

  ;; Helper for evaluating a failing call to `assoc`.

  [x k v]

  (some? ($.eval/exception $.break/ctx
                           ($.cell/* (assoc (quote ~x)
                                            (quote ~k)
                                            (quote ~v))))))



(mprop/deftest assoc--fail

  {:ratio-num 10}

  (TC.prop/for-all* [(TC.gen/such-that (fn [x]
                                         (not (or ($.std/list? x)
                                                  ($.std/map? x)
                                                  (nil? x)
                                                  ($.std/set? x)
                                                  ($.std/vector? x))))
                                       $.gen/any
                                       100)
                     $.gen/any
                     $.gen/any]
                    -assoc-fail))



(mprop/deftest assoc--blob-map-fail

  {:ratio-num 10}

  (TC.prop/for-all* [($.gen/blob-map ($.gen/blob)
                                     $.gen/any)
                     (TC.gen/such-that (fn [x]
                                         (not (or ($.std/address? x)
                                                  ($.std/blob? x))))
                                       $.gen/any
                                       100)
                     $.gen/any]
                    -assoc-fail))



(mprop/deftest assoc--sequential-fail

  {:ratio-num 10}

  (TC.prop/for-all [[x
                     k] (TC.gen/let [x (TC.gen/one-of [$.gen/any-list
                                                       $.gen/any-vector])
                                     k (TC.gen/such-that #(not (and ($.std/number? %)
                                                                    ($.std/<= ($.cell/long 0)
                                                                              %
                                                                              ($.cell/long (dec ($.std/count x))))))
                                                         $.gen/any
                                                         100)]
                          [x k])
                    v   $.gen/any]
    (-assoc-fail x
                 k
                 v)))


;;;;;;;;;; `assoc-in`


(mprop/deftest assoc-in--fail-path

  ;; Trying to assoc using a path that is not a collection.

  {:ratio-num 10}

  (TC.prop/for-all [x    (TC.gen/one-of [$.gen/any-list
                                         $.gen/any-map
                                         $.gen/any-vector
                                         $.gen/nothing])
                    path (TC.gen/such-that (fn [x]
                                             (not (or (nil? x)
                                                      ($.std/list? x)
                                                      ($.std/vector? x))))
                                           $.gen/any
                                           100)
                    v    $.gen/any]
    (some? ($.eval/exception $.break/ctx
                             ($.cell/* (assoc-in (quote ~x)
                                                 (quote ~path)
                                                 (quote ~v)))))))



(mprop/deftest assoc-in--fail-type

  {:ratio-num 10}

  (TC.prop/for-all [x    (TC.gen/such-that (fn [x]
                                             (not (or (nil? x)
                                                      ($.std/list? x)
                                                      ($.std/map? x)
                                                      ($.std/set? x)
                                                      ($.std/vector? x))))
                                           $.gen/any
                                           100)
                    path (TC.gen/such-that (comp not
                                                 $.std/empty?)
                                           (TC.gen/one-of [$.gen/any-list
                                                           $.gen/any-vector])
                                           100)
                    v    $.gen/any]
    (some? ($.eval/exception $.break/ctx
                             ($.cell/* (assoc-in (quote ~x)
                                                 (quote ~path)
                                                 (quote ~v)))))))



#_(defn- -eval-assoc-in

  ;; Helper for writing and evaling the CVM code for passing `assoc-in` tests.

  [x path v]

  ($.eval/true? $.break/ctx
                ($.cell/* (= ~v
                             (let [x-2 (assoc-in ~x
                                                 ~path
                                                 ~v)]
                               (if (empty? ~path)
                                 x-2
                                 (get-in x-2
                                         ~path)))))))



#_(mprop/deftest assoc-in--map

  ;; TODO. Currently, empty path returns the value. Keep an eye on: https://github.com/Convex-Dev/convex/issues/96
  
  ;; TODO. Failing because of https://github.com/Convex-Dev/convex/issues/384

  {:ratio-num 10}

  (TC.prop/for-all [x    (TC.gen/one-of [($.gen/quoted $.gen/any-map)
                                         $.gen/nothing])
                    path ($.gen/quoted (TC.gen/one-of [$.gen/any-list
                                                       $.gen/any-vector]))
                    v    ($.gen/quoted $.gen/any)]
    (-eval-assoc-in x
                    path
                    v)))


;;;;;;;;;; Misc


(mprop/deftest mapcat--

  {:ratio-num 10}
  
  (TC.prop/for-all [coll ($.gen/quoted $.break.gen/collection)]
    (mprop/mult

      "Duplicating items"

      ($.eval/true? $.break/ctx
                    ($.cell/* (let [coll ~coll]
                                (= (vec (mapcat (fn [x]
                                                  [x x])
                                                coll))
                                   (reduce (fn [acc x]
                                             (conj acc
                                                   x
                                                   x))
                                           []
                                           coll)))))


      "Keeping items at even positions"

      ($.eval/true? $.break/ctx 
                    ($.cell/* (do
                                (def n-mapcat
                                     -1)
                                (def n-reduce
                                     -1)
                                (defn even? [x]
                                  (zero? (mod x
                                              2)))
                                (let [coll ~coll]
                                  (= (vec (mapcat (fn [x]
                                                    (def n-mapcat
                                                         (inc n-mapcat))
                                                    (when (even? n-mapcat)
                                                      [x]))
                                                  coll))
                                     (reduce (fn [acc x]
                                               (def n-reduce
                                                    (inc n-reduce))
                                               (if (even? n-reduce)
                                                 (conj acc
                                                       x)
                                                 acc))
                                             []
                                             coll)))))))))



(mprop/deftest mapping

  {:ratio-num 10}
  
  (TC.prop/for-all [coll $.break.gen/collection]
    (let [ctx ($.eval/ctx $.break/ctx
                          ($.cell/* (do
                                      (def coll
                                           (quote ~coll))
                                      (def vect
                                           (vec coll))
                                      (def modified
                                           (mapv vector
                                                 coll)))))]
      (mprop/mult

        "`for` to recreate collection as vector"

        ($.eval/true? ctx
                      ($.cell/* (= vect
                                   (for [x coll]
                                     x))))


        "`for` to modify collection"

        ($.eval/true? ctx
                      ($.cell/* (= modified
                                   (for [x coll]
                                     [x]))))


        "`mapv` with identity"

        ($.eval/true? ctx
                      ($.cell/* (= vect
                                   (mapv identity
                                         coll))))


        "`mapv` to modify collection"

        ($.eval/true? ctx
                      ($.cell/* (= modified
                                   (mapv vector
                                         coll))))


        "`mapcat`"

        (mprop/and (mprop/check
                     
                     "Modifies collection"

                     ($.eval/true? ctx
                                   ($.cell/* (= modified
                                                (vec (mapcat (fn [x]
                                                               [[x]])
                                                             coll))))))

                   (let [ctx-2 ($.eval/ctx ctx
                                           ($.cell/* (def -mapcat
                                                          (mapcat vector
                                                                     coll))))]
                     (if ($.std/list? coll)
                       (mprop/mult

                         "Produces a list"

                         ($.eval/true? ctx-2
                                       ($.cell/* (list? -mapcat)))

                         "List is recreated"

                         ($.eval/true? ctx-2
                                       ($.cell/* (= coll
                                                    -mapcat))))
                       (mprop/mult

                         "Produces a vector"

                         ($.eval/true? ctx-2
                                       ($.cell/* (vector? -mapcat)))


                         "Recreates collection as a vector"
                         ($.eval/true? ctx-2
                                       ($.cell/* (= vect
                                                    -mapcat)))))))))))



(mprop/deftest merge--

  {:ratio-num 4}

  (TC.prop/for-all [x+ ($.gen/vector (TC.gen/one-of [($.gen/quoted $.gen/any-map)
                                                     $.gen/nothing])
                                     0
                                     16)]
    (let [ctx ($.eval/ctx $.break/ctx
                          ($.cell/* (do
                                      (def arg+
                                           ~x+)
                                      (def merge-
                                           (merge ~@x+)))))]
      (mprop/mult

        "Count of merge cannot be bigger than all involved key-values"

        ($.eval/true? ctx
                      ($.cell/* (<= (count merge-)
                                    (reduce (fn [acc arg]
                                              (+ acc
                                                 (count arg)))
                                            0
                                            arg+))))


        "All key-values in merged result must be in at least one input"

        ($.eval/true? ctx
                      ($.cell/* ($/every? (fn [[k v]]
                                            ($/some (fn [arg]
                                                      (and (= v
                                                              (get arg
                                                                   k))
                                                           (if (nil? arg)
                                                             true
                                                             (= v
                                                                (arg k)))))
                                                    arg+))
                                          merge-)))))))



(mprop/deftest reduce--

  {:ratio-num 10}

  (TC.prop/for-all [percent $.break.gen/percent
                    x       (TC.gen/such-that (comp not
                                                    $.std/empty?)
                                              $.break.gen/collection
                                              100)]
    ($.eval/true? $.break/ctx
                  ($.cell/* (let [x (quote ~x)
                                  v (nth x
                                         (long (floor (* ~percent
                                                         (dec (count x))))))]
                              (= v
                                 (reduce (fn [acc item]
                                           (if (= item
                                                  v)
                                             (reduced item)
                                             acc))
                                         :convex-sentinel
                                         x)))))))


;;;;;;;;;; Negative tests


(mprop/deftest blob-map--err-cast

  (TC.prop/for-all [arg+ ($.break.gen/mix-one-in ($.gen/tuple (TC.gen/such-that (fn [x]
                                                                                  (not (or ($.std/address? x)
                                                                                           ($.std/blob x))))
                                                                                $.gen/any
                                                                                100)
                                                              $.gen/any)
                                                 ($.break.gen/kv+ (TC.gen/one-of [(TC.gen/one-of [$.gen/address
                                                                                                  ($.gen/blob)])
                                                                                  $.gen/any])
                                                                  $.gen/any))]
    (= ($.cell/code-std* :ARGUMENT)
       ($.eval/exception-code $.break/ctx
                              ($.cell/* (blob-map ~@(mapcat (partial map
                                                                     $.cell/quoted)
                                                            arg+)))))))



(mprop/deftest concat--err-cast

  (TC.prop/for-all [arg+ ($.break.gen/mix-one-in (TC.gen/such-that (fn [x]
                                                                     (not (or (nil? x)
                                                                              ($.std/list? x)
                                                                              ($.std/map? x)
                                                                              ($.std/set? x)
                                                                              ($.std/vector? x))))
                                                                   $.gen/any
                                                                   100)
                                                 ($.gen/vector $.gen/any))]
    (= ($.cell/code-std* :CAST)
       ($.eval/exception-code $.break/ctx
                              ($.cell/* (concat ~@(map $.cell/quoted
                                                       arg+)))))))



(mprop/deftest conj--err-cast

  ;; TODO. Blob-maps with non-blob keys.

  {:ratio-num 10}

  (TC.prop/for-all [arg+ ($.gen/vector ($.gen/quoted $.gen/any)
                                       0
                                       6)
                    x    ($.gen/quoted $.break.gen/not-collection)]
    (= ($.cell/code-std* :CAST)
       ($.eval/exception-code $.break/ctx
                              ($.cell/* (conj ~x
                                              ~@arg+))))))



(mprop/deftest cons--err-cast

  {:ratio-num 20}

  (TC.prop/for-all [not-coll $.break.gen/not-collection
                    x        $.gen/any]
    (= ($.cell/code-std* :CAST)
       ($.eval/exception-code $.break/ctx
                              ($.cell/* (cons (quote ~x)
                                              (quote ~not-coll)))))))



(mprop/deftest contains-key?--err-cast

  {:ratio-num 15}

  (TC.prop/for-all [x $.break.gen/not-collection
                    k $.gen/any]
    (= ($.cell/code-std* :CAST)
       ($.eval/exception-code $.break/ctx
                              ($.cell/* (contains-key? (quote ~x)
                                                       (quote ~k)))))))
