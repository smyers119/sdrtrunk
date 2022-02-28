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
import org.apache.commons.math3.stat.descriptive.moment.Mean;

import java.nio.ByteBuffer;

public class AirspySampleConverterCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 262144;
    private static final int BUFFER_ITERATIONS = 100;
    private static final int WARM_UP_ITERATIONS = 50;
    private static final int TEST_ITERATIONS = 50;

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
        Mean scalarMean = new Mean();
        Mean vectorMean = new Mean();

        byte[] samples = new byte[BUFFER_SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(samples);

        for(int scalarTest = 0; scalarTest < WARM_UP_ITERATIONS; scalarTest++)
        {
            long scalarElapsed = testScalar(buffer);
            scalarMean.increment(scalarElapsed);
        }

        mLog.info("AIRSPY CONVERTER WARMUP - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        for(int vectorTest = 0; vectorTest < WARM_UP_ITERATIONS; vectorTest++)
        {
            long vectorElapsed = testVector(buffer);
            vectorMean.increment(vectorElapsed);
        }

        mLog.info("AIRSPY CONVERTER WARMUP - VECTOR: " + DECIMAL_FORMAT.format(vectorMean.getResult()));

        scalarMean.clear();
        vectorMean.clear();

        for(int scalarTest = 0; scalarTest < TEST_ITERATIONS; scalarTest++)
        {
            long scalarElapsed = testScalar(buffer);
            scalarMean.increment(scalarElapsed);
        }

        mLog.info("AIRSPY CONVERTER - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        for(int vectorTest = 0; vectorTest < TEST_ITERATIONS; vectorTest++)
        {
            long vectorElapsed = testVector(buffer);
            vectorMean.increment(vectorElapsed);
        }

        mLog.info("AIRSPY CONVERTER - VECTOR: " + DECIMAL_FORMAT.format(vectorMean.getResult()));

        if(scalarMean.getResult() < vectorMean.getResult())
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("AIRSPY CONVERTER - SETTING OPTIMAL IMPLEMENTATION TO: " + getImplementation());
    }

    private long testScalar(ByteBuffer buffer)
    {
        ScalarUnpackedSampleConverter scalor = new ScalarUnpackedSampleConverter();
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < BUFFER_ITERATIONS; x++)
        {
            short[] converted = scalor.convert(buffer);
            accumulator += converted[2];
        }

        return System.currentTimeMillis() - start + (accumulator * 0);
    }

    private long testVector(ByteBuffer buffer)
    {
        VectorUnpackedSampleConverter vector = new VectorUnpackedSampleConverter();
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < BUFFER_ITERATIONS; x++)
        {
            short[] converted = vector.convert(buffer);
            accumulator += converted[2];
        }

        return System.currentTimeMillis() - start + (accumulator * 0);
    }
}
