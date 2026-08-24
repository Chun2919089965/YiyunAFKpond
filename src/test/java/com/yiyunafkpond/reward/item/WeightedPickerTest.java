package com.yiyunafkpond.reward.item;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WeightedPickerTest {
    private record Candidate(String id, boolean enabled, double weight) {
    }

    @Test
    void ignoresDisabledAndNonPositiveEntries() {
        List<Candidate> candidates = List.of(
                new Candidate("disabled", false, 100.0),
                new Candidate("zero", true, 0.0),
                new Candidate("winner", true, 1.0)
        );

        Candidate selected = WeightedPicker.pick(candidates, Candidate::enabled,
                Candidate::weight, new FixedRandom(0.0));

        assertEquals("winner", selected.id());
    }

    @Test
    void returnsNullWhenNoEntryIsEligible() {
        List<Candidate> candidates = List.of(
                new Candidate("disabled", false, 1.0),
                new Candidate("zero", true, 0.0),
                new Candidate("nan", true, Double.NaN)
        );

        assertNull(WeightedPicker.pick(candidates, Candidate::enabled,
                Candidate::weight, new FixedRandom(0.5)));
    }

    @Test
    void selectsAcrossWeightBoundariesDeterministically() {
        List<Candidate> candidates = List.of(
                new Candidate("common", true, 3.0),
                new Candidate("rare", true, 1.0)
        );

        assertEquals("common", WeightedPicker.pick(candidates, Candidate::enabled,
                Candidate::weight, new FixedRandom(0.749999)).id());
        assertEquals("rare", WeightedPicker.pick(candidates, Candidate::enabled,
                Candidate::weight, new FixedRandom(0.75)).id());
    }

    @Test
    void handlesFiniteWeightsWhoseRawSumWouldOverflow() {
        List<Candidate> candidates = List.of(
                new Candidate("first", true, Double.MAX_VALUE),
                new Candidate("second", true, Double.MAX_VALUE)
        );

        assertEquals("first", WeightedPicker.pick(candidates, Candidate::enabled,
                Candidate::weight, new FixedRandom(0.25)).id());
        assertEquals("second", WeightedPicker.pick(candidates, Candidate::enabled,
                Candidate::weight, new FixedRandom(0.75)).id());
    }

    private static final class FixedRandom extends Random {
        private final double value;

        private FixedRandom(double value) {
            this.value = value;
        }

        @Override
        public double nextDouble() {
            return value;
        }
    }
}
