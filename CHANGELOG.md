# Change Log

## 0.2.1
- Fixed some errors on connecting
- Added an option to define where to put the config file

## 0.2.0
- Remembering host:port (https://github.com/mauricioszabo/clover/issues/7)
- Added custom commands and configs
- Fix on regexp printing
- Using another way to connect to Shadow-CLJS
- Added a config for Experimental Features (exactly the same as Chlorine)
- Fix error while trying to print lots of really small lines
- Clickable stacktraces for Clojerl
- Better printer for Clojerl and nREPL
- Cutting some stdout messages from aux REPL
- Better support for Lumo, CLR, and Joker
- Fixed defining functions with same name as `clojure.core` (fixes https://github.com/mauricioszabo/atom-chlorine/issues/214).
- Fixed clojure REPL not connecting on first try
- Interactive eval redirects STDOUT
- Goto var definition and doc for var now work with ClojureScript's macros
- Stacktraces on ClojureScript will use source-maps to parse their errors

### Needs experimental features toggled:
- Implemented the new Websocket REPL of Shadow-CLJS (Remote API)
- Possibility of running Shadow-CLJS Remote API commands (see: https://github.com/mauricioszabo/repl-tooling/pull/83)
- `tap>` support for Shadow-CLJS Relay API
- Support for resolving promises on Shadow Relay API


## 0.1.1
- Fixes on nREPL imports for Orchard, Compliment, etc (https://github.com/mauricioszabo/atom-chlorine/issues/191)
- Forward-finding namespaces if a NS form was not found before the cursor (fixes https://github.com/mauricioszabo/atom-chlorine/issues/193)
- Fix on `get-selection` for configs in ClojureScript
- Performance improvement while parsing Clojure code
- Fixed error trying to connect to ClojureScript socket REPLs
- Fixed core Clojerl exception

## 0.1.0
- Fixed connection for other Clojure implementations like Clojerl, Joker, etc.
- Interactive results
- First support for `info` command, if "Orchard" is on classpath
- Code refactoring, fixed issues
- Loading tests on full refresh
- Fixed parsing of namespaces with metadata, and quotes (the kind you get when using clj-kondo)
- Load-file now prints the stacktrace when it fails to load
- Fixed paths on Windows, so goto var definition and clicking on stacktraces will work
- Simple fix for nREPL on slower sockets
- Alpha support for nREPL
- Fixed connection with Babashka
- Fixed issue with GOTO Var Definition when the current file's full path is too long.


## 0.0.7
- Support for nREPL
- Indentation engine (alpha)
- Load-file now prints the stacktrace when it fails to load
- Fixed paths on Windows, so goto var definition and clicking on stacktraces will work
- Fixed connection with Babashka
- Fixed issue with GOTO Var Definition when the current file's full path is too long.
- Redirecting `*test-out*` to the right output
- Fixes "Attempting to call unbound fn: #'unrepl.core/write" (https://github.com/mauricioszabo/atom-chlorine/issues/158)


## 0.0.6
- Fixed issues with Clojure grammar rules (vars are now identified correctly)

## 0.0.5
- Added better support for ClojureScript
- Autocomplete and goto var definition fixes
- Added "doc for var" command

## 0.0.1

- Initial release
- Connect Socket REPL
- Evaluations
- Disconnect
- Goto VAR definition (only on current project)
