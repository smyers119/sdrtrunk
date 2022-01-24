package io.github.dsheirer.dsp.fm;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating FM demodulators
 */
public class FmDemodulatorFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(FmDemodulatorFactory.class);

    /**
     * Creates the optimal FM demodulator using calibration data to select the optimal
     * implementation from scalar and vector options.
     * @return demodulator instance
     */
    public static IFmDemodulator getFmDemodulator()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.FM_DEMODULATOR);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
                return new VectorFMDemodulator();
            case SCALAR:
                return new FMDemodulator();
            default:
                mLog.warn("Unrecognized optimal operation for FM demodulator: " + implementation.name());
                return new FMDemodulator();
        }
    }

    /**
     * Creates the optimal Squelching FM demodulator using calibration data to select the optimal
     * implementation from scalar and vector options.
     * @return demodulator instance
     */
    public static ISquelchingFmDemodulator getSquelchingFmDemodulator(float alpha, float threshold, int ramp)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.FM_DEMODULATOR);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
                return new VectorSquelchingFMDemodulator(alpha, threshold, ramp);
            case SCALAR:
                return new SquelchingFMDemodulator(alpha, threshold, ramp);
            default:
                mLog.warn("Unrecognized optimal operation for Squelching FM demodulator: " + implementation.name());
                return new SquelchingFMDemodulator(alpha, threshold, ramp);
        }
    }
}
