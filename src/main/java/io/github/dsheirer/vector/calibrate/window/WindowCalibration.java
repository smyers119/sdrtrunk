/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.vector.calibrate.window;

import io.github.dsheirer.dsp.window.ScalarWindow;
import io.github.dsheirer.dsp.window.VectorWindow;
import io.github.dsheirer.dsp.window.WindowFactory;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates the optimal (scalar vs vector) implementation for windowing samples.
 */
public class WindowCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(WindowCalibration.class);
    private static final int WINDOW_SIZE = 8192;
    private static final int ITERATIONS = 10_000_000;

    /**
     * Constructs an instance
     */
    public WindowCalibration()
    {
        super(CalibrationType.WINDOW);
    }

    @Override public void calibrate() throws CalibrationException
    {
        float[] window = WindowFactory.getBlackman(WINDOW_SIZE);
        float[] scalarSamples = getFloatSamples(WINDOW_SIZE);
        float[] vectorSamples = getFloatSamples(WINDOW_SIZE);
        ScalarWindow scalar = new ScalarWindow(window);
        VectorWindow vector = new VectorWindow(window);

        long start = System.currentTimeMillis();
        double accumulator = 0.0d;

        for(int x = 0; x < ITERATIONS; x++)
        {
            scalar.apply(scalarSamples);
            accumulator += scalarSamples[3];
        }

        long scalarElapsed = System.currentTimeMillis() - start;
        mLog.info("WINDOW - SCALAR: " + scalarElapsed);

        start = System.currentTimeMillis();
        accumulator = 0.0d;

        for(int x = 0; x < ITERATIONS; x++)
        {
            vector.apply(vectorSamples);
            accumulator += vectorSamples[2];
        }

        long vectorElapsed = System.currentTimeMillis() - start;
        mLog.info("WINDOW - VECTOR: " + vectorElapsed);

        if(scalarElapsed < vectorElapsed)
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("WINDOW - OPTIMAL IMPLEMENTATION SET TO: " + getImplementation());
    }
}
