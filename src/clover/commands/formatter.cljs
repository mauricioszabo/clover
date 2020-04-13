(ns clover.commands.formatter
  (:require ["parinfer" :as par]))

    ; let editor = vscode.window.activeTextEditor;
    ; let pos = new vscode.Position(position.line, 0);
    ; let indent = getIndent(getDocument(document).model.lineInputModel, getDocumentOffset(document, position), config.getConfig());
    ; let delta = document.lineAt(position.line).firstNonWhitespaceCharacterIndex - indent;
    ; if (delta > 0) {}
    ;     //return [vscode.TextEdit.delete(new vscode.Range(pos, new vscode.Position(pos.line, delta)))];
    ;     return editor.edit(edits => edits.delete(new vscode.Range(pos, new vscode.Position(pos.line, delta))), { undoStopAfter: false, undoStopBefore: false});
    ;
    ; else if (delta < 0) {}
    ;     let str = "";
    ;     while (delta++ < 0)
    ;         str += " ";
    ;     //return [vscode.TextEdit.insert(pos, str)];
    ;     return editor.edit(edits => edits.insert(pos, str), { undoStopAfter: false, undoStopBefore: false})));

(defn- format-on-type [document]
  (prn :FORMATTING!))

        ; const editor = vscode.window.activeTextEditor;
        ; continueComment(editor, document, editor.selection.active).then(() => {})
        ;     const pos = editor.selection.active;
        ;     if (vscode.workspace.getConfiguration("calva.fmt").get("formatAsYouType")) {}
        ;         if (vscode.workspace.getConfiguration("calva.fmt").get("newIndentEngine")) {}
        ;             return formatter.indentPosition(pos, document);
        ;          else {}
        ;             try {}
        ;                 return formatter.formatPosition(editor, true);
        ;              catch (e) {}
        ;                 return formatter.indentPosition(pos, document)));

(def formatter #js {:provideOnTypeFormattingEdits format-on-type})
