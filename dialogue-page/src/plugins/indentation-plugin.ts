import { EditorView } from "codemirror";
import { ViewPlugin, PluginValue, ViewUpdate } from "@codemirror/view";
import { EditorState, TransactionSpec } from "@codemirror/state";
import { findFirstNode } from "../util/find-first-node";
import { syntaxTree } from "@codemirror/language";
import { CONVERSATION_STATE, NPC_STATEMENT, USER_REPLY } from "../language/nodes";
import {
    IndentationKind,
    INDENTATION_KIND_NONE,
    INDENTATION_KIND_SPACES,
    inferIndentationInfo,
    inferTabSize,
    INDENTATION_KIND_TABS,
    INDENTATION_KIND_MIXED,
    IndentationInfo,
} from "../language/get-tab-size";
import { Diagnostic } from "@codemirror/lint";
import { SyntaxNode } from "@lezer/common";
import { repeatString } from "../util/repeat-string";

function getReindentTransactions(
    state: EditorState,
    node: SyntaxNode,
    tabSize: number,
    usingSpaces: boolean,
    depth = 0,
): TransactionSpec[] {
    let transactions: TransactionSpec[] = [];
    if (node.name === CONVERSATION_STATE) {
        depth++;
    } else if (node.name === NPC_STATEMENT || node.name === USER_REPLY) {
        const expectedIndentation = tabSize * depth;
        const line = state.doc.lineAt(node.from);
        const actualIndentation = line.text.search(/[^ \t]/);
        const indentationString = line.text.substring(0, actualIndentation);

        if (expectedIndentation !== actualIndentation || indentationString.indexOf(usingSpaces ? "\t" : " ") > -1) {
            const newIndentString = repeatString(usingSpaces ? " " : "\t", expectedIndentation);
            const transaction: TransactionSpec = {
                changes: { from: line.from, to: line.from + actualIndentation, insert: newIndentString },
            };
            transactions.push(transaction);
        }
    }

    let child = node.firstChild;
    while (child) {
        transactions = transactions.concat(getReindentTransactions(state, child, tabSize, usingSpaces, depth));
        child = child.nextSibling;
    }
    return transactions;
}

class IndentationPlugin implements PluginValue {
    view: EditorView;
    container: HTMLElement;
    tabSizeButton: HTMLInputElement;
    indentationKindButton: HTMLElement;

    indentInfo: IndentationInfo;
    nextIndentationKind: IndentationKind = INDENTATION_KIND_NONE;

    constructor(view: EditorView) {
        this.view = view;
        this.container = view.dom.appendChild(document.createElement("div"));
        this.container.style.cssText = "position: absolute; inset-block-end: 0em; inset-inline-end: 1em";

        this.tabSizeButton = document.createElement("input");
        this.tabSizeButton.type = "number";
        this.tabSizeButton.min = "1";
        this.container.appendChild(this.tabSizeButton);
        this.tabSizeButton.addEventListener("change", this.changeIndentationSize.bind(this));
        this.tabSizeButton.style.width = "4em";
        this.tabSizeButton.style.textAlign = "right";

        this.indentationKindButton = document.createElement("button");
        this.container.appendChild(this.indentationKindButton);
        this.indentationKindButton.addEventListener("click", this.changeIndentationKind.bind(this));
        this.indentationKindButton.style.minWidth = "5em";

        this.updateText(view.state);
    }

    update(update: ViewUpdate) {
        if (update.docChanged) {
            update.changes;
            this.updateText(update.state);
        }
    }

    changeIndentationSize() {
        const newTabSize = parseInt(this.tabSizeButton.value);
        if (newTabSize !== this.indentInfo.tabSize) {
            this.reindent(newTabSize, this.indentInfo.indentationKind);
        }
    }

    changeIndentationKind() {
        this.reindent(this.indentInfo.tabSize, this.nextIndentationKind);
    }

    reindent(tabSize: number, indentationKind: IndentationKind) {
        if (indentationKind !== INDENTATION_KIND_SPACES && indentationKind !== INDENTATION_KIND_TABS) {
            return;
        }
        const reindentTransactions = getReindentTransactions(
            this.view.state,
            syntaxTree(this.view.state).topNode,
            tabSize,
            indentationKind === INDENTATION_KIND_SPACES,
        );
        this.view.dispatch(...reindentTransactions);
    }

    updateText(state: EditorState): void {
        this.indentInfo = inferIndentationInfo(state);

        const suffix = this.indentInfo.tabSize == 1 ? "" : "s";
        this.nextIndentationKind = INDENTATION_KIND_SPACES;
        this.tabSizeButton.removeAttribute("disabled");
        this.indentationKindButton.removeAttribute("disabled");
        switch (this.indentInfo.indentationKind) {
            case " ":
                this.indentationKindButton.textContent = `space${suffix}`;
                this.nextIndentationKind = INDENTATION_KIND_TABS;
                break;
            case "\t":
                this.indentationKindButton.textContent = `tab${suffix}`;
                break;
            case " \t":
                this.indentationKindButton.textContent = `<MIXED>`;
                break;
            default:
                this.indentationKindButton.textContent = `<NONE>`;
                this.nextIndentationKind = INDENTATION_KIND_NONE;
                this.tabSizeButton.setAttribute("disabled", "true");
                this.indentationKindButton.setAttribute("disabled", "true");
        }
        if (this.indentInfo.indentationKind !== INDENTATION_KIND_NONE) {
            this.tabSizeButton.value = this.indentInfo.tabSize.toString();
        }
    }

    destroy() {
        this.container.remove();
    }
}

export const indentationPlugin = ViewPlugin.fromClass(IndentationPlugin);
