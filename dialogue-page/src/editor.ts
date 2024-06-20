import { EditorView, basicSetup } from "codemirror";
import { dialogue } from "./language/dialogue";
import { docSizePlugin } from "./plugins/doc-size-plugin";
import { parsePreviewPlugin } from "./plugins/parse-preview-plugin";
import { labelLinkLinter } from "./plugins/label-link-linter";
import { labelLinkFollowPlugin } from "./plugins/label-link-follow-plugin";
import { lintingFunnyEffect } from "./plugins/linting-funny-effect";
import { labelIdentifierLinter } from "./plugins/label-identifier-linter";
import { statementAndReplyLinter } from "./plugins/statement-and-reply-linter";
import { realDialogue } from "./dialogues/real-dialogue";
import { smallDialogue } from "./dialogues/small-dialogue";
import { syntaxErrorLinter } from "./plugins/syntax-error-linter";
import { indentationLinter } from "./plugins/indentation-linter";
import { indentationPlugin } from "./plugins/indentation-plugin";
import { DialogueGraph } from "./graph/dialogue-graph";

function initEditor(): boolean {
    const editorContainer = document.getElementById("editor-container");
    if (!editorContainer) {
        return false;
    }

    const editor = new EditorView({
        extensions: [
            basicSetup,
            dialogue(),

            indentationPlugin,
            docSizePlugin,

            parsePreviewPlugin,
            labelIdentifierLinter,
            labelLinkLinter,
            labelLinkFollowPlugin,
            lintingFunnyEffect,
            statementAndReplyLinter,
            syntaxErrorLinter,
            indentationLinter,
        ],
        parent: editorContainer,
        doc: realDialogue,
    });
    return true;
}

function initGraph(): boolean {
    const graphContainer = document.getElementById("graph-container");

    if (!graphContainer) {
        return false;
    }

    const dialogueGraph = new DialogueGraph(graphContainer);
    // dialogueGraph.beginSimulation(
    //     {
    //         friction: 0.3,
    //         idealDistance: 0.1,
    //         maxSpeed: 0.5,
    //         edgeIntegrity: 0.05,
    //         repulsion: 0.0001,
    //     },
    //     300,
    // );
    return true;
}

function initialize(): boolean {
    return initEditor() && initGraph();
}

if (!initialize()) {
    window.addEventListener("load", initialize);
}
