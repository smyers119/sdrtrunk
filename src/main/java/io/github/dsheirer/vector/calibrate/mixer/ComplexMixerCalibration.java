package io.github.dsheirer.vector.calibrate.mixer;

import io.github.dsheirer.dsp.mixer.ScalarComplexMixer;
import io.github.dsheirer.dsp.mixer.VectorComplexMixer;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calibrates the complex mixer implementations to determine the optimal instance.
 */
public class ComplexMixerCalibration extends Calibration
{
    private Logger mLog = LoggerFactory.getLogger(ComplexMixerCalibration.class);

    /**
     * Constructs an instance
     */
    public ComplexMixerCalibration()
    {
        super(CalibrationType.COMPLEX_MIXER);
    }

    @Override public void calibrate() throws CalibrationException
    {
        double frequency = 2.0;
        double sampleRate = 10.0;
        int sampleSize = 2048;
        int iterations = 10_000_000;

        ScalarComplexMixer scalar = new ScalarComplexMixer(frequency, sampleRate);
        VectorComplexMixer vector = new VectorComplexMixer(frequency, sampleRate);
        float[] i = getSamples(sampleSize);
        float[] q = getSamples(sampleSize);

        long start = System.currentTimeMillis();
        double accumulator = 0.0;
        for(int count = 0; count < iterations; count++)
        {
            ComplexSamples mixed = scalar.mix(i, q);
            accumulator += mixed.i()[2];
        }

        long scalarTime = System.currentTimeMillis() - start;
        mLog.info("COMPLEX MIXER - SCALAR:" + scalarTime);

        start = System.currentTimeMillis();
        accumulator = 0.0;

        for(int count = 0; count < iterations; count++)
        {
            ComplexSamples mixed = vector.mix(i, q);
            accumulator += mixed.i()[2];
        }

        long vectorTime = System.currentTimeMillis() - start;
        mLog.info("COMPLEX MIXER - VECTOR:" + vectorTime);

        if(scalarTime < vectorTime)
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("COMPLEX MIXER - IMPLEMENTATION SET TO:" + getImplementation());
    }
}
