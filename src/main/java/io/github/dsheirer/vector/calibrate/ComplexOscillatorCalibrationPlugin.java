package io.github.dsheirer.vector.calibrate;

import io.github.dsheirer.dsp.oscillator.ComplexOscillator;
import io.github.dsheirer.dsp.oscillator.IComplexOscillator;
import io.github.dsheirer.dsp.oscillator.IRealOscillator;
import io.github.dsheirer.dsp.oscillator.RealOscillator;
import io.github.dsheirer.dsp.oscillator.VectorComplexOscillator;
import io.github.dsheirer.dsp.oscillator.VectorRealOscillator;

/**
 * Calibration plugin for complex oscillators
 */
public class ComplexOscillatorCalibrationPlugin extends CalibrationPlugin
{
    private static final int ITERATIONS = 500_000;
    private static final int BUFFER_SIZE = 2048;
    private static final double FREQUENCY = 5.0d;
    private static final double SAMPLE_RATE = 100.0d;

    /**
     * Constructs an instance
     */
    public ComplexOscillatorCalibrationPlugin()
    {
        super(CalibrationPluginType.OSCILLATOR_COMPLEX);
    }

    /**
     * Performs calibration to determine optimal (Scalar vs Vector) operation type.
     * @throws CalibrationException
     */
    @Override public void calibrate() throws CalibrationException
    {
        long scalar = calculateScalar(BUFFER_SIZE, ITERATIONS);
        System.out.println("COMPLEX OSCILLATOR SCALAR:" + scalar);
        long vector = calculateVector(BUFFER_SIZE, ITERATIONS);
        System.out.println("COMPLEX OSCILLATOR VECTOR:" + vector);

        if(scalar < vector)
        {
            setOptimalOperation(OptimalOperation.SCALAR);
        }
        else
        {
            setOptimalOperation(OptimalOperation.VECTOR_SIMD_PREFERRED);
        }

        System.out.println("Set Complex Oscillator optimal operation to: " + getOptimalOperation());

    }

    /**
     * Calculates the time duration needed to generate the sample buffers of the specified size and iteration count
     * @param bufferSize for size of buffer.
     * @param iterations for number of buffers to generate
     * @return time duration in milliseconds
     */
    private static long calculateScalar(int bufferSize, int iterations)
    {
        float accumulator = 0.0f;

        long start = System.currentTimeMillis();

        IComplexOscillator oscillator = new ComplexOscillator(FREQUENCY, SAMPLE_RATE);

        for(int i = 0; i < iterations; i++)
        {
            float[] generated = oscillator.generate(BUFFER_SIZE);
            accumulator += generated[0];
        }

        return System.currentTimeMillis() - start;
    }

    /**
     * Calculates the time duration to process the sample buffer with the filter coefficients.
     * @param bufferSize size of each sample buffer
     * @param iterations count of how many times to generate the samples buffer
     * @return time duration in milliseconds
     */
    private static long calculateVector(int bufferSize, int iterations)
    {
        float accumulator = 0.0f;

        IComplexOscillator oscillator = new VectorComplexOscillator(FREQUENCY, SAMPLE_RATE);

        long start = System.currentTimeMillis();

        for(int i = 0; i < iterations; i++)
        {
            float[] generated = oscillator.generate(bufferSize);
            accumulator += generated[0];
        }

        return System.currentTimeMillis() - start;
    }
}
