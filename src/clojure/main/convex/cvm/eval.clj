(ns convex.cvm.eval

  "Shortcuts for evaluating Convex Lisp code, useful for development and testing.

   Takes a form (Clojure data expressing Convex Lisp code) and evaluates to some result returned as Clojure data.
  
   This namespace offers many flavors of how exceptions and results are handled and returned.

   Primarily useful for dev and testing.

   Given context is always forked, meaning the argument is left intact. See [[convex.cvm/fork]]."

  {:author "Adam Helinski"}

  (:require [convex.cvm  :as $.cvm]
            [convex.lisp :as $.lisp]))


(declare result)


;;;;;;;;;;


(def ^:dynamic *ctx-default*

  "Holder for a default context which can set at start-up or dynamically with Clojure's `binding`.

   For preparing one at start-up, set the `CVM_CTX` environment variable to a qualified symbol which points to a
   context producing function. This function is called without argument when this namespace is required.

   For instance, [[convex.cvm/ctx]] creates a basic context. Hence, if it does not need further preparation:

   ```bash
   $ env CVM_CTX='convex.cvm/ctx'  clojure ...
   ```"

  (when-some [ctx-string (not-empty (System/getenv "CVM_CTX"))]
    (try
      ((requiring-resolve (symbol ctx-string)))
      (catch Throwable e
        (throw (ex-info (str "While trying to produce a default CVM context with: "
                             ctx-string)
                        {::env ctx-string}
                        e))))))


;;;;;;;;;;



(defn ctx

  "Evaluates the given `form` and returns `ctx`.
  
   Uses [[convex.cvm/*ctx-default*]] if `ctx` is not provided."


  ([form]

   (convex.cvm.eval/ctx *ctx-default*
                        form))


  ([ctx form]

   ($.cvm/eval ($.cvm/fork ctx)
               (-> form
                   $.lisp/src
                   $.cvm/read))))


;;;;;;;;;;


(defn code

  "Like [[exception]] but returns an exception only if it matches the given code.
  
   See [[convex.cvm/code*]]."

  
  ([code form]

   (convex.cvm.eval/code *ctx-default*
                         code
                         form))


  ([ctx code form]

   (->> (convex.cvm.eval/ctx ctx
                             form)
        ($.cvm/exception code)
        $.cvm/as-clojure)))



(defn code?

  "Like [[code]] but returns a boolean indicating if an exception of the given `code occured."

  
  ([code form]

   (code? *ctx-default*
          code
          form))


  ([ctx code form]

   (->> (convex.cvm.eval/ctx ctx
                             form)
        ($.cvm/exception? code))))



(defn exception

  "Like [[ctx]] but returns the current exception or nil if there is none."


  ([form]

   (exception *ctx-default*
              form))


  ([ctx form]

   (-> (convex.cvm.eval/ctx ctx
                            form)
       $.cvm/exception
       $.cvm/as-clojure)))



(defn exception?

  "Like [[ctx]] but returns a boolean indicating if an exception occured."


  ([form]

   (exception? *ctx-default*
               form))


  ([ctx form]

   (-> (convex.cvm.eval/ctx ctx
                            form)
       $.cvm/exception?)))



(defn like-clojure?

  "Returns true if applying `arg+` to `form` on the CVM produces the exact same result as
  `(apply f arg+)`
  
   Alternatively, a unique `form` can be given for both execution environments. In that case,
   the form is processed in Clojure via `eval`.
  
   ```clojure
   (like-clojure? '(+ 2 2))

   (like-clojure? '+
                  +
                  [2 2)
   ```"


  ([form]

   (like-clojure? *ctx-default*
                  form))


  ([ctx form]

   ($.lisp/= (eval form)
             (result ctx
                     form)))


  ([form f arg+]

   (like-clojure? *ctx-default*
                  form
                  f
                  arg+))


  ([ctx form f arg+]

   ($.lisp/= (apply f
                    arg+)
             (result ctx
                     (list* form
                            arg+)))))



(let [src     (fn [form]
                ($.lisp/src ($.lisp/templ* (log {:form   (quote ~form)
                                                 :return ~form}))))
      process (fn [ctx]
                (-> ctx
                    $.cvm/log
                    $.cvm/as-clojure))]
  (defn log

    "Like [[ctx]] but returns the context log as Clojure data structure, where the last entry for the executing
     address is a map containing the given `form` as well as its return value.
    
     Useful for debugging, akin to using `println` with Clojure."


    ([form]

     (-> (convex.cvm.eval/ctx form)
         process))


    ([ctx form]

     (-> (convex.cvm.eval/ctx ctx
                              form)
         process))))



(defn result

  "Like [[ctx]] but returns the result as Clojure data."


  ([form]

   (result *ctx-default*
           form))


  ([ctx form]

   (-> (convex.cvm.eval/ctx ctx
                            form)
       $.cvm/result
       $.cvm/as-clojure)))



(defn value

  "Like [[ctx]] but returns either an [[exception]] or a [[result]]."
  

  ([form]

   (value *ctx-default*
          form))


  ([ctx form]

   (let [ctx-2     (convex.cvm.eval/ctx ctx
                                        form)
         exception ($.cvm/exception ctx-2)]
     (-> (if (nil? exception)
           ($.cvm/result ctx-2)
           exception)
         $.cvm/as-clojure))))
