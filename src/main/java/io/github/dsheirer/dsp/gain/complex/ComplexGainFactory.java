package io.github.dsheirer.dsp.gain.complex;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory for selecting and creating the optimal implementation of complex gain
 * control for this hardware.
 */
public class ComplexGainFactory
{
    /**
     * Instantiates the optimal complex gain control implementation for this hardware,
     * as determined by the Calibration Manager.
     */
    public static IComplexGainControl getGainControl()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.COMPLEX_GAIN);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
            {
                return new VectorComplexGainControl();
            }
            case SCALAR:
            default:
                return new ComplexGainControl();
        }
    }
}
