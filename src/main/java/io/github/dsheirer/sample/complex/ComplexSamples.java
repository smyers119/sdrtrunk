package io.github.dsheirer.sample.complex;

/**
 * Wrapper for a complex sample array where I and Q samples are in separate arrays.
 */
public class ComplexSamples
{
    private float[] mISamples;
    private float[] mQSamples;

    /**
     * Constructs an instance
     * @param iSamples array
     * @param qSamples array
     */
    public ComplexSamples(float[] iSamples, float[] qSamples)
    {
        mISamples = iSamples;
        mQSamples = qSamples;
    }

    /**
     * I samples array
     * @return samples
     */
    public float[] getISamples()
    {
        return mISamples;
    }

    /**
     * Q samples array
     * @return samples
     */
    public float[] getQSamples()
    {
        return mQSamples;
    }
}
