# Any aspect of the Convex stack

This monorepo hosts a variety of applications and libraries written in Clojure providing access to all aspects of the [Convex stack](https://github.com/Convex-Dev/convex)
with additional capabilities à la carte.

Since some key aspects of [Convex](https://convex.world/) have been modeled on Clojure constructs. Hence, there is no surprise in realizing that both form a unique and perfect
match. Even without having any interest in blockchain, it is still worth exploring features offered by this repository such as the immutable [Etch database](./project/db).

Overview of main folders in the [./project](./project) directory:

| Project | Purpose |
|---|---|
| [`:project/app.fuzz`](./project/app/fuzz) | CLI multicore fuzzy tester, generates and tests random Convex Lisp forms | 
| [`:project/break`](./project/break) | Advanced generative test suite for the CVM ; novel smart contract testing |
| [`:project/clojurify`](./project/clojurify) | Convex <-> Clojure data conversions, quick evaluation, useful `test.check` generators |
| [`:project/crypto`](./project/crypto) | Key pair creation and management for digital signing |
| [`:project/cvm`](./project/cvm) | Convex types, reading Convex Lisp code, execution |
| [`:project/dapp`](./project/db) | Bundle of useful libraries for building decentralized applications |
| [`:project/db`](./project/db) | Create and handle immutable Etch databases crafted for Convex types |
| [`:project/net`](./project/net) | Convex network stack (running peers and using the binary client) |
| [`:project/recipe`](./project/recipe) | Recipes for understanding Convex and writing dApps |
| [`:project/run`](./project/run) | Convex Lisp Runner and REPL, advanced terminal environment |

[Recipes](./project/recipe) are key to reach a high-level understanding of Convex and understanding how to use all those utilities to write decentralized applications.
They provide an easy way for experimenting key concepts by cloning/forking this repository.


## Releases

Released applications and libraries:

| Project | Library | Cljdoc | Download |
|---|---|---|---|
| [`:project/clojurify`](./project/clojurify) | [![Clojars](https://img.shields.io/clojars/v/world.convex/clojurify.clj.svg)](https://clojars.org/world.convex/clojurify.clj) | [![cljdoc](https://cljdoc.org/badge/world.convex/clojurify.clj)](https://cljdoc.org/d/world.convex/clojurify.clj/CURRENT) | / |
| [`:project/crypto`](./project/crypto) | [![Clojars](https://img.shields.io/clojars/v/world.convex/crypto.clj.svg)](https://clojars.org/world.convex/crypto.clj) | [![cljdoc](https://cljdoc.org/badge/world.convex/crypto.clj)](https://cljdoc.org/d/world.convex/crypto.clj/CURRENT) | / |
| [`:project/cvm`](./project/cvm) | [![Clojars](https://img.shields.io/clojars/v/world.convex/cvm.clj.svg)](https://clojars.org/world.convex/cvm.clj) | [![cljdoc](https://cljdoc.org/badge/world.convex/cvm.clj)](https://cljdoc.org/d/world.convex/cvm.clj/CURRENT) | / |
| [`:project/dapp`](./project/dapp) | [![Clojars](https://img.shields.io/clojars/v/world.convex/dapp.clj.svg)](https://clojars.org/world.convex/dapp.clj) | [![cljdoc](https://cljdoc.org/badge/world.convex/dapp.clj)](https://cljdoc.org/d/world.convex/dapp.clj/CURRENT) | / |
| [`:project/db`](./project/db) | [![Clojars](https://img.shields.io/clojars/v/world.convex/db.clj.svg)](https://clojars.org/world.convex/db.clj) | [![cljdoc](https://cljdoc.org/badge/world.convex/db.clj)](https://cljdoc.org/d/world.convex/db.clj/CURRENT) | / |
| [`:project/net`](./project/net) | [![Clojars](https://img.shields.io/clojars/v/world.convex/net.clj.svg)](https://clojars.org/world.convex/net.clj) | [![cljdoc](https://cljdoc.org/badge/world.convex/net.clj)](https://cljdoc.org/d/world.convex/net.clj/CURRENT) | / |
| [`:project/run`](./project/run) | [![Clojars](https://img.shields.io/clojars/v/world.convex/run.clj.svg)](https://clojars.org/world.convex/run.clj) | [![cljdoc](https://cljdoc.org/badge/world.convex/run.clj)](https://cljdoc.org/d/world.convex/run.clj/CURRENT) | [CVX runner](https://github.com/Convex-Dev/convex.cljc/releases/tag/run%2F0.0.0-alpha2) |


## Structure

Each project follows a predictable structure:

- Project details are exposed in a dedicated README
- All source is located under the `./src` directory of each project or subproject
- Source is subdivided by language (eg. `clj`, `cvx`) and then by purpose (eg. `main`, `test`)
- All scripts and tasks are located and executed at the root of this repository


## Conventions

The following conventions are enforced in READMEs and source files:

- Namespaces shorten `convex` into `$`: `convex.cvm` -> `$.cvm`
- Symbols referring to collections are pluralized with `+` at the end: `items` -> `item+`


## Maintenance

Following sections are only useful for managing this repository or experimenting with a clone/fork.


### Clojure Deps

This repository rely on the [Clojure command line tools](https://clojure.org/guides/getting_started). Familiarity with
[Clojure Deps](https://clojure.org/guides/deps_and_cli) is required.

Alias names follow the convention established in [Maestro](https://github.com/helins/maestro.clj). For instance, see project aliases in table above.


### Babashka and tasks

All scripting is done using [Babashka](https://book.babashka.org/), a fast Clojure interpreter that comes with a powerful task runner.
Follow this [simple installation process](https://book.babashka.org/#_installation).

All tasks are written in [./bb.edn](./bb.edn) and can by listed by running in your shell:

```bash
bb tasks
```

Printed list shows all current tasks available for managing this repository: starting dev mode, running some tests, compiling, etc.

A task typically requires one or several aliases from `deps.edn` and sometimes CLI arguments.

For instance:

```bash
# Starts project 'CVM' in dev mode which is an alias in `deps.edn` + personal `:nrepl` alias 
$ bb dev :project/cvm:nrepl

# Testings all namespaces for project 'break' and dependencies
$ bb test :project/break
```


## Dev

Following directory structure, each project typically has a `dev_templ.clj` file in its Clojure dev files which requires useful namespaces.
This file can be copied in the same directory to `dev.clj` for hacking and trying thing out. Those `dev.clj` files are effectively private and will
not appear in this repository.

For example, see [`:project/all` dev directory](./project/all/src/clj/dev/convex/all).


## License

Copyright © 2021 Adam Helinski, the Convex Foundation, and contributors

Licensed under the Apache License, Version 2.0
