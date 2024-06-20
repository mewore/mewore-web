import { EditorView } from "codemirror";
import { ViewPlugin, PluginValue, ViewUpdate } from "@codemirror/view";
import { SyntaxNode } from "@lezer/common";
import { getLabelsByName } from "../language/get-labels-by-name";

class LabelLinkFollowPlugin implements PluginValue {
    view: EditorView;
    styleElement: HTMLStyleElement;
    refreshCtrlHeldDownFn: (event: MouseEvent | KeyboardEvent) => void;
    tryFollowLinkFn: (event: MouseEvent | KeyboardEvent) => void;
    labelNodesByName: { [name: string]: SyntaxNode[] };

    constructor(view: EditorView) {
        this.view = view;
        this.styleElement = document.createElement("style");
        this.styleElement.textContent = `
        .ctrl-held-down :not(.cm-lintRange-error)>.ͼc.ͼ6 {
            cursor: pointer;
        }
        `;
        document.head.appendChild(this.styleElement);

        this.refreshCtrlHeldDownFn = this.refreshCtrlHeldDown.bind(this);
        document.addEventListener("keyup", this.refreshCtrlHeldDownFn);
        document.addEventListener("keydown", this.refreshCtrlHeldDownFn);
        view.dom.addEventListener("mousemove", this.refreshCtrlHeldDownFn);

        this.tryFollowLinkFn = this.tryFollowLink.bind(this);
        view.dom.addEventListener("click", this.tryFollowLinkFn);
    }

    refreshCtrlHeldDown(event: MouseEvent | KeyboardEvent) {
        if (event.ctrlKey) {
            this.view.dom.classList.add("ctrl-held-down");
        } else {
            this.view.dom.classList.remove("ctrl-held-down");
        }
    }

    tryFollowLink(event: MouseEvent | KeyboardEvent) {
        // TODO: Find a way not to rely on these randomly generated class names
        if (
            !event.ctrlKey ||
            !(event.target instanceof HTMLElement) ||
            !event.target.classList.contains("ͼc") ||
            !event.target.classList.contains("ͼ6")
        ) {
            return;
        }
        const name = event.target.innerText.trim();
        const label = (this.labelNodesByName[name] || [])[0];

        if (label) {
            setTimeout(() => {
                this.view.dispatch(
                    // { changes: { from: label.from, to: label.to, insert: "wawa" } },
                    { selection: { anchor: label.from, head: label.to }, scrollIntoView: true },
                );
            }, 0);

            event.preventDefault();
        }
    }

    update(update: ViewUpdate) {
        if (!update.docChanged) {
            return;
        }

        this.labelNodesByName = getLabelsByName(update.state);
    }

    destroy() {
        this.styleElement.remove();
        document.removeEventListener("keyup", this.refreshCtrlHeldDownFn);
        document.removeEventListener("keydown", this.refreshCtrlHeldDownFn);
        this.view.dom.removeEventListener("mousemove", this.refreshCtrlHeldDownFn);
        this.view.dom.removeEventListener("click", this.tryFollowLinkFn);
    }
}

export const labelLinkFollowPlugin = ViewPlugin.fromClass(LabelLinkFollowPlugin);
