package io.github.dsheirer.vector.calibrate;

import io.github.dsheirer.vector.calibrate.demodulator.FmDemodulatorCalibration;
import io.github.dsheirer.vector.calibrate.demodulator.SquelchingFmDemodulatorCalibration;
import io.github.dsheirer.vector.calibrate.filter.ComplexHalfBand11TapFilterCalibration;
import io.github.dsheirer.vector.calibrate.filter.ComplexHalfBand15TapFilterCalibration;
import io.github.dsheirer.vector.calibrate.filter.FirFilterCalibration;
import io.github.dsheirer.vector.calibrate.filter.RealHalfBand11TapFilterCalibration;
import io.github.dsheirer.vector.calibrate.filter.RealHalfBand15TapFilterCalibration;
import io.github.dsheirer.vector.calibrate.filter.RealHalfBand23TapFilterCalibration;
import io.github.dsheirer.vector.calibrate.filter.RealHalfBand63TapFilterCalibration;
import io.github.dsheirer.vector.calibrate.filter.RealHalfBandDefaultFilterCalibration;
import io.github.dsheirer.vector.calibrate.oscillator.ComplexOscillatorCalibration;
import io.github.dsheirer.vector.calibrate.oscillator.RealOscillatorCalibration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Determines the optimal (scalar vs vector) class to use for the current CPU architecture.
 */
public class CalibrationManager
{
    private static final Logger mLog = LoggerFactory.getLogger(CalibrationManager.class);
    private Map<CalibrationType, Calibration> mCalibrationMap = new HashMap<>();
    private static CalibrationManager sInstance;

    /**
     * Uses the singleton pattern to construct a single instance.
     */
    private CalibrationManager()
    {
        add(new ComplexHalfBand11TapFilterCalibration());
        add(new ComplexHalfBand15TapFilterCalibration());
        add(new ComplexOscillatorCalibration());
        add(new FirFilterCalibration());
        add(new FmDemodulatorCalibration());
        add(new RealHalfBand11TapFilterCalibration());
        add(new RealHalfBand15TapFilterCalibration());
        add(new RealHalfBand23TapFilterCalibration());
        add(new RealHalfBand63TapFilterCalibration());
        add(new RealHalfBandDefaultFilterCalibration());
        add(new RealOscillatorCalibration());
        add(new SquelchingFmDemodulatorCalibration());
    }

    /**
     * Access a singleton instance of this class.
     */
    public static CalibrationManager getInstance()
    {
        if(sInstance == null)
        {
            sInstance = new CalibrationManager();
        }

        return sInstance;
    }

    /**
     * Adds the calibration to the map of calibrations for this manager.
      * @param calibration to add
     */
    private void add(Calibration calibration)
    {
        mCalibrationMap.put(calibration.getType(), calibration);
    }

    /**
     * Access a calibration by type
     * @param type of calibration
     * @return calibration instance, or null.
     */
    public Calibration getCalibration(CalibrationType type)
    {
        return mCalibrationMap.get(type);
    }

    /**
     * Identifies the optimal operation for the calibration type.
     * @param type of calibration
     * @return operation
     */
    public OptimalOperation getOperation(CalibrationType type)
    {
        Calibration calibration = getCalibration(type);

        if(calibration != null)
        {
            return calibration.getOptimalOperation();
        }

        return OptimalOperation.UNCALIBRATED;
    }

    /**
     * Indicates if all calibrations are calibrated.
     * @return true if all calibrations are calibrated or if there are no calibrations registered.
     */
    public boolean isCalibrated()
    {
        for(Calibration calibration: mCalibrationMap.values())
        {
            if(!calibration.isCalibrated())
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Resets all calibrations to an uncalibrated state.
     */
    public void reset()
    {
        for(Calibration calibration: mCalibrationMap.values())
        {
            calibration.reset();
        }
    }

    /**
     * Resets a specific calibration type.
     */
    public void reset(CalibrationType type)
    {
        Calibration calibration = mCalibrationMap.get(type);

        if(calibration != null)
        {
            calibration.reset();
        }
    }

    /**
     * Calibrates any calibrations that are not currently calibrated
     * @throws CalibrationException if any errors are encountered by any of the calibrations.
     */
    public void calibrate() throws CalibrationException
    {
        for(Calibration calibration: mCalibrationMap.values())
        {
            if(!calibration.isCalibrated())
            {
                mLog.info("====> Calibrating: " + calibration.getType());
                calibration.calibrate();
            }
        }
    }

    public static void main(String[] args)
    {
        CalibrationManager manager = getInstance();

        manager.reset(CalibrationType.FILTER_HALF_BAND_REAL_DEFAULT);
//        manager.reset();

        if(!manager.isCalibrated())
        {
            try
            {
                manager.calibrate();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        System.out.println("Complete");
    }
}
