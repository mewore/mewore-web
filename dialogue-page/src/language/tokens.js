import { ContextTracker, ExternalTokenizer } from "@lezer/lr";
import { indent, dedent, blankLineStart } from "../../grammar-dist/dialogue-parser.terms";

class IndentLevel {
    constructor(parent, depth) {
        this.parent = parent;
        this.depth = depth;
        this.hash = (parent ? (parent.hash + parent.hash) << 8 : 0) + depth + (depth << 4);
    }
}

export const trackIndent = new ContextTracker({
    start: new IndentLevel(null, 0),
    shift(context, term, stack, input) {
        if (term === indent) return new IndentLevel(context, stack.pos - input.pos);
        if (term === dedent) return context.parent;
        return context;
    },
    hash: (context) => context.hash,
});

const OUT_OF_FILE = -1,
    NEWLINE = 10,
    SPACE = 32,
    TAB = 9,
    HASH = 35;

export const indentation = new ExternalTokenizer((input, stack) => {
    let prev = input.peek(-1);
    // Do the indentation check only at newlines
    if (prev != OUT_OF_FILE && prev != NEWLINE) {
        return;
    }

    let spaces = 0;
    while (input.next == SPACE || input.next == TAB) {
        input.advance();
        spaces++;
    }

    if ((input.next == NEWLINE || input.next == HASH) && stack.canShift(blankLineStart)) {
        // console.log(`Accepted token blankLineStart at ${input.pos}; spaces=${spaces}`);
        input.acceptToken(blankLineStart, -spaces);
    } else if (spaces > stack.context.depth) {
        input.acceptToken(indent);
    } else if (spaces < stack.context.depth) {
        input.acceptToken(dedent, -spaces);
    }
});
