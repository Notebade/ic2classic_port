package ic2.core.machine;

public final class WindFieldTest {

    public static void main(String[] args) {
        var state = WindField.initial(123);
        for (int i = 0; i < 20000; i++) {
            var next = state.step();
            if (Math.abs(next.current() - state.current()) > 0.100001 || Math.abs(next.current()) > 100) throw new AssertionError("wind must change smoothly and stay within original range");
            var loaded = new WindField.State(state.current(), state.target(), state.remaining(), state.seed());
            if (!next.equals(loaded.step())) throw new AssertionError("saved wind must resume identically");
            state = next;
        }
        if (WindField.initial(123).equals(WindField.initial(124))) throw new AssertionError("worlds need independent wind");
        System.out.println("Persistent wind field: all checks passed");
    }
}
