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

package io.github.dsheirer.vector.calibrate.airspy;

import io.github.dsheirer.buffer.airspy.ScalarUnpackedSampleConverter;
import io.github.dsheirer.buffer.airspy.VectorUnpackedSampleConverter;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class AirspySampleConverterCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(AirspySampleConverterCalibration.class);
    private static final int BUFFER_SIZE = 262144;
    private static final int ITERATIONS = 100_000;

    /**
     * Constructs an instance
     */
    public AirspySampleConverterCalibration()
    {
        super(CalibrationType.AIRSPY_SAMPLE_CONVERTER);
    }

    @Override
    public void calibrate() throws CalibrationException
    {
        byte[] samples = new byte[BUFFER_SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(samples);

        ScalarUnpackedSampleConverter scalor = new ScalarUnpackedSampleConverter();
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < ITERATIONS; x++)
        {
            short[] converted = scalor.convert(buffer);
            accumulator += converted[2];
        }
        long scalorElapsed = System.currentTimeMillis() - start;

        mLog.info("AIRSPY CONVERTER - SCALAR: " + scalorElapsed);

        VectorUnpackedSampleConverter vector = new VectorUnpackedSampleConverter();
        start = System.currentTimeMillis();
        accumulator = 0;
        for(int x = 0; x < ITERATIONS; x++)
        {
            short[] converted = vector.convert(buffer);
            accumulator += converted[2];
        }
        long vectorElapsed = System.currentTimeMillis() - start;

        mLog.info("AIRSPY CONVERTER - VECTOR: " + vectorElapsed);
        if(scalorElapsed < vectorElapsed)
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("AIRSPY CONVERTER - SETTING OPTIMAL OPERATION TO: " + getImplementation());
    }
}
