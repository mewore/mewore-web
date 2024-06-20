export function repeatString(str: string, count: number): string {
    return Array.apply(null, Array(count))
        .map(() => str)
        .join("");
}
