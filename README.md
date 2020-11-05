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

### Example:

![Evaluating code](doc/sample.gif)

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

### Custom Commands
Exactly as in Chlorine, [there's support for Custom Commands](https://github.com/mauricioszabo/atom-chlorine/blob/master/docs/extending.md). Please follow the link for more explanation, but the idea is that, after connecting to a REPL, you run the command `Clover: Open config file" and then register your custom commands there. Please notice that "custom tags" is **not supported**, and probably never will unless VSCode makes its API better - the only custom tags supported are `:div/clj` and `:div/ansi` (the first accepts a Clojure data structure and uses the Clover renderer to render it, the second accepts a string with ANSI codes and color then accordingly).

Because of limitations of VSCode, you will **not see** custom commands on the command palette - they are registered as Tasks. So you'll run the command "Tasks: Run Task" and then choose "Clover". There, the custom commands will be presented.

The reason that Clover uses tasks is because **you can set keybindings to tasks** - to register a keybinding to a task, you need to run the command "Preferences: Open Keyboard Shortcuts (JSON)". Be aware that **you need to edit keybindings via  JSON**. There, you'll define a keybinding for the command `workbench.action.tasks.runTask` and the args will be **exactly the name** that appears on the task menu - **case sensitive**.

So, supposed you did add a custom command on your config (one that just prints "Hello, World" to the console:

```clojure
(defn hello-world []
  (println "Hello, World!"))
```

Then, you'll see that the task registered will be named "Clover: Hello world". You can register, for example, `ctrl+h` with the following JSON fragment:

```json
    {
        "key": "ctrl+h",
        "command": "workbench.action.tasks.runTask",
        "args": "Clover: Hello world"
    }
```

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
Currently there are some synchronization problems, so sometimes a restart of VSCode is necessary.

## Related Projects:
* [Chlorine](https://github.com/mauricioszabo/atom-chlorine)
* [REPL Tooling](https://github.com/mauricioszabo/repl-tooling)
