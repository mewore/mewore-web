import { syntaxTree } from "@codemirror/language";
import { Action, Diagnostic, linter } from "@codemirror/lint";
import { TransactionSpec } from "@codemirror/state";
import { getLabelsByName } from "../language/get-labels-by-name";
import { SyntaxNode, SyntaxNodeRef } from "@lezer/common";
import { EditorView } from "codemirror";
import { CONVERSATION_STATE, NPC_STATEMENT, SYNTAX_ERROR, USER_REPLY } from "../language/nodes";
import { repeatString } from "../util/repeat-string";
import { findFirstNode } from "../util/find-first-node";
import {
    INDENTATION_KIND_MIXED,
    INDENTATION_KIND_SPACES,
    IndentationInfo,
    getIndentationOfNode,
    inferIndentationInfo,
} from "../language/get-tab-size";

//! TODO: Decide whether to prevent colons from being at the end of text

function getIndentationErrors(
    view: EditorView,
    node: SyntaxNode,
    documentIndentation: IndentationInfo,
    usingSpaces: boolean,
    depth = 0,
): { diagnostic: Diagnostic; transaction: TransactionSpec }[] {
    let diagnosticsAndTransactions: { diagnostic: Diagnostic; transaction: TransactionSpec }[] = [];
    if (node.name === CONVERSATION_STATE) {
        depth++;
    } else if (node.name === NPC_STATEMENT || node.name === USER_REPLY) {
        const expectedIndentation = documentIndentation.tabSize * depth;
        const line = view.state.doc.lineAt(node.from);
        const actualIndentation = line.text.search(/[^ \t]/);
        const indentationString = line.text.substring(0, actualIndentation);
        if (expectedIndentation !== actualIndentation || indentationString.indexOf(usingSpaces ? "\t" : " ") > -1) {
            const newIndentString = repeatString(usingSpaces ? " " : "\t", expectedIndentation);
            const diagnostic: Diagnostic = {
                from: line.from,
                to: line.from + actualIndentation,
                severity: "warning",
                message: `The indentation here should be ${expectedIndentation} ${
                    usingSpaces ? "spaces" : "tabs"
                } for consistency.`,
                actions: [
                    {
                        name: `Fix indentation`,
                        apply(view, from, to) {
                            view.dispatch({ changes: { from, to, insert: newIndentString } });
                        },
                    },
                ],
            };
            const transaction: TransactionSpec = {
                changes: { from: line.from, to: line.from + actualIndentation, insert: newIndentString },
            };
            diagnosticsAndTransactions.push({ diagnostic, transaction });
        }
    }

    let child = node.firstChild;
    while (child) {
        diagnosticsAndTransactions = diagnosticsAndTransactions.concat(
            getIndentationErrors(view, child, documentIndentation, usingSpaces, depth),
        );
        child = child.nextSibling;
    }
    return diagnosticsAndTransactions;
}

export const indentationLinter = linter((view: EditorView): Diagnostic[] => {
    const root = syntaxTree(view.state).topNode;
    const firstNpcStatement = findFirstNode(view.state, NPC_STATEMENT);
    if (!firstNpcStatement) {
        return [];
    }
    const documentIndentation = getIndentationOfNode(view.state, firstNpcStatement);

    if (documentIndentation.indentationKind === INDENTATION_KIND_MIXED) {
        const firstIndentedLine = view.state.doc.lineAt(firstNpcStatement.from);
        const indentationEnd = firstIndentedLine.from + documentIndentation.tabSize;
        return [
            {
                from: firstIndentedLine.from,
                to: indentationEnd,
                severity: "warning",
                message: `The indentation here contains both spaces and tabs!`,
                actions: [
                    {
                        name: `Convert to spaces`,
                        apply(view, from, to) {
                            view.dispatch({
                                changes: { from, to, insert: repeatString(" ", documentIndentation.tabSize) },
                            });
                        },
                    },
                    {
                        name: `Convert to tabs`,
                        apply(view, from, to) {
                            view.dispatch({
                                changes: { from, to, insert: repeatString("\t", documentIndentation.tabSize) },
                            });
                        },
                    },
                ],
            },
        ];
    }

    const indentationErrors = getIndentationErrors(
        view,
        root,
        documentIndentation,
        documentIndentation.indentationKind === INDENTATION_KIND_SPACES,
    );
    if (indentationErrors.length > 1) {
        const allTransactions = indentationErrors.map((error) => error.transaction);

        const fixAllAction: Action = {
            name: `Fix all indentation`,
            apply(view, from, to) {
                view.dispatch(...allTransactions);
            },
        };
        for (const error of indentationErrors) {
            error.diagnostic.actions = [(error.diagnostic.actions || [])[0], fixAllAction];
        }
    }

    return indentationErrors.map((error) => error.diagnostic);
});
