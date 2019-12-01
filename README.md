# Clover

A Clojure Socket REPL package for Visual Studio Code and Visual Studio Codium

## Features

For now, it is possible to connect on a Clojure (and Clojure-ish) socket REPL and evaluate forms. You can load the current file, there's autocomplete, and support for multiple ClojureScript REPLs (with the notable exception being that Figwheel is missing - it only supports Shadow-CLJS and "pure ClojureScript socket REPLs" like Lumo, Plank, `clj` with some additional code, etc).

For now, the following Clojure implementations were tested and are known to work:

* Clojure with lein/boot/clj
* ClojureScript with Shadow-CLJS (multi-target)
* ClojureScript with `clj` and the command `clj -J-Dclojure.server.browser="{:port 4444 :accept cljs.server.browser/repl}"`
* ClojureCLR
* Lumo
* Plank
* Joker
* Babashka

## Disclaimer:
This plug-in should be considered "alpha state". Its still in the beginning, and the only reason it works for evaluation, autocomplete, and supports lots of Clojure implementations is because it re-uses most of the code from Chlorine.

### Known problems:
Autocomplete can't find some suggestions. This could be a limitation of VSCode (that don't detect, for example, variables like `*clojure-version*` because of their special characters on the beginning)

There's also some synchronization problems, so sometimes a restart of VSCode is necessary.

## Related Projects:
* [Chlorine](https://github.com/mauricioszabo/atom-chlorine)
* [REPL Tooling](https://github.com/mauricioszabo/repl-tooling)
