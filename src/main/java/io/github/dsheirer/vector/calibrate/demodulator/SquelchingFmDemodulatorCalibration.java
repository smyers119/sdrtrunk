package io.github.dsheirer.vector.calibrate.demodulator;

import io.github.dsheirer.dsp.fm.SquelchingFMDemodulator;
import io.github.dsheirer.dsp.fm.VectorSquelchingFMDemodulator;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calibrates squelching FM demodulator options
 */
public class SquelchingFmDemodulatorCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(SquelchingFmDemodulatorCalibration.class);
    private static final int SAMPLE_BUFFER_SIZE = 2048;
    private static final int ITERATIONS = 100_000;
    private static final float POWER_SQUELCH_ALPHA_DECAY = 0.0004f;
    private static final float POWER_SQUELCH_THRESHOLD_DB = -78.0f;
    private static final int POWER_SQUELCH_RAMP = 4;


    /**
     * Constructs an instance
     */
    public SquelchingFmDemodulatorCalibration()
    {
        super(CalibrationType.SQUELCHING_FM_DEMODULATOR);
    }

    @Override public void calibrate() throws CalibrationException
    {
        long scalarScore = calculateScalar(SAMPLE_BUFFER_SIZE, ITERATIONS);
        mLog.info("SQUELCHING FM DEMODULATOR SCALAR:" + scalarScore);
        Implementation operation = Implementation.SCALAR;

        long vectorScore = calculateVector(SAMPLE_BUFFER_SIZE, ITERATIONS);
        mLog.info("SQUELCHING FM DEMODULATOR VECTOR PREFERRED:" + vectorScore);

        if(vectorScore < scalarScore)
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }
        else
        {
            setImplementation(Implementation.SCALAR);
        }

        mLog.info("SQUELCHING FM DEMODULATOR - SETTING OPTIMAL OPERATION TO:" + getImplementation());
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

        SquelchingFMDemodulator demodulator = new SquelchingFMDemodulator(POWER_SQUELCH_ALPHA_DECAY,
                POWER_SQUELCH_THRESHOLD_DB, POWER_SQUELCH_RAMP);

        float[] iSamples = getFloatSamples(sampleSize);
        float[] qSamples = getFloatSamples(sampleSize);

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

        VectorSquelchingFMDemodulator demodulator = new VectorSquelchingFMDemodulator(POWER_SQUELCH_ALPHA_DECAY,
                POWER_SQUELCH_THRESHOLD_DB, POWER_SQUELCH_RAMP);

        float[] iSamples = getFloatSamples(sampleSize);
        float[] qSamples = getFloatSamples(sampleSize);

        long start = System.currentTimeMillis();

        for(int i = 0; i < iterations; i++)
        {
            float[] demodulated = demodulator.demodulate(iSamples, qSamples);
            accumulator += demodulated[0];
        }

        return System.currentTimeMillis() - start;
    }
}
