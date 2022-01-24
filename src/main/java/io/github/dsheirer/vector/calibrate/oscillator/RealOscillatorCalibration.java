package io.github.dsheirer.vector.calibrate.oscillator;

import io.github.dsheirer.dsp.oscillator.IRealOscillator;
import io.github.dsheirer.dsp.oscillator.RealOscillator;
import io.github.dsheirer.dsp.oscillator.VectorRealOscillator;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.OptimalOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calibration plugin for real oscillators
 */
public class RealOscillatorCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(RealOscillatorCalibration.class);
    private static final int ITERATIONS = 500_000;
    private static final int BUFFER_SIZE = 2048;
    private static final double FREQUENCY = 5.0d;
    private static final double SAMPLE_RATE = 100.0d;

    /**
     * Constructs an instance
     */
    public RealOscillatorCalibration()
    {
        super(CalibrationType.OSCILLATOR_REAL);
    }

    /**
     * Performs calibration to determine optimal (Scalar vs Vector) operation type.
     * @throws CalibrationException
     */
    @Override public void calibrate() throws CalibrationException
    {
        long scalar = calculateScalar(BUFFER_SIZE, ITERATIONS);
        mLog.info("REAL OSCILLATOR SCALAR:" + scalar);
        long vector = calculateVector(BUFFER_SIZE, ITERATIONS);
        mLog.info("REAL OSCILLATOR VECTOR:" + vector);

        if(scalar < vector)
        {
            setOptimalOperation(OptimalOperation.SCALAR);
        }
        else
        {
            setOptimalOperation(OptimalOperation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("REAL OSCILLATOR - SETTING OPTIMAL OPERATION TO:" + getOptimalOperation());
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

        IRealOscillator oscillator = new RealOscillator(FREQUENCY, SAMPLE_RATE);

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

        IRealOscillator oscillator = new VectorRealOscillator(FREQUENCY, SAMPLE_RATE);

        long start = System.currentTimeMillis();

        for(int i = 0; i < iterations; i++)
        {
            float[] generated = oscillator.generate(bufferSize);
            accumulator += generated[0];
        }

        return System.currentTimeMillis() - start;
    }
}
