import { indentNodeProp, foldNodeProp, bracketMatchingHandle, TreeIndentContext } from "@codemirror/language";
import { EditorState } from "@codemirror/state";
import { styleTags, tags } from "@lezer/highlight";
import { SyntaxNode } from "@lezer/common";
import { parser } from "../../grammar-dist/dialogue-parser";
import {
    LABEL_IDENTIFIER,
    LABEL_LINK_IDENTIFIER,
    STATEMENT_VARIATION_TEXT,
    VARIATION_SEPARATOR,
    REPLY_TEXT,
    COMMENT,
    LABEL,
    USER_REPLY,
    NPC_STATEMENT,
} from "./nodes";
import { findFirstNode } from "../util/find-first-node";
import { inferTabSize } from "./get-tab-size";

const dialogueStyleTags = styleTags({
    [LABEL_IDENTIFIER]: [tags.labelName, tags.strong],

    [LABEL_LINK_IDENTIFIER]: [tags.labelName, tags.link],

    [STATEMENT_VARIATION_TEXT]: [tags.string, tags.emphasis],
    [VARIATION_SEPARATOR]: [tags.separator, tags.contentSeparator, tags.controlOperator],

    [REPLY_TEXT]: [tags.string, tags.strong],
    [COMMENT]: [tags.comment],
});

const foldAll = (node: SyntaxNode, _state: EditorState): { from: number; to: number } | null => {
    let lastChild = node;
    while (lastChild.lastChild) {
        lastChild = lastChild.lastChild;
        while (lastChild.name === COMMENT && lastChild.prevSibling) {
            lastChild = lastChild.prevSibling;
        }
    }
    if (node.firstChild) {
        return { from: node.firstChild.to, to: lastChild.to };
    }

    return { from: node.from, to: lastChild.to };
};

export const dialogueParser = parser.configure({
    props: [
        dialogueStyleTags,
        indentNodeProp.add({
            // [LABEL]: (context) => {
            //     console.log(context);
            //     return context.unit;
            // },
            // [LABEL]: (context) => context.state.tabSize,
            [LABEL]: (context) => inferTabSize(context.state, context.unit),
            [USER_REPLY]: (context) => context.column(context.node.from) + inferTabSize(context.state, context.unit),
        }),
        foldNodeProp.add({
            [LABEL]: foldAll,
            [NPC_STATEMENT]: foldAll,
            [USER_REPLY]: foldAll,
        }),
        // TODO: Find out how to make this work
        bracketMatchingHandle.add({
            [NPC_STATEMENT]: (node: SyntaxNode) => node.parent,
            [USER_REPLY]: (node: SyntaxNode) => node.parent,
        }),
    ],
    // strict: true,
});
