package ic2.core.machine;

import java.util.Set;

public final class MonitorLayoutTest {

    public static void main(String[] args) {
        var rectangle = MonitorLayout.rectangle(Set.of(0L, 1L, 1L << 32, (1L << 32) | 1L));
        if (rectangle.width() != 2 || rectangle.height() != 2) throw new AssertionError("2x2 panel must form one screen");
        if (MonitorLayout.rectangle(Set.of(0L, 1L, 1L << 32)) != null) throw new AssertionError("L shape must not draw outside panels");
        if (MonitorLayout.rectangle(Set.of(MonitorLayout.key(-2, -3))).x() != -2) throw new AssertionError("negative coordinates");
        if (MonitorLayout.textX(100, 40, 0) != 0 || MonitorLayout.textX(100, 40, 1) != 30 || MonitorLayout.textX(100, 40, 2) != 60) throw new AssertionError("alignment");
        System.out.println("Monitor layout: all checks passed");
    }
}
