package io.github.dsheirer.sample.complex;

/**
 * Wrapper for a complex sample array where I and Q samples are in separate arrays.
 */
public record ComplexSamples(float[] i, float[] q) {}

