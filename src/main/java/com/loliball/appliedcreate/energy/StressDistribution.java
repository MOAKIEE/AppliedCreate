package com.loliball.appliedcreate.energy;

/** Deterministic, bounded fair sharing. Storage callbacks return actual accepted amounts. */
public final class StressDistribution {
    private StressDistribution() {}

    @FunctionalInterface
    public interface Transfer {
        long accept(int port, long alreadyAssigned, long requested);
    }

    public static long deficit(double capacity, double load) {
        return units(Math.ceil(Math.max(0, load - capacity)));
    }

    public static long surplus(double capacity, double load) {
        return units(Math.floor(Math.max(0, capacity - load)));
    }

    public static long units(double value) {
        return Double.isFinite(value) && value > 0 ? (long) Math.min(value, Long.MAX_VALUE) : 0;
    }

    public static long[] allocate(long total, long[] limits, Transfer transfer) {
        long[] assigned = new long[limits.length];
        boolean[] exhausted = new boolean[limits.length];
        long remaining = Math.max(0, total);
        while (remaining > 0) {
            int active = 0;
            for (int i = 0; i < limits.length; i++) {
                if (!exhausted[i] && assigned[i] < limits[i]) active++;
            }
            if (active == 0) break;
            long share = remaining / active + (remaining % active == 0 ? 0 : 1);
            for (int i = 0; i < limits.length && remaining > 0; i++) {
                if (exhausted[i] || assigned[i] >= limits[i]) continue;
                long requested = Math.min(remaining, Math.min(share, limits[i] - assigned[i]));
                long actual = transfer.accept(i, assigned[i], requested);
                if (actual < 0 || actual > requested) {
                    throw new IllegalStateException("Invalid stress allocation result");
                }
                assigned[i] += actual;
                remaining -= actual;
                if (actual < requested) exhausted[i] = true;
            }
        }
        return assigned;
    }
}
