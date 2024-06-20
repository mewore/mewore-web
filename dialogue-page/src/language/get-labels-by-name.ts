import { syntaxTree } from "@codemirror/language";
import { SyntaxNode } from "@lezer/common";
import { EditorState } from "@codemirror/state";
import { LABEL, LABEL_IDENTIFIER } from "./nodes";

export function getLabelsByName(state: EditorState): { [name: string]: SyntaxNode[] } {
    const labelsByName: { [name: string]: SyntaxNode[] } = {};
    for (const label of syntaxTree(state).topNode.getChildren(LABEL)) {
        const identifier = label.getChild(LABEL_IDENTIFIER);

        if (!identifier) {
            continue;
        }
        const name = state.sliceDoc(identifier.from, identifier.to);
        if (!labelsByName[name]) {
            labelsByName[name] = [];
        }
        labelsByName[name].push(identifier);
    }
    return labelsByName;
}
