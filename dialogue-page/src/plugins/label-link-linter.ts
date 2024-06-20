import { syntaxTree } from "@codemirror/language";
import { Action, Diagnostic, linter } from "@codemirror/lint";
import { getLabelsByName } from "../language/get-labels-by-name";
import { LABEL_LINK_IDENTIFIER, NPC_STATEMENT } from "../language/nodes";

export const labelLinkLinter = linter((view) => {
    const labelsByName = getLabelsByName(view.state);

    let diagnostics: Diagnostic[] = [];
    syntaxTree(view.state)
        .cursor()
        .iterate((node) => {
            if (node.name !== LABEL_LINK_IDENTIFIER) {
                return;
            }
            const name = view.state.sliceDoc(node.from, node.to);
            if (!labelsByName[name]) {
                const actions: Action[] = [];
                for (const name in labelsByName) {
                    actions.push({
                        name: `Change to "${name}"`,
                        apply(view, from, to) {
                            view.dispatch({ changes: { from, to, insert: name } });
                        },
                    });
                }
                diagnostics.push({
                    from: node.from,
                    to: node.to,
                    severity: "error",
                    message: `There is no label named "${name}"!\n`,
                    actions,
                });
            }

            const parent = node.node?.parent;
            const parentNextSibling = parent?.nextSibling;
            if (parent && parentNextSibling) {
                const thisNode = parent.name === NPC_STATEMENT ? "NPC statement" : "user reply";
                const childNodes = parent.name === NPC_STATEMENT ? "replies" : "NPC statements";
                diagnostics.push({
                    from: node.from,
                    to: node.to,
                    severity: "warning",
                    message: `This label link causes the conversation to go to the "${name}" label after this ${thisNode},\
                     making the ${childNodes} after this redundant\n`,
                    actions: [
                        {
                            name: "Remove this label link",
                            apply: (view) => view.dispatch({ changes: { from: parent.from, to: parent?.to } }),
                        },
                        {
                            name: `Remove the ${childNodes} underneath (danger!)`,
                            apply: (view) =>
                                view.dispatch({
                                    changes: { from: parentNextSibling.from, to: parent?.parent?.to },
                                }),
                        },
                    ],
                });
            }
        });
    return diagnostics;
});
