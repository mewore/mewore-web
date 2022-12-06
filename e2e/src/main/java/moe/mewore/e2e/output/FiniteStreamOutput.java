package moe.mewore.e2e.output;

import java.io.InputStream;
import java.util.Scanner;

public class FiniteStreamOutput implements ApplicationOutput {

    private final Scanner scanner;

    public FiniteStreamOutput(final InputStream stream) {
        scanner = new Scanner(stream);
    }

    @Override
    public String nextLine() {
        return scanner.nextLine();
    }

    @Override
    public boolean mayHaveNextLine() {
        return scanner.hasNext();
    }

    @Override
    public void close() {
    }
}
