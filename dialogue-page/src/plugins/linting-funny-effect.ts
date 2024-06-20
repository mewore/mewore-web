import { EditorView } from "codemirror";
import { ViewPlugin, PluginValue } from "@codemirror/view";

class LintingFunnyEffectPlugin implements PluginValue {
    styleElement: HTMLStyleElement;

    constructor(view: EditorView) {
        this.styleElement = document.createElement("style");
        this.styleElement.textContent = `
        .cm-lintRange-error {
            animation: blink-animation 2s cubic-bezier(.5, 0, 1, 1) infinite;
            -webkit-animation: blink-animation 2s cubic-bezier(.5, 0, 1, 1) infinite;
        }
        .cm-lintRange-error > * {
            text-decoration: none;
        }
        .cm-line:has(.cm-lintRange-error), .cm-line:has(.cm-lintPoint-error) {
            background: rgba(255, 0, 0, 0.05);
        }

        @keyframes blink-animation {

            from,
            to {
                background-color: rgba(255, 0, 0, 0);
            }

            50% {
                background-color: rgba(255, 0, 0, 0.2);
            }
        }

        @-webkit-keyframes blink-animation {

            from,
            to {
                background-color: rgba(255, 0, 0, 0);
            }

            50% {
                background-color: rgba(255, 0, 0, 0.2);
            }
        }
        `;
        document.head.appendChild(this.styleElement);
    }

    destroy() {
        this.styleElement.remove();
    }
}

export const lintingFunnyEffect = ViewPlugin.fromClass(LintingFunnyEffectPlugin);
