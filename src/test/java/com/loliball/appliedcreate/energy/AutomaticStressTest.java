package com.loliball.appliedcreate.energy;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AutomaticStressTest {
    @Test
    void idleRotationHasNeitherCapacityCostNorHarvestableSurplus() {
        assertEquals(0, StressDistribution.deficit(0, 0));
        assertEquals(0, StressDistribution.surplus(0, 0));
        StressBudget budget = new StressBudget();
        assertEquals(0, budget.reserve(1, 0, requested -> fail("Idle rotation withdrew stress")));
        budget.commit(1, 0);
    }

    @Test
    void addingMachinesChangesTheNetDirection() {
        assertEquals(4_000, StressDistribution.surplus(10_000, 6_000));
        assertEquals(0, StressDistribution.deficit(10_000, 6_000));
        assertEquals(0, StressDistribution.surplus(10_000, 12_000));
        assertEquals(2_000, StressDistribution.deficit(10_000, 12_000));
    }

    @Test
    void repeatedNetworkUpdatesDoNotChargeAgain() {
        AtomicLong storage = new AtomicLong(1_000);
        StressBudget budget = new StressBudget();
        for (int i = 0; i < 100; i++) {
            assertEquals(100, budget.reserve(1, 100, requested -> take(storage, requested)));
            budget.commit(1, 100);
        }
        assertEquals(900, storage.get());
        assertEquals(0, budget.stored());
    }

    @Test
    void addingRemovingAndReaddingLoadOnlyChargesTheHighWaterMark() {
        AtomicLong storage = new AtomicLong(1_000);
        StressBudget budget = new StressBudget();
        for (long load : new long[]{100, 300, 20, 200, 400, 0, 400}) {
            budget.reserve(9, load, requested -> take(storage, requested));
            budget.commit(9, load);
        }
        assertEquals(600, storage.get());
        assertEquals(400, budget.available(9));
    }

    @Test
    void nextTickNeedsNewPayment() {
        AtomicLong storage = new AtomicLong(200);
        StressBudget budget = new StressBudget();
        for (int tick = 1; tick <= 2; tick++) {
            budget.reserve(tick, 100, requested -> take(storage, requested));
            budget.commit(tick, 100);
        }
        assertEquals(0, budget.reserve(3, 100, requested -> take(storage, requested)));
        assertThrows(IllegalArgumentException.class, () -> budget.commit(3, 100));
    }

    @Test
    void failedJointSupplyKeepsTheActualWithdrawalAndRefundsIt() {
        AtomicLong storage = new AtomicLong(50);
        StressBudget budget = new StressBudget();
        assertEquals(50, budget.reserve(1, 100, requested -> take(storage, requested)));
        assertThrows(IllegalArgumentException.class, () -> budget.commit(1, 100));
        assertEquals(50, budget.stored());
        assertEquals(50, budget.refund(amount -> { storage.addAndGet(amount); return amount; }));
        assertEquals(50, storage.get());
        assertEquals(0, budget.stored());
    }

    @Test
    void rejectedRefundSurvivesSaveAndReloadWithoutRestoringSpentCapacity() {
        StressBudget budget = new StressBudget();
        budget.reserve(1, 100, amount -> amount);
        budget.commit(1, 40);
        assertEquals(0, budget.refund(amount -> 0));
        StressBudget reloaded = new StressBudget();
        reloaded.restore(budget.stored());
        assertEquals(60, reloaded.available(2));
        assertEquals(20, reloaded.refund(amount -> 20));
        assertEquals(40, reloaded.stored());
        assertThrows(IllegalArgumentException.class, () -> reloaded.commit(2, 100));
    }

    @Test
    void portsOnTheSameGridCannotSpendItsLastUnitsTwice() {
        AtomicLong shared = new AtomicLong(100);
        StressBudget[] ports = {new StressBudget(), new StressBudget(), new StressBudget()};
        long[] assigned = StressDistribution.allocate(300, new long[]{300, 300, 300}, (i, previous, requested) ->
                ports[i].reserve(1, previous + requested, amount -> take(shared, amount)) - previous);
        assertEquals(100, Arrays.stream(assigned).sum());
        assertEquals(0, shared.get());
        assertEquals(100, Arrays.stream(ports).mapToLong(StressBudget::stored).sum());
    }

    @Test
    void fullReceiversRedistributeTheirShareWithoutOverfillingSharedStorage() {
        AtomicLong space = new AtomicLong(1_000);
        long[] assigned = StressDistribution.allocate(900, new long[]{1_000, 1_000, 1_000}, (i, previous, requested) ->
                i == 0 ? 0 : take(space, requested));
        assertArrayEquals(new long[]{0, 450, 450}, assigned);
        assertEquals(100, space.get());
    }

    @Test
    void sourceLimitsAndPartialTransfersRedistributeTheRemainder() {
        long[] assigned = StressDistribution.allocate(1_000, new long[]{100, 1_000, 1_000},
                (i, previous, requested) -> i == 1 ? Math.min(50, requested) : requested);
        assertArrayEquals(new long[]{100, 50, 850}, assigned);
    }

    @Test
    void sharingIsFairWithinOneUnitWhenAllPortsCanParticipate() {
        assertArrayEquals(new long[]{334, 334, 333},
                StressDistribution.allocate(1_001, new long[]{2_000, 2_000, 2_000}, (i, old, wanted) -> wanted));
    }

    @Test
    void fractionalRoundingCannotCreateStressOnARoundTrip() {
        for (double stress : new double[]{0.01, 0.5, 1.01, 2.99999, 16_384.125}) {
            long paid = StressDistribution.deficit(0, stress);
            long collected = StressDistribution.surplus(stress, 0);
            assertTrue(paid >= stress);
            assertTrue(collected <= stress);
            assertTrue(collected <= paid);
        }
    }

    @Test
    void exactLongLimitDoesNotOverflowTheFairShareCalculation() {
        long[] assigned = StressDistribution.allocate(Long.MAX_VALUE,
                new long[]{Long.MAX_VALUE, Long.MAX_VALUE}, (i, old, wanted) -> wanted);
        assertEquals(Long.MAX_VALUE, Math.addExact(assigned[0], assigned[1]));
    }

    @Test
    void invalidTransferResultsCannotAdvertiseCapacity() {
        StressBudget budget = new StressBudget();
        assertThrows(IllegalStateException.class, () -> budget.reserve(1, 10, n -> 11));
        assertThrows(IllegalStateException.class, () -> budget.reserve(1, 10, n -> -1));
        assertEquals(0, budget.stored());
    }

    @Test
    void randomizedReservationsConserveStorageBufferAndConsumedUnits() {
        Random random = new Random(0xAEE2);
        AtomicLong storage = new AtomicLong(1_000_000);
        StressBudget budget = new StressBudget();
        long consumed = 0;
        for (int tick = 1; tick <= 1_000; tick++) {
            long peak = 0;
            for (int update = 0; update < 8; update++) {
                long target = random.nextInt(1_000);
                long available = budget.reserve(tick, target, amount -> take(storage, amount));
                if (available >= target) {
                    budget.commit(tick, target);
                    peak = Math.max(peak, target);
                }
            }
            consumed += peak;
            budget.refund(amount -> { storage.addAndGet(amount); return amount; });
            assertEquals(1_000_000, storage.get() + budget.stored() + consumed);
        }
    }

    private static long take(AtomicLong balance, long requested) {
        long actual = Math.min(balance.get(), requested);
        balance.addAndGet(-actual);
        return actual;
    }
}
