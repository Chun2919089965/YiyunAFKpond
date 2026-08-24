package com.yiyunafkpond.reward.item;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public final class WeightedPicker {
    private WeightedPicker() {
    }

    public static <T> T pick(List<T> values, Predicate<T> eligible,
                             ToDoubleFunction<T> weight, Random random) {
        double maxWeight = 0.0;
        for (T value : values) {
            double currentWeight = weight.applyAsDouble(value);
            if (eligible.test(value) && Double.isFinite(currentWeight) && currentWeight > 0.0) {
                maxWeight = Math.max(maxWeight, currentWeight);
            }
        }
        if (maxWeight <= 0.0) return null;

        double totalWeight = 0.0;
        for (T value : values) {
            double currentWeight = weight.applyAsDouble(value);
            if (eligible.test(value) && Double.isFinite(currentWeight) && currentWeight > 0.0) {
                totalWeight += currentWeight / maxWeight;
            }
        }

        double cursor = random.nextDouble() * totalWeight;
        T lastEligible = null;
        for (T value : values) {
            double currentWeight = weight.applyAsDouble(value);
            if (!eligible.test(value) || !Double.isFinite(currentWeight) || currentWeight <= 0.0) continue;
            lastEligible = value;
            cursor -= currentWeight / maxWeight;
            if (cursor < 0.0) return value;
        }
        return lastEligible;
    }
}
