(ns convex.run

  "Core namespace implementing a Convex Lisp runner.

   The purpose of a runner is to provide a convenient framework for evaluating a Convex Lisp main file and
   related dependencies.


   ENV
   ===

   Those utilities are built using the 'sync' project and the 'watch' project. Just as in those project, a runner
   operates over an \"environment\" map. See [[init]].

   In a main file, each form is evaluated as a transaction and gradually modifies an environment.


   EVALUATION
   ==========

   [[eval]] serves to evaluate a given string as if it was a main file ; [[load]] reads and executes a main file ;
   [[watch]] behaves like [[load]] but provides a live-reloading experience.


   SPECIAL REQUESTS
   ================

   A runner can do useful side-effects on-demand called \"special requests\", such as outputting a value. Those special
   requests are nothing more than vector that the runner interprets at runtime.
  
    See the [[convex.run.sreq]] namespace.

  
   DYNAMIC VALUES AND HELP
   =======================

   A runner maintains a set of dynamic values that users can access in the address aliased by default as `help`. For more
   information, use [[eval]] to evaluate `(help/about help)`.


   OUTPUT
   ======

   Any value that is requested to be outputted (by the `(sreq/out ...)` special request) goes through the output hook.
   See hook section.


   ERRORS
   ======

   When a CVM exception or any other error occurs, it is being signaled using the error hook. See hook section.

   Errors are usually signaled to the user with [[convex.run.err/signal]] and [[convex.run.err/fatal]] in extreme cases.


   HOOKS
   =====

   Hooks are functions or transactions executed by the runner at key moments.

   They are specified in the environment:

   | When | What | Default behavior |
   |---|---|---|
   | `:convex.run.hook/end` | A function `env` -> `env` called after all transactions have been processed | Nothing |
   | `:convex.run.hook/error` | A function `env` -> `env` called when an error occurs | Passing the error to the output hook |
   | `:convex.run.hook/out` | A function `env`, `value` -> `env` called when a value must be outputted | Printing to terminal using `println` |
   | `:convex.run.hook/result | A transaction executed after every transaction provided by the user | Nothing |


   CLI APP
   =======

   The CLI Convex Lisp Runner is a light layer built on top of this project. It is not much more than a CLI interface with
   description.

   See 'app/run' project."

  ;; TODO. Improve reader error reporting when Antlr gets stabilized.

  {:author "Adam Helinski"}

  (:import (convex.core.data AList)
           (convex.core.lang Symbols)
           (java.io File)
           (java.nio.file NoSuchFileException))
  (:refer-clojure :exclude [eval
                            load])
  (:require [clojure.java.io]
            [convex.data      :as $.data]
            [convex.read      :as $.read]
            [convex.cvm       :as $.cvm]
            [convex.run.ctx   :as $.run.ctx]
            [convex.run.err   :as $.run.err]
            [convex.run.exec  :as $.run.exec]
            [convex.run.kw    :as $.run.kw]
            [convex.run.sym   :as $.run.sym]
            [convex.run.sreq]
            [convex.sync      :as $.sync]
            [convex.watch     :as $.watch]))


(declare sym->dep)


;;;;;;;;;; Computing dependencies


(defn assoc-trx+

  "Associates the given `trx+` on `env` and computes dependencies using [[sym->dep]]."

  [env trx+]

  (-> env
      (assoc :convex.run/trx+
             trx+)
      sym->dep))



(defn sym->dep

  "Used when a main file is read. Looks at the first transaction under `:convex.run/trx+` and computes
   dependencies if it is a `(sreq/dep ...)` form."

  [env]

  (or (when-some [trx (first (env :convex.run/trx+))]
        (when ($.data/list? trx)
          (let [^AList form (first trx)]
            (when (and ($.data/list? form)
                       (= (count form)
                          3)
                       (= (.get form
                                0)
                          Symbols/LOOKUP)
                       (let [addr (.get form
                                        1)]
                         (or (= addr
                                $.run.sym/sreq)
                             (= addr
                                $.run.ctx/addr-sreq)))
                       (= (.get form
                                2)
                          $.run.sym/dep))
              (let [sym->dep (second trx)]
                (if ($.data/map? sym->dep)
                  (reduce (fn [env-2 [cvm-sym cvm-str-dep]]
                            (if-some [err-message (cond
                                                    (not ($.data/string? cvm-str-dep)) (str "Dependency must be a path to a file (string), not: "
                                                                                            cvm-str-dep)
                                                    (not ($.data/symbol? cvm-sym))     (format "Dependency '%s' must be defined under a symbol, not: %s"
                                                                                               cvm-str-dep
                                                                                               cvm-sym))]
                              (reduced ($.run.err/signal env
                                                         (-> ($.data/error ($.cvm/code-std* :CAST)
                                                                           ($.data/string err-message))
                                                             ($.run.err/assoc-phase $.run.kw/dep))))
                              (assoc-in env-2
                                        [:convex.run/sym->dep
                                         (str cvm-sym)]
                                        (-> cvm-str-dep
                                            str
                                            File.
                                            .getCanonicalPath))))
                          (update env
                                  :convex.run/trx+
                                  rest)
                          sym->dep)
                  ($.run.err/signal env
                                    (-> ($.data/error ($.cvm/code-std* :CAST)
                                                      ($.data/string "Dependencies must but a map of 'symbol' -> 'file path (string)'"))
                                        ($.run.err/assoc-phase $.run.kw/dep)))))))))
      env))
          

;;;;;;;;;; Error handling aspects


(defn check-err-sync

  "Signals a sync error if needed (see [[convex.run.err/sync]]).
  
   Returns nil otherwise, meaning no sync error has been detected and handled."

  [env]

  (when-some [err (env :convex.sync/error)]
    ($.run.err/signal env
                      ($.run.err/sync err))))


;;;;;;;;;; Initializing an environment


(defn init

  "Initializes important functions and values in the given `env`, using defaults when relevant.

   Must be used prior to other preparatory functions such as [[main-file]].
  
   Operates over:
  
   | Key | Action |
   |---|---|
   | `:convex.run/path` | If present, ensures the path to the main file is canonical |
   | `:convex.run.hook/end` | Ensures a default end hook, see namespace description |
   | `:convex.run.hook/error` | Ensures a default error hook, see namespace description |
   | `:convex.run.hook/out` | Ensures a default output hook, see namespace description |
   | `:convex.sync/cx-base | Ensures a default base context, see [[convex.run.ctx/base]] |"

  [env]

  (-> env
      (update :convex.run/path
              (fn [^String path]
                (when path
                  (.getCanonicalPath (File. path)))))
      (update :convex.run.hook/end
              #(or %
                   identity))
      (update :convex.run.hook/error
              #(or %
                   (fn [env-2]
                     ((env-2 :convex.run.hook/out)
                      env-2
                      (env-2 :convex.run/error)))))
      (update :convex.run.hook/out
              #(or %
                   (fn [env-2 x]
                     (when x
                       (-> x
                           str
                           tap>))
                     env-2)))
      (update :convex.sync/ctx-base
              #(or %
                   $.run.ctx/base))
      $.run.ctx/init))



(defn main-file

  "Reads and parses the main file under `:convex.run/path` and prepares transactions using [[assoc-trx+]]."

  [env]

  (let [[err
         form+] (try

                  [nil
                   (vec ($.read/file (env :convex.run/path)))]

                  (catch NoSuchFileException _ex
                    [($.run.err/main-src-access)
                     nil])

                  (catch Throwable _ex
                    [($.run.err/main-src "Main file cannot be read and parsed as Convex Lisp")
                     nil]))]
    (if err
      ($.run.err/signal env
                        err)
      (assoc-trx+ env
                  form+))))


;;;;;;;;;; Executing source once


(defn once

  "Assuming `env` has been properly initialized (eg. see [[init]]) and no error occured, executes transactions once to
   completion.

   Takes care of preparing dependencies.

   Built on top of the 'sync' project (see the [[convex.sync]] namespace).

   Used by [[eval]] and [[load]]."

  [env]

  (if (env :convex.run/error)
    env
    (if-some [sym->dep' (env :convex.run/sym->dep)]
      (let [env-2    (merge env
                            ($.sync/disk ($.cvm/fork (env :convex.sync/ctx-base))
                                         sym->dep'))]
        (or (check-err-sync env-2)
            ($.run.exec/cycle env-2)))
      (-> env
          (assoc :convex.sync/ctx
                 ($.cvm/fork (env :convex.sync/ctx-base)))
          $.run.exec/cycle))))


;;;;;;;;;; Evaluating a given source string


(defn eval

  "Like [[load]] but evaluates a given string as opposed to a file."


  ([string]

   (eval nil
         string))


  ([env ^String string]

   (let [env-2   (init env)
         [ex
          form+] (try
                   [nil
                    (vec ($.read/string string))]
                   (catch Throwable ex
                     [ex
                      nil]))]
     (if ex
       ($.run.err/signal env-2
                         ($.run.err/main-src "Given string cannot be parsed as Convex Lisp"))
       (-> env-2
           (assoc-trx+ form+)
           once)))))


;;;;;;;;;; Loading a main file and its dependencies


(defn load

  "Reads and executes the main file under `path` by using [[once]]."


  ([path]

   (load nil
         path))


  ([env path]

   (-> env
       (assoc :convex.run/path
              path)
       init
       main-file
       once)))


;;;;;;;;;; Watch files

(def foo nil)

(let [-restart (fn [a*env env]
                 ;;
                 ;; Restart watcher whenever a change in dependency is detected.
                 ;;
                 ($.watch/-stop env)
                 ($.watch/-start a*env
                                 (dissoc env
                                         :convex.run/error
                                         :convex.run/i-trx
                                         :convex.run/juice-last
                                         :convex.sync/ctx
                                         :convex.sync/input+
                                         :convex.sync/input->code
                                         :convex.sync/input->cvm-sym
                                         :convex.watch/error
                                         :convex.watch/watcher)))]

  (defn watch

    "Executes a main file just like [[load]] but provides a live-reloading experience.

     Whenever the main file or any of its dependencies changes, relevant assets are reloaded and
     the whole is rexecuted anew.
   
     Built on top of the 'watch' project (see the [[convex.watch]] namespace)."

    ;; Implementation of the `:convex.watch/on-change` is a tad convoluted because it handles various kind of errors
    ;; and dependency changes.
    ;;
    ;; Essentially, it first checks for errors at the level of the watcher, then errors at the level of the syncing,
    ;; then checks if dependencies changed, and ultimately run transactions unless it waits for a correction in dependencies
    ;; given that an error occured at that level (eg. requesting an nonexistent file).

    ([path]

     (watch nil
            path))


    ([env ^String path]

     (let [a*env ($.watch/init (-> env
                                   (assoc :convex.run/path
                                          path)
                                   init
                                   (assoc :convex.watch/extra+
                                          #{(.getCanonicalPath (File. path))})))]
       (send a*env
             (fn [env]
               (assoc env
                      :convex.watch/on-change
                      (fn on-change [{:as      env-2
                                      dep-old+ :convex.run/dep+}]
                        (let [env-3 (dissoc env-2
                                            :convex.run/error)]
                          (or ;;
                              ;; Handles watcher error if any.
                              ;;
                              (when-some [[etype
                                           arg]  (env-3 :convex.watch/error)]
                                (case etype
                                  :not-found (if (= arg
                                                    (first (env-3 :convex.watch/extra+)))
                                               ($.run.err/signal env-3
                                                                 ($.run.err/main-src-access))
                                               ;;
                                               ;; Dependency is missing, restart watching only main file for retrying on new changes.
                                               ;; A "dep-lock" is used so that a new watcher with the same failing dependencies is not restarted right away.
                                               ;;
                                               (-restart a*env
                                                         (-> env-3
                                                             (assoc :convex.run/dep-lock
                                                                    dep-old+)
                                                             (dissoc :convex.run/dep+
                                                                     :convex.watch/sym->dep)
                                                             ($.run.err/signal ($.run.err/sync :load
                                                                                               {arg [:not-found]})))))
                                  :unknown   ($.run.err/signal env
                                                               ($.run.err/watcher-setup))))
                              ;;
                              ;; Handles sync error if any.
                              ;;
                              (check-err-sync env-3)
                              ;;
                              ;; No significant errors were detected so try evaluation.
                              ;;
                              (let [dep-lock (env-3 :convex.run/dep-lock)]
                                (if (or dep-lock
                                        (nil? dep-old+)
                                        (seq (env-3 :convex.watch/extra->change)))
                                  (let [env-4 (main-file env-3)]
                                    (if (env-4 :convex.run/error)
                                      env-4
                                      (let [sym->dep' (env-4 :convex.run/sym->dep)
                                            dep-new+  (set (vals sym->dep'))
                                            env-5     (-> env-4
                                                          (assoc :convex.run/dep+       dep-new+
                                                                 :convex.watch/sym->dep sym->dep')
                                                          (dissoc :convex.run/sym->dep))]
                                        (if (env-5 :convex.run/error)
                                          env-5
                                          (if (= (not-empty dep-new+)
                                                 (not-empty dep-old+))
                                            (-> env-5
                                                (dissoc :convex.watch/dep-lock
                                                        :convex.watch/extra->change)
                                                $.sync/patch
                                                $.sync/eval
                                                $.run.exec/cycle)
                                            (if (= dep-new+
                                                   dep-lock)
                                              (dissoc env-5
                                                      :convex.run/dep+
                                                      :convex.run/dep-lock
                                                      :convex.run/extra->change)
                                              (-restart a*env
                                                        (dissoc env-5
                                                                :convex.run/dep-lock))))))))
                                  ($.run.exec/cycle env-3)))))))))
       ($.watch/start a*env)
       a*env))))


;;;;;;;;;; During dev


(comment


  (eval "(help/about sreq)")



  (load "project/run/src/cvx/dev/convex/run/dev.cvx")



  (def a*env
       (watch "project/run/src/cvx/dev/convex/run/dev.cvx"))

  ($.watch/stop a*env)

  (clojure.pprint/pprint (dissoc @a*env
                                 :convex.sync/input->code))

  (agent-error a*env)


  )
