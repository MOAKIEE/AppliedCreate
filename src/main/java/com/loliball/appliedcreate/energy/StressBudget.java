package com.loliball.appliedcreate.energy;

import java.util.function.LongUnaryOperator;

/** Whole stored SU are prepaid before they can become kinetic capacity. */
public final class StressBudget {
    private long stored;
    private long tick = Long.MIN_VALUE;
    private long committed;

    public long stored() {
        return stored;
    }

    public void restore(long amount) {
        stored = Math.max(0, amount);
        tick = Long.MIN_VALUE;
        committed = 0;
    }

    private void begin(long now) {
        if (tick != now) {
            tick = now;
            committed = 0;
        }
    }

    public long available(long now) {
        begin(now);
        return stored > Long.MAX_VALUE - committed ? Long.MAX_VALUE : stored + committed;
    }

    /** A simulation is not payment. Only the actual extraction result enters the buffer. */
    public long reserve(long now, long target, LongUnaryOperator extract) {
        long available = available(now);
        if (target > available) {
            long requested = target - available;
            long extracted = checked(extract.applyAsLong(requested), requested);
            stored += extracted;
        }
        return Math.min(target, available(now));
    }

    /** Replanning the same tick charges only an increase, never the full demand again. */
    public void commit(long now, long amount) {
        if (amount < 0 || amount > available(now)) {
            throw new IllegalArgumentException("Unfunded stress capacity");
        }
        if (amount > committed) {
            stored -= amount - committed;
            committed = amount;
        }
    }

    /** Unused reservations are returned; a rejected return remains owned by this block. */
    public long refund(LongUnaryOperator insert) {
        if (stored == 0) return 0;
        long returned = checked(insert.applyAsLong(stored), stored);
        stored -= returned;
        return returned;
    }

    private static long checked(long actual, long requested) {
        if (actual < 0 || actual > requested) {
            throw new IllegalStateException("Invalid stress storage transfer result");
        }
        return actual;
    }
}
