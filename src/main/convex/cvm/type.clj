(ns convex.cvm.type

  "Constructing CVM objects."

  {:author "Adam Helinski"}

  (:import (convex.core.data Address
                             AList
                             AMap
                             ASet
                             AString
                             AVector
                             Blob
                             Keyword
                             Lists
                             MapEntry
                             Maps
                             Sets
                             Strings
                             Symbol
                             Vectors)
           (convex.core.data.prim CVMBool
                                  CVMByte
                                  CVMChar
                                  CVMDouble
                                  CVMLong)
           (java.util Collection
                      List))
  (:refer-clojure :exclude [boolean
                            byte
                            char
                            double
                            keyword
                            list
                            long
                            map
                            set
                            symbol
                            vector])
  (:require [clojure.core]))


(set! *warn-on-reflection*
      true)


(declare vector)


;;;;;;;;;;


(defn address

  "Creates a CVM address from a long."

  ^Address

  [long]

  (Address/create (clojure.core/long long)))



(defn blob

  "Creates a CVM blob from a byte array."

  ^Blob

  [byte-array]

  (Blob/create byte-array))



(defn boolean

  "Creates a CVM boolean given a falsy or truthy value."

  ^CVMBool
  
  [x]

  (CVMBool/create (clojure.core/boolean x)))



(defn byte

  "Creates a CVM byte from a value between 0 and 255 inclusive."

  ^CVMByte

  [b]

  (CVMByte/create b))



(defn char

  "Creates a CVM character from a regular characer."

  ^CVMChar

  [ch]

  (CVMChar/create (clojure.core/long ch)))



(defn double

  "Creates a CVM double."

  ^CVMDouble

  [x]

  (CVMDouble/create x))



(defn keyword

  "Creates a CVM keyword from a Clojure keyword."

  ^Keyword

  [kw]

  (Keyword/create (name kw)))



(defn list

  "Creates a CVM list from a collection of CVM items."

  ^AList

  [x]

  (Lists/create x))



(defn long

  "Creates a CVM long."

  ^CVMLong

  [x]

  (CVMLong/create x))


(defn map

  "Creates a CVM map from a collection of `[key value]`."

  ^AMap

  [x]

  (Maps/create ^List (clojure.core/map (fn [[k v]]
                                         (MapEntry/create k
                                                          v))
                                       x)))



(defn set

  "Creates a CVM set from a collection of CVM items."

  ^ASet

  [x]

  (Sets/create (vector x)))



(defn string

  "Creates a CVM string from a regular string."

  ^AString

  [string]

  (Strings/create string))



(defn symbol

  "Creates a CVM symbol either from:
  
   - Clojure symbol where namespace and name matters
   - Namespace (either long representing an address or a Clojure symbol) and name (Clojure symbol)
     given separately"


  (^Symbol [sym]

   (let [name- (name sym)]
     (if-some [ns- (namespace sym)]
       (Symbol/create (Symbol/create ns-)
                      (string name-))
       (Symbol/create ^String name-))))


  (^Symbol [namespace name]

   (let [name-2 (string (clojure.core/name name))]
     (if (int? namespace)
       (Symbol/create ^Address (address namespace)
                      name-2)
       (Symbol/create (Symbol/create ^String namespace)
                      name-2)))))



(defn vector

  "Creates a CVM vector from a collection of CVM items."

  ^AVector

  [^Collection x]

  (Vectors/create x))