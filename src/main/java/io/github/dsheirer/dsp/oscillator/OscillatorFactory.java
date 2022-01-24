package io.github.dsheirer.dsp.oscillator;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating real and complex oscillators.
 *
 * Uses the CalibrationManager to determine the optimal (scalar vs vector) oscillator implementation type.
 */
public class OscillatorFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(OscillatorFactory.class);

    /**
     * Constructs an optimal implementation of a real sample oscillator using calibration data when available.
     * @param frequency of the oscillator
     * @param sampleRate of the oscillator
     * @return constructed oscillator
     */
    public static IRealOscillator getRealOscillator(double frequency, double sampleRate)
    {
        Implementation operation = CalibrationManager.getInstance().getImplementation(CalibrationType.OSCILLATOR_REAL);

        switch(operation)
        {
            case VECTOR_SIMD_PREFERRED:
                return new VectorRealOscillator(frequency, sampleRate);
            case SCALAR:
                return new RealOscillator(frequency, sampleRate);
            default:
                mLog.info("Calibration not available for Real Oscillator.  Using default SCALAR implementation.  "
                        + "Perform calibration to identify optimal implementation type");
                return new RealOscillator(frequency, sampleRate);
        }
    }

    /**
     * Constructs an optimal implementation of a complex sample oscillator using calibration data when available.
     * @param frequency of the oscillator
     * @param sampleRate of the oscillator
     * @return constructed oscillator
     */
    public static IComplexOscillator getComplexOscillator(double frequency, double sampleRate)
    {
        Implementation operation = CalibrationManager.getInstance().getImplementation(CalibrationType.OSCILLATOR_COMPLEX);

        switch(operation)
        {
            case VECTOR_SIMD_PREFERRED:
                return new VectorComplexOscillator(frequency, sampleRate);
            case SCALAR:
                return new ComplexOscillator(frequency, sampleRate);
            default:
                mLog.info("Calibration not available for Complex Oscillator.  Using default SCALAR implementation.  "
                        + "Perform calibration to identify optimal implementation type");
                return new ComplexOscillator(frequency, sampleRate);
        }
    }
}
