package io.github.dsheirer.dsp.mixer;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory for creating complex mixer instances from scalar and vector implementations.
 */
public class ComplexMixerFactory
{
    /**
     * Creates an instance of the optimal implementation of a complex mixer for this hardware.
     * @param frequency of the mixing oscillator
     * @param sampleRate of the mixing oscillator
     * @return optimal instance
     */
    public static ComplexMixer getMixer(double frequency, double sampleRate)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.COMPLEX_MIXER);

        return new ScalarComplexMixer(frequency, sampleRate);
    }
}
