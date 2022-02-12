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

package io.github.dsheirer.vector.calibrate.filter;

import io.github.dsheirer.dsp.filter.dc.ScalarDcRemovalFilter;
import io.github.dsheirer.dsp.filter.dc.VectorDcRemovalFilter;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines optimal DC removal implementation
 */
public class RealDcRemovalCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(RealDcRemovalCalibration.class);
    private static final int BUFFER_SIZE = 8192;
    private static final int ITERATIONS = 1_000_000;
    private static final float GAIN = 0.15f;

    /**
     * Constructs an instance
     */
    public RealDcRemovalCalibration()
    {
        super(CalibrationType.DC_REMOVAL_REAL);
    }

    @Override
    public void calibrate() throws CalibrationException
    {
        float[] samples = getSamples(BUFFER_SIZE);

        ScalarDcRemovalFilter scalar = new ScalarDcRemovalFilter(GAIN);

        long start = System.currentTimeMillis();

        for(int iteration = 0; iteration < ITERATIONS; iteration++)
        {
            scalar.filter(samples);
        }

        long scalarElapsed = System.currentTimeMillis() - start;
        mLog.info("REAL DC REMOVAL SCALAR:" + scalarElapsed);


        VectorDcRemovalFilter vector = new VectorDcRemovalFilter(GAIN);
        samples = getSamples(BUFFER_SIZE);

        start = System.currentTimeMillis();

        for(int iteration = 0; iteration < ITERATIONS; iteration++)
        {
            vector.filter(samples);
        }

        long vectorElapsed = System.currentTimeMillis() - start;
        mLog.info("REAL DC REMOVAL VECTOR:" + vectorElapsed);

        if(scalarElapsed < vectorElapsed)
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("REAL DC REMOVAL - SETTING OPTIMAL OPERATION TO: " + getImplementation());
    }
}
