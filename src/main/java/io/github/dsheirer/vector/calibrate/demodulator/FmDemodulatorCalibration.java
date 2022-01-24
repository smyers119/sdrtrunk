package io.github.dsheirer.vector.calibrate.demodulator;

import io.github.dsheirer.dsp.fm.FMDemodulator;
import io.github.dsheirer.dsp.fm.IFmDemodulator;
import io.github.dsheirer.dsp.fm.VectorFMDemodulator;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calibrates FM demodulator options
 */
public class FmDemodulatorCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(FmDemodulatorCalibration.class);
    private static final int SAMPLE_BUFFER_SIZE = 2048;
    private static final int ITERATIONS = 100_000;

    /**
     * Constructs an instance
     */
    public FmDemodulatorCalibration()
    {
        super(CalibrationType.FM_DEMODULATOR);
    }

    @Override public void calibrate() throws CalibrationException
    {
        long scalarScore = calculateScalar(SAMPLE_BUFFER_SIZE, ITERATIONS);
        mLog.info("FM DEMODULATOR SCALAR:" + scalarScore);
        Implementation operation = Implementation.SCALAR;

        long vectorScore = calculateVector(SAMPLE_BUFFER_SIZE, ITERATIONS);
        mLog.info("FM DEMODULATOR VECTOR PREFERRED:" + vectorScore);

        if(vectorScore < scalarScore)
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }
        else
        {
            setImplementation(Implementation.SCALAR);
        }

        mLog.info("FM DEMODULATOR - SETTING OPTIMAL OPERATION TO:" + getImplementation());
    }

    /**
     * Calculates the time duration to process the sample buffer using scalar implementation.
     * @param sampleSize of buffers
     * @param iterations count of how many times to (re)filter the samples buffer
     * @return time duration in milliseconds
     */
    private long calculateScalar(int sampleSize, int iterations)
    {
        float accumulator = 0.0f;

        IFmDemodulator demodulator = new FMDemodulator();

        float[] iSamples = getSamples(sampleSize);
        float[] qSamples = getSamples(sampleSize);

        long start = System.currentTimeMillis();

        for(int i = 0; i < iterations; i++)
        {
            float[] demodulated = demodulator.demodulate(iSamples, qSamples);
            accumulator += demodulated[0];
        }

        return System.currentTimeMillis() - start;
    }

    /**
     * Calculates the time duration to process the sample buffer using scalar implementation.
     * @param sampleSize of buffers
     * @param iterations count of how many times to (re)filter the samples buffer
     * @return time duration in milliseconds
     */
    private long calculateVector(int sampleSize, int iterations)
    {
        float accumulator = 0.0f;

        IFmDemodulator demodulator = new VectorFMDemodulator();

        float[] iSamples = getSamples(sampleSize);
        float[] qSamples = getSamples(sampleSize);

        long start = System.currentTimeMillis();

        for(int i = 0; i < iterations; i++)
        {
            float[] demodulated = demodulator.demodulate(iSamples, qSamples);
            accumulator += demodulated[0];
        }

        return System.currentTimeMillis() - start;
    }
}
