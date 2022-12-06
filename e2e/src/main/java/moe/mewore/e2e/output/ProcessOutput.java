package moe.mewore.e2e.output;

public class ProcessOutput extends FiniteStreamOutput {

    public ProcessOutput(final Process process) {
        super(process.getInputStream());
    }
}
