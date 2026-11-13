package ic2.core.machine;

/** Battery is slot zero; inputs precede outputs. */
record ProcessingLayout(int inputs, int outputs) {
    ProcessingLayout {
        if (inputs < 1 || inputs > 2 || outputs < 1 || inputs == 2 && outputs != 2) {
            throw new IllegalArgumentException("Unsupported machine inventory layout");
        }
    }

    int slots() { return 1 + inputs + outputs; }
    boolean isInput(int slot) { return slot > 0 && slot <= inputs; }
    boolean supportsSplit() { return inputs == 2; }
    int firstOutput(int input, boolean split) { return split && supportsSplit() ? inputs + input : inputs + 1; }
    int lastOutput(int input, boolean split) { return split && supportsSplit() ? inputs + input : slots() - 1; }

    int inputForPass(int pass, boolean split, boolean firstEmpty, boolean secondEmpty) {
        if (pass < 0 || pass >= inputs) throw new IllegalArgumentException("Invalid processing pass");
        if (!supportsSplit()) return 1;
        if (split) return pass + 1;
        return pass == 0 ? (firstEmpty ? 2 : 1) : (secondEmpty ? 1 : 2);
    }
}
