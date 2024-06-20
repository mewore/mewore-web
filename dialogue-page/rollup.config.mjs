import { nodeResolve } from "@rollup/plugin-node-resolve";
import typescript from "@rollup/plugin-typescript";

export default {
    input: "./src/editor.ts",
    output: {
        file: "./public/js/editor.bundle.js",
        format: "iife",
        name: "DialogueEditorBundle",
    },
    plugins: [nodeResolve(), typescript()],
};
``;
