# Change Log

## FUTURE
- More commands comming from Orchard:
  - find-usages
  - clojure-doc-for-var


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
