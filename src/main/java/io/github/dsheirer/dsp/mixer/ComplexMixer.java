package io.github.dsheirer.dsp.mixer;

import io.github.dsheirer.dsp.oscillator.IComplexOscillator;
import io.github.dsheirer.dsp.oscillator.OscillatorFactory;
import io.github.dsheirer.sample.complex.ComplexSamples;

/**
 * Base complex mixer that wraps an oscillator implementation and mixes complex sample buffers
 */
public abstract class ComplexMixer
{
    private IComplexOscillator mOscillator;

    public ComplexMixer(double frequency, double sampleRate)
    {
        mOscillator = OscillatorFactory.getComplexOscillator(frequency, sampleRate);
    }

    /**
     * Sets the frequency of the underlying oscillator
     * @param frequency in Hertz
     */
    public void setFrequency(double frequency)
    {
        mOscillator.setFrequency(frequency);
    }

    /**
     * Sets the sample rate of the underlying oscillator
     * @param sampleRate in Hertz
     */
    public void setSampleRate(double sampleRate)
    {
        mOscillator.setSampleRate(sampleRate);
    }

    /**
     * Generates complex samples from the underlying oscillator
     * @param sampleCount to generate
     * @return complex samples
     */
    protected ComplexSamples generate(int sampleCount)
    {
        return mOscillator.generateComplexSamples(sampleCount);
    }

    /**
     * Mixes the complex I & Q samples with samples generated from an oscillator.
     * @param samples to mix
     * @return mixed samples
     */
    public ComplexSamples mix(ComplexSamples samples)
    {
        return mix(samples.i(), samples.q());
    }

    /**
     * Mixes the complex I & Q samples with samples generated from an oscillator.
     * @param i complex samples to mix
     * @param q complex samples to mix
     * @return mixed samples
     */
    public abstract ComplexSamples mix(float[] i, float[] q);
}
