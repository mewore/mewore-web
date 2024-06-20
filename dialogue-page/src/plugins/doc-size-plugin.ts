import { EditorView } from "codemirror";
import { ViewPlugin, PluginValue, ViewUpdate } from "@codemirror/view";

const NOTECARD_LIMIT_BYTES = 65536;

class DocSizePlugin implements PluginValue {
    dom: HTMLElement;

    constructor(view: EditorView) {
        this.dom = view.dom.appendChild(document.createElement("div"));
        this.dom.style.cssText = "position: absolute; inset-block-end: 2em; inset-inline-end: 1em";
        this.dom.style.cursor = "help";
        this.updateText(view.state.doc.length);
    }

    update(update: ViewUpdate) {
        if (update.docChanged) {
            this.updateText(update.state.doc.length);
        }
    }

    updateText(characters: number): void {
        this.dom.textContent = `${characters} / ${NOTECARD_LIMIT_BYTES}`;
        if (characters <= NOTECARD_LIMIT_BYTES) {
            const closenessToLimit = characters / NOTECARD_LIMIT_BYTES;
            const red = 50 + closenessToLimit * 205;
            const green = 50 + closenessToLimit * 75;
            const blue = (1.0 - closenessToLimit) * 100;
            this.dom.style.color = `rgb(${red}, ${green}, ${blue})`;
            this.dom.style.fontWeight = "unset";
        } else {
            this.dom.style.color = "red";
            this.dom.style.fontWeight = "bold";
        }
        this.dom.title = `There are ${characters} characters in this file. A notecard in Second Life can contain up to ${NOTECARD_LIMIT_BYTES} characters.`;
    }

    destroy() {
        this.dom.remove();
    }
}

export const docSizePlugin = ViewPlugin.fromClass(DocSizePlugin);
