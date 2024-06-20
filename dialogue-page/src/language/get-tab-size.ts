import { EditorState } from "@codemirror/state";
import { findFirstNode } from "../util/find-first-node";
import { NPC_STATEMENT } from "./nodes";
import { SyntaxNode } from "@lezer/common";

export const INDENTATION_KIND_SPACES: IndentationKind = " ";
export const INDENTATION_KIND_TABS: IndentationKind = "\t";
export const INDENTATION_KIND_MIXED: IndentationKind = " \t";
export const INDENTATION_KIND_NONE: IndentationKind = "";
export type IndentationKind = " " | "\t" | " \t" | "";

// TODO: Find a better way to set the indentation
export function inferTabSize(state: EditorState, defaultTabSize?: number): number {
    const firstNpcStatement = findFirstNode(state, NPC_STATEMENT);
    if (!firstNpcStatement) {
        return defaultTabSize != null ? defaultTabSize : state.tabSize;
    }

    const statementLine = state.doc.lineAt(firstNpcStatement.from);
    const documentIndentation = firstNpcStatement.from - statementLine.from;
    return documentIndentation;
}

export type IndentationInfo = { tabSize: number; indentationKind: IndentationKind };

export function inferIndentationInfo(state: EditorState, defaultTabSize?: number): IndentationInfo {
    const firstNpcStatement = findFirstNode(state, NPC_STATEMENT);
    if (!firstNpcStatement) {
        return {
            tabSize: defaultTabSize != null ? defaultTabSize : state.tabSize,
            indentationKind: INDENTATION_KIND_NONE,
        };
    }

    return getIndentationOfNode(state, firstNpcStatement);
}

export function getIndentationOfNode(state: EditorState, node: SyntaxNode): IndentationInfo {
    const statementLine = state.doc.lineAt(node.from);

    const documentIndentationString = state.sliceDoc(statementLine.from, node.from);
    const indentationIsSpaces = documentIndentationString.indexOf(" ") > -1;
    const indentationIsTabs = documentIndentationString.indexOf("\t") > -1;

    return {
        tabSize: documentIndentationString.length,
        indentationKind: indentationIsSpaces
            ? indentationIsTabs
                ? INDENTATION_KIND_MIXED
                : INDENTATION_KIND_SPACES
            : indentationIsTabs
            ? INDENTATION_KIND_TABS
            : INDENTATION_KIND_NONE,
    };
}
