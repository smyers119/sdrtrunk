package io.github.dsheirer.vector.calibrate.gain;

import io.github.dsheirer.dsp.gain.complex.ComplexGain;
import io.github.dsheirer.dsp.gain.complex.ScalarComplexGain;
import io.github.dsheirer.dsp.gain.complex.VectorComplexGain;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines the optimal scalar vs vector implementation of complex gain.
 */
public class ComplexGainCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(ComplexGainCalibration.class);
    private static final int ITERATIONS = 1_000_000;
    private static final int SAMPLE_BUFFER_SIZE = 2048;

    /**
     * Constructs an instance
     */
    public ComplexGainCalibration()
    {
        super(CalibrationType.COMPLEX_GAIN);
    }

    @Override public void calibrate() throws CalibrationException
    {
        float[] i = getFloatSamples(SAMPLE_BUFFER_SIZE);
        float[] q = getFloatSamples(SAMPLE_BUFFER_SIZE);

        float accumulator = 0.0f;
        float gain = 0.75f;

        ComplexGain scalar = new ScalarComplexGain(gain);
        ComplexGain vector = new VectorComplexGain(gain);

        long start = System.currentTimeMillis();

        for(int scalarCount = 0; scalarCount < ITERATIONS; scalarCount++)
        {
            ComplexSamples amplified = scalar.apply(i, q);
            accumulator += amplified.i()[2];
        }

        long scalarDuration = System.currentTimeMillis() - start;
        mLog.info("COMPLEX GAIN - SCALAR:" + scalarDuration);

        start = System.currentTimeMillis();
        accumulator = 0.0f;

        for(int vectorCount = 0; vectorCount < ITERATIONS; vectorCount++)
        {
            ComplexSamples amplified = vector.apply(i, q);
            accumulator += amplified.i()[2];
        }

        long vectorDuration = System.currentTimeMillis() - start;
        mLog.info("COMPLEX GAIN - VECTOR:" + vectorDuration);

        if(scalarDuration < vectorDuration)
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("COMPLEX GAIN - SETTING IMPLEMENTATION TO:" + getImplementation());
    }
}
