package io.github.dsheirer.vector.calibrate.gain;

import io.github.dsheirer.dsp.gain.complex.ComplexGainControl;
import io.github.dsheirer.dsp.gain.complex.IComplexGainControl;
import io.github.dsheirer.dsp.gain.complex.VectorComplexGainControl;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines the optimal scalar vs vector implementation of complex gain control.
 */
public class ComplexGainCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(ComplexGainCalibration.class);
    private static final int ITERATIONS = 5_000_000;
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
        float[] i = getSamples(SAMPLE_BUFFER_SIZE);
        float[] q = getSamples(SAMPLE_BUFFER_SIZE);

        float accumulator = 0.0f;

        IComplexGainControl scalar = new ComplexGainControl();
        IComplexGainControl vector = new VectorComplexGainControl();

        long start = System.currentTimeMillis();

        for(int scalarCount = 0; scalarCount < ITERATIONS; scalarCount++)
        {
            ComplexSamples amplified = scalar.process(i, q);
            accumulator += amplified.i()[2];
        }

        long scalarDuration = System.currentTimeMillis() - start;
        mLog.info("COMPLEX GAIN CONTROL - SCALAR:" + scalarDuration);

        start = System.currentTimeMillis();
        accumulator = 0.0f;

        for(int vectorCount = 0; vectorCount < ITERATIONS; vectorCount++)
        {
            ComplexSamples amplified = vector.process(i, q);
            accumulator += amplified.i()[2];
        }

        long vectorDuration = System.currentTimeMillis() - start;
        mLog.info("COMPLEX GAIN CONTROL - VECTOR:" + vectorDuration);

        if(scalarDuration < vectorDuration)
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("COMPLEX GAIN CONTROL - SETTING IMPLEMENTATION TO:" + getImplementation());
    }
}
