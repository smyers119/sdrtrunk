package io.github.dsheirer.vector.calibrate;

import java.util.Random;
import java.util.prefs.Preferences;

/**
 * Abstract calibration plugin base class
 */
public abstract class Calibration
{
    private Preferences mPreferences = Preferences.userNodeForPackage(Calibration.class);
    private CalibrationType mType;
    private OptimalOperation mOptimalOperation;

    /**
     * Constructs an instance
     * @param type
     */
    public Calibration(CalibrationType type)
    {
        mType = type;
    }

    /**
     * Type of calibration plugin
     */
    public CalibrationType getType()
    {
        return mType;
    }

    /**
     * Indicates if this plugin is calibrated.
     */
    public boolean isCalibrated()
    {
        return getOptimalOperation() != OptimalOperation.UNCALIBRATED;
    }

    /**
     * Resets calibration status to uncalibrated for the plugin
     */
    public void reset()
    {
        setOptimalOperation(OptimalOperation.UNCALIBRATED);
    }

    private String getOperationKey()
    {
        return getType() + "-operation";
    }

    /**
     * Optimal operation for this plugin
     * @return operation or uncalibrated if this plugin has not yet been calibrated
     */
    public OptimalOperation getOptimalOperation()
    {
        if(mOptimalOperation == null)
        {
            String operation = mPreferences.get(getOperationKey(), OptimalOperation.UNCALIBRATED.name());
            mOptimalOperation = OptimalOperation.valueOf(operation);
        }

        return mOptimalOperation;

    }

    /**
     * Sets the optimal operation as determined via calibration.
     * @param optimalOperation to set
     */
    protected void setOptimalOperation(OptimalOperation optimalOperation)
    {
        mOptimalOperation = optimalOperation;
        mPreferences.put(getOperationKey(), optimalOperation.name());
    }

    /**
     * Executes calibration for the plugin
     */
    public abstract void calibrate() throws CalibrationException;

    /**
     * Generates an array of floating point samples in the range -1.0 - 1.0
     * @param size of array
     * @return generated samples
     */
    protected float[] getSamples(int size)
    {
        Random random = new Random();

        float[] samples = new float[size];
        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = random.nextFloat() * 2.0f - 1.0f;
        }

        return samples;
    }
}
