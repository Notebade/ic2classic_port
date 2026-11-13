package ic2.core.machine;

public final class ProcessingLayoutTest {
    public static void main(String[] args) {
        verify(new ProcessingLayout(2, 2), 5, 3, 4);
        verify(new ProcessingLayout(1, 2), 4, 2, 3);
        verify(new ProcessingLayout(1, 3), 5, 2, 4);
        verify(new ProcessingLayout(1, 1), 3, 2, 2);
        var single = new ProcessingLayout(1, 3);
        check(!single.supportsSplit(), "single-input machines never process a second input");
        check(single.firstOutput(1, true) == 2 && single.lastOutput(1, true) == 4,
            "single-input output uses every output slot even when loading an old split flag");
        var dual = new ProcessingLayout(2, 2);
        check(dual.firstOutput(1, true) == 3 && dual.lastOutput(1, true) == 3, "first split lane");
        check(dual.firstOutput(2, true) == 4 && dual.lastOutput(2, true) == 4, "second split lane");
        check(dual.firstOutput(2, false) == 3 && dual.lastOutput(2, false) == 4, "shared lane");
        check(single.inputForPass(0, false, true, true) == 1, "pump recipe still uses the single input lane");
        check(dual.inputForPass(0, false, true, false) == 2, "shared mode falls back to second input");
        check(dual.inputForPass(1, false, false, true) == 1, "shared mode can process a second item from first input");
        check(dual.inputForPass(1, true, false, true) == 2, "split mode never processes first lane twice");
        System.out.println("Processing layouts: all checks passed");
    }

    private static void verify(ProcessingLayout layout, int slots, int first, int last) {
        check(layout.slots() == slots, "inventory size includes one battery slot");
        check(layout.firstOutput(1, false) == first && layout.lastOutput(1, false) == last,
            "pooled output range follows input slots");
        check(layout.isInput(1) && !layout.isInput(0) && !layout.isInput(first), "battery and outputs are not inputs");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
