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

## Usage:
Fire up a clojure REPL with Socket REPL support. With `shadow-cljs`, when you `watch` some build ID it'll give you a port for nREPL and Socket REPL. With `lein`, invoke it in a folder where you have `project.clj` and you can use `JVM_OPTS` environment variable like:

```bash
JVM_OPTS='-Dclojure.server.myrepl={:port,5555,:accept,clojure.core.server/repl}' lein repl
```

On Shadow-CLJS' newer versions, when you start a build with `shadow-cljs watch <some-id>`, it doesn't shows the Socket REPL port on the console, but it does create a file with the port number on `.shadow-cljs/socket-repl.port`. You can read that file to see the port number (Clover currently uses this file to mark the port as default).

With `clj`, you can run the following from any folder:

```bash
clj -J'-Dclojure.server.repl={:port,5555,:accept,clojure.core.server/repl}'
```

Or have it in `:aliases` in `deps.edn`. (For an example with port 50505 see https://github.com/seancorfield/dot-clojure/blob/master/deps.edn, then you can run `clj -A:socket`.)

Then, you connect Clover with the port using the command _Connect Clojure Socket REPL_. This package works with lumo too, but you'll need to run _Connect ClojureScript Socket REPL_.

When connected, it'll try to load `compliment` (for autocomplete, falling back to a "simpler" autocomplete if not present). Then you can evaluate code on it.

## Keybindings
To avoid conflicts, this plug-in does not register any keybindings. You can define your own on `keybindings.json`. One possible suggestion is:

```json
    {
        "key": "ctrl+enter",
        "command": "clover.evaluate-top-block",
        "when": "!editorHasSelection"
    },
    {
        "key": "shift+enter",
        "command": "clover.evaluate-block",
        "when": ""
    },
    {
        "key": "ctrl+enter",
        "command": "clover.evaluate-selection",
        "when": "editorHasSelection"
    },
    {
        "key": "ctrl+shift+c",
        "command": "extension.connectSocketRepl",
        "when": ""
    },
    {
        "key": "ctrl+shift+l",
        "command": "clover.clear-console",
        "when": ""
    }
```

## Disclaimer:
This plug-in should be considered "alpha state". Its still in the beginning, and the only reason it works for evaluation, autocomplete, and supports lots of Clojure implementations is because it re-uses most of the code from Chlorine.

### Known problems:
Autocomplete can't find some suggestions. This could be a limitation of VSCode (that don't detect, for example, variables like `*clojure-version*` because of their special characters on the beginning)

There's also some synchronization problems, so sometimes a restart of VSCode is necessary.

## Related Projects:
* [Chlorine](https://github.com/mauricioszabo/atom-chlorine)
* [REPL Tooling](https://github.com/mauricioszabo/repl-tooling)
