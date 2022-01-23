package io.github.dsheirer.vector.calibrate;

import java.util.HashMap;
import java.util.Map;

/**
 * Determines the optimal (scalar vs vector) class to use for the current CPU architecture.
 */
public class CalibrationManager
{
    private Map<CalibrationPluginType, CalibrationPlugin> mPluginMap = new HashMap<>();
    private static CalibrationManager sInstance;

    /**
     * Uses the singleton pattern to construct a single instance.
     */
    private CalibrationManager()
    {
        addPlugin(new FirFilterCalibrationPlugin());
        addPlugin(new ComplexOscillatorCalibrationPlugin());
        addPlugin(new RealOscillatorCalibrationPlugin());
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
     * Adds the plugin to the map of plugins for this manager.
      * @param plugin to add
     */
    private void addPlugin(CalibrationPlugin plugin)
    {
        mPluginMap.put(plugin.getType(), plugin);
    }

    /**
     * Access a plugin by type
     * @param type of plugin
     * @return plugin instance, or null.
     */
    public CalibrationPlugin getPlugin(CalibrationPluginType type)
    {
        return mPluginMap.get(type);
    }

    /**
     * Identifies the optimal operation for the plugin type.
     * @param type of plugin
     * @return operation
     */
    public OptimalOperation getOperation(CalibrationPluginType type)
    {
        CalibrationPlugin plugin = getPlugin(type);

        if(plugin != null)
        {
            return plugin.getOptimalOperation();
        }

        return OptimalOperation.UNCALIBRATED;
    }

    /**
     * Indicates if all plugins are calibrated.
     * @return true if all plugins are calibrated or if there are no plugins.
     */
    public boolean isCalibrated()
    {
        for(CalibrationPlugin plugin: mPluginMap.values())
        {
            if(!plugin.isCalibrated())
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Resets all plugins to an uncalibrated state.
     */
    public void reset()
    {
        for(CalibrationPlugin plugin: mPluginMap.values())
        {
            plugin.reset();
        }
    }

    /**
     * Resets a specific calibration plugin type.
     */
    public void reset(CalibrationPluginType type)
    {
        CalibrationPlugin plugin = mPluginMap.get(type);

        if(plugin != null)
        {
            plugin.reset();
        }
    }

    /**
     * Calibrates any plugins that are not currently calibrated
     * @throws CalibrationException if any errors are encountered by any of the plugins.
     */
    public void calibrate() throws CalibrationException
    {
        for(CalibrationPlugin plugin: mPluginMap.values())
        {
            if(!plugin.isCalibrated())
            {
                plugin.calibrate();
            }
        }
    }

    public static void main(String[] args)
    {
        CalibrationManager manager = getInstance();

        manager.reset(CalibrationPluginType.OSCILLATOR_COMPLEX);

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

        OptimalOperation firOperation = manager.getOperation(CalibrationPluginType.FILTER_FIR);
        System.out.println("FIR FILTER: " + firOperation);
    }
}
