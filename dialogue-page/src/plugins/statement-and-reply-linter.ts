import { syntaxTree } from "@codemirror/language";
import { Diagnostic, linter } from "@codemirror/lint";
import { getLabelsByName } from "../language/get-labels-by-name";
import { SyntaxNodeRef } from "@lezer/common";
import { EditorView } from "codemirror";
import { CONVERSATION_STATE, NPC_STATEMENT, SYNTAX_ERROR, USER_REPLY } from "../language/nodes";

function validatePrefix(node: SyntaxNodeRef, view: EditorView, expectedPrefix: string, diagnostics: Diagnostic[]) {
    const line = view.state.doc.lineAt(node.from);
    const lineIndentation = line.text.search(/[^ \t]/);
    const prefixStart = line.from + lineIndentation;

    const prefixEnd = Math.min(line.to, prefixStart + expectedPrefix.length);
    const prefix = view.state.doc.sliceString(prefixStart, prefixEnd);

    const firstNonPrefix = Math.min(line.from + line.text.search(/[^ \t\-]/), line.to);

    if (prefix !== expectedPrefix) {
        diagnostics.push({
            from: prefixStart,
            to: firstNonPrefix,
            severity: "error",
            message: `This line must begin with "${expectedPrefix}"!\n`,
            actions: [
                {
                    name: `Change to "${expectedPrefix}"`,
                    apply(view, from, to) {
                        view.dispatch({ changes: { from, to, insert: expectedPrefix } });
                    },
                },
            ],
        });
    }
}

//! TODO: Decide whether to prevent colons from being at the end of text

export const statementAndReplyLinter = linter((view) => {
    const labelsByName = getLabelsByName(view.state);

    let diagnostics: Diagnostic[] = [];
    syntaxTree(view.state)
        .cursor()
        .iterate((node: SyntaxNodeRef) => {
            if (node.name === NPC_STATEMENT) {
                validatePrefix(node, view, "- ", diagnostics);
            } else if (node.name === USER_REPLY) {
                validatePrefix(node, view, "- - ", diagnostics);
            } else if (node.name === CONVERSATION_STATE) {
                // TODO
            }
        });
    return diagnostics;
});
