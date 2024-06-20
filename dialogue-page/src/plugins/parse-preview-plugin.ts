import { EditorView } from "codemirror";
import { logTree } from "../util/print-lezer-tree";
import { ViewPlugin, PluginValue, ViewUpdate } from "@codemirror/view";
import { EditorState } from "@codemirror/state";
import { dialogueParser } from "../language/parser";

const MAX_SIZE_ON_UPDATE = 500;

class ParsePreviewPlugin implements PluginValue {
    constructor(view: EditorView) {
        this.print(view.state);
    }

    update(update: ViewUpdate) {
        if (update.docChanged && update.state.doc.length <= MAX_SIZE_ON_UPDATE) {
            this.print(update.state);
        }
    }

    print(state: EditorState): void {
        const parsedTree = dialogueParser.parse(state.doc.toString());
        logTree(parsedTree, state.doc.toString());
    }
}

export const parsePreviewPlugin = ViewPlugin.fromClass(ParsePreviewPlugin);
