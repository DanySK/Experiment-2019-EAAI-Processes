/*
 * Copyright (C) 2010-2019, Danilo Pianini and contributors listed in the main project's alchemist/build.gradle file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.loader.variables;

import org.apache.commons.math3.util.FastMath;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A variable ranging geometrically (exponentially) in a range. Ideal for log-scale comparisons.
 *
 * e.g. a {@link GeometricVariable} with minimum = 1, maximum = 100 and samples = 5 will range over [1, ~3.16, 10, ~31.62 100].
 *
 * Both min and max must be strictly bigger than 0.
 */
public class GeometricVariable<V> extends PrintableVariable<Double> {

    private static final long serialVersionUID = 1L;
    private static final int MAXIMUM_DECIMALS = 16;
    private final boolean shouldRescale;
    private final double defaultValue;
    private final double scale;
    private final double min, max;
    private final int samples;

    /**
     * @param defaultValue
     *            default value
     * @param min
     *            minimum value
     * @param max
     *            maximum value
     * @param samples
     *            number of samples (must be bigger than zero)
     */
    public GeometricVariable(final double defaultValue, final double min, final double max, final int samples, final int decimals) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") can't be bigger than max (" + max + ")");
        }
        if (min <= 0d || max <= 0) {
            throw new IllegalArgumentException("Both minimum and maximum must be bigger than 0 for a geometric variable to work.");
        }
        if (samples <= 0) {
            throw new IllegalArgumentException("At least one sample is required.");
        }
        if (decimals < 0 || decimals > MAXIMUM_DECIMALS) {
            throw new IllegalArgumentException("Decimals must be between 0 and " + MAXIMUM_DECIMALS);
        }
        if (min == max && samples != 1) {
            throw new IllegalArgumentException("Only a single sample can be produced if min and max are exactly equal. (min="
                    + min + ", max=" + max + ", samples=" + samples);
        }
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.samples = samples;
        shouldRescale = decimals != MAXIMUM_DECIMALS;
        this.scale = Math.pow(10, decimals);
    }

    /**
     * @param defaultValue
     *            default value
     * @param min
     *            minimum value
     * @param max
     *            maximum value
     * @param samples
     *            number of samples (must be bigger than zero)
     */
    public GeometricVariable(final double defaultValue, final double min, final double max, final int samples) {
        this(defaultValue, min, max, samples, MAXIMUM_DECIMALS);
    }

    @Override
    public Double getDefault() {
        return defaultValue;
    }

    @Override
    public Stream<Double> stream() {
        var base = IntStream.range(0, samples)
                .mapToDouble(s -> min * FastMath.pow(max / min, (double) s / Math.max(1, samples - 1)));
        if (shouldRescale) {
            base = base.map(it -> Math.round(it * scale) / scale).distinct();
        }
        return base.boxed();
    }

    @Override
    public String toString() {
        return '[' + stream().map(Object::toString).collect(Collectors.joining(",")) + ']';
    }

}
