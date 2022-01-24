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
    private Implementation mImplementation;

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
        return getImplementation() != Implementation.UNCALIBRATED;
    }

    /**
     * Resets calibration status to uncalibrated for the plugin
     */
    public void reset()
    {
        setImplementation(Implementation.UNCALIBRATED);
    }

    private String getImplementationKey()
    {
        return getType() + "-implementation";
    }

    /**
     * Optimal implementation for this plugin
     * @return implementation or uncalibrated if this plugin has not yet been calibrated
     */
    public Implementation getImplementation()
    {
        if(mImplementation == null)
        {
            String implementation = mPreferences.get(getImplementationKey(), Implementation.UNCALIBRATED.name());
            mImplementation = Implementation.valueOf(implementation);
        }

        return mImplementation;

    }

    /**
     * Sets the optimal implementation as determined via calibration.
     * @param implementation to set
     */
    protected void setImplementation(Implementation implementation)
    {
        mImplementation = implementation;
        mPreferences.put(getImplementationKey(), implementation.name());
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
