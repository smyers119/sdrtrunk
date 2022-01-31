package io.github.dsheirer.dsp.mixer;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating complex mixer instances from scalar and vector implementations.
 */
public class ComplexMixerFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(ComplexMixerFactory.class);

    /**
     * Creates an instance of the optimal implementation of a complex mixer for this hardware.
     * @param frequency of the mixing oscillator
     * @param sampleRate of the mixing oscillator
     * @return optimal instance
     */
    public static ComplexMixer getMixer(double frequency, double sampleRate)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.COMPLEX_MIXER);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
                {
                    return new VectorComplexMixer(frequency, sampleRate);
                }
            case SCALAR:
                {
                    return new ScalarComplexMixer(frequency, sampleRate);
                }
            default:
                {
                    mLog.warn("Unrecognized complex mixer implementation: " + implementation);
                    return new ScalarComplexMixer(frequency, sampleRate);
                }
        }
    }
}
