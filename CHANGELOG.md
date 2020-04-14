# Change Log

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
