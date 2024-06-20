import { syntaxTree } from "@codemirror/language";
import { Diagnostic, linter } from "@codemirror/lint";
import { getLabelsByName } from "../language/get-labels-by-name";
import { LABEL_IDENTIFIER, LABEL_IDENTIFIER_COLON } from "../language/nodes";

export const labelIdentifierLinter = linter((view) => {
    const labelsByName = getLabelsByName(view.state);

    let diagnostics: Diagnostic[] = [];
    syntaxTree(view.state)
        .cursor()
        .iterate((node) => {
            if (node.name !== LABEL_IDENTIFIER) {
                return;
            }
            const name = view.state.sliceDoc(node.from, node.to);
            if (labelsByName[name]?.length > 1 && node.from !== labelsByName[name][0].from) {
                diagnostics.push({
                    from: node.from,
                    to: node.to,
                    severity: "error",
                    message: `There is already another label named '${name}'!\n`,
                    actions: [],
                });
            }
            const shouldBeColon = node.node.nextSibling;
            const line = view.state.doc.lineAt(node.from);
            if (
                shouldBeColon &&
                shouldBeColon?.name === LABEL_IDENTIFIER_COLON &&
                view.state.doc.sliceString(shouldBeColon.from, shouldBeColon.to).trim() !== ""
            ) {
                if (shouldBeColon.to !== line.to) {
                    // There must be some unmatched error-characters after it
                    diagnostics.push({
                        from: shouldBeColon.from,
                        to: line.to,
                        severity: "error",
                        message: `There must be ONLY one colon (":") at the end of this line!\n`,
                        actions: [],
                    });
                }
            } else {
                diagnostics.push({
                    from: node.to,
                    to: line.to,
                    severity: "error",
                    message: `There must be a colon (":") at the end of this line!\n`,
                    actions: [
                        {
                            name: `Add colon`,
                            apply(view, from, to) {
                                view.dispatch({ changes: { from, to, insert: ":" } });
                            },
                        },
                    ],
                });
            }

            if (node.from > line.from && line.text.charAt(0).trim() === "") {
                diagnostics.push({
                    from: line.from,
                    to: node.from,
                    severity: "error",
                    message: `There must be no whitespace before the label!\n`,
                    actions: [
                        {
                            name: `Remove`,
                            apply(view, from, to) {
                                view.dispatch({ changes: { from, to } });
                            },
                        },
                    ],
                });
            }
        });
    return diagnostics;
});
