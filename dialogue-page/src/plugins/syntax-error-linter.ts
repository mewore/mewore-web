import { syntaxTree } from "@codemirror/language";
import { Action, Diagnostic, linter } from "@codemirror/lint";
import { getLabelsByName } from "../language/get-labels-by-name";
import { LABEL_LINK_IDENTIFIER } from "../language/nodes";

export const syntaxErrorLinter = linter((view) => {
    const labelsByName = getLabelsByName(view.state);

    let diagnostics: Diagnostic[] = [];
    syntaxTree(view.state)
        .cursor()
        .iterate((node) => {
            if (node.name === "⚠") {
                diagnostics.push({
                    from: node.from,
                    to: node.to,
                    severity: "warning",
                    message: `SYNTAX ERROR!\n`,
                    actions: [],
                });
                return;
            }
        });
    return diagnostics;
});
