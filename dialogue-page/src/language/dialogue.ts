import { LanguageSupport, LRLanguage } from "@codemirror/language";
import { CompletionContext, CompletionResult, ifIn } from "@codemirror/autocomplete";
import { getLabelsByName } from "./get-labels-by-name";
import { LABEL_LINK_IDENTIFIER, LABEL_LINK } from "./nodes";
import { dialogueParser } from "./parser";

const dialogueLanguage = LRLanguage.define({
    name: "dialogue",
    parser: dialogueParser,
    languageData: {},
});

const dialogueCompletion = dialogueLanguage.data.of({
    autocomplete: ifIn(
        [LABEL_LINK, LABEL_LINK_IDENTIFIER],
        (context: CompletionContext): CompletionResult | null | Promise<CompletionResult | null> => {
            const line = context.state.doc.lineAt(context.pos);
            if (context.state.doc.sliceString(context.pos, line.to + 1).trim() !== "") {
                // We do not want to provide autocompletion if there is text after the cursor
                return null;
            }

            // Find the first hash ("#") preceded by whitespace
            let hashPosRelative = line.text.indexOf("#");
            while (hashPosRelative > -1 && line.text.charAt(hashPosRelative - 1).trim().length > 0) {
                hashPosRelative = line.text.indexOf("#", hashPosRelative + 1);
            }
            const hashPos = line.from + hashPosRelative;
            if (hashPosRelative === -1 || context.pos <= hashPos) {
                return null;
            }

            let contextFrom = hashPos;
            let prefix = "";
            if (context.pos === hashPos + 1) {
                // #[cursor]
                contextFrom = hashPos;
                prefix = "# ";
            } else {
                const afterHash = context.state.doc.sliceString(hashPos + 1, context.pos);
                if (afterHash.trim() === "") {
                    // # [whitespace](...)[whitespace] [cursor]
                    contextFrom = hashPos;
                    prefix = `#${afterHash}`;
                } else {
                    // # [text](...)[text] [cursor]
                    const nonWhitespaceIndex = afterHash.search(/[^ \t]/);
                    contextFrom = hashPos + 1 + nonWhitespaceIndex;
                    prefix = "";
                }
            }

            const labelsByName = getLabelsByName(context.state);
            const options = Object.keys(labelsByName).map((labelName) => ({
                label: prefix + labelName,
                type: "class",
            }));
            return { from: contextFrom, options };
        },
    ),
});

export function dialogue() {
    return new LanguageSupport(dialogueLanguage, [dialogueCompletion]);
}
