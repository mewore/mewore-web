import { syntaxTree } from "@codemirror/language";
import { EditorState } from "@codemirror/state";
import { SyntaxNode } from "@lezer/common";

export function findFirstNode(state: EditorState, name: string) {
    return findFirstNodeOfType(syntaxTree(state).topNode, name);
}

function findFirstNodeOfType(node: SyntaxNode, name: string): SyntaxNode | undefined {
    if (node.name === name) {
        return node;
    }
    let child = node.firstChild;
    while (child) {
        const childNpcStatement = findFirstNodeOfType(child, name);
        if (childNpcStatement) {
            return childNpcStatement;
        }
        child = child.nextSibling;
    }
    return undefined;
}
