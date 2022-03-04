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

import io.github.dsheirer.buffer.airspy.AirspyBufferIterator;
import io.github.dsheirer.buffer.airspy.AirspyBufferIteratorScalar;
import io.github.dsheirer.buffer.airspy.AirspyBufferIteratorVector128Bits;
import io.github.dsheirer.buffer.airspy.AirspyBufferIteratorVector256Bits;
import io.github.dsheirer.buffer.airspy.AirspyBufferIteratorVector512Bits;
import io.github.dsheirer.buffer.airspy.AirspyBufferIteratorVector64Bits;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import jdk.incubator.vector.FloatVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates optimal implementation (SCALAR vs VECTOR) for non-interleaved airspy native buffers.
 */
public class AirspyUnpackedCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(AirspyUnpackedCalibration.class);
    private static final int BUFFER_SIZE = 131072;
    private static final int ITERATIONS = 10_000;

    /**
     * Constructs an instance
     */
    public AirspyUnpackedCalibration()
    {
        super(CalibrationType.AIRSPY_UNPACKED_ITERATOR);
    }

    @Override
    public void calibrate() throws CalibrationException
    {
        short[] samples = getShortSamples(BUFFER_SIZE);
        short[] residualI = getShortSamples(AirspyBufferIterator.I_OVERLAP);
        short[] residualQ = getShortSamples(AirspyBufferIterator.Q_OVERLAP);

        long bestScore = calibrateScalar(samples, residualI, residualQ);
        Implementation bestImplementation = Implementation.SCALAR;
        mLog.info("AIRSPY UNPACKED - SCALAR: " + bestScore);

        switch(FloatVector.SPECIES_PREFERRED.length())
        {
            //Deliberate fall-through of each case statement so that we can test the largest
            //SIMD lane width supported by hardware down to the smallest SIMD lane width.
            case 16:
            {
                long vector512 = calibrateVector512(samples, residualI, residualQ);
                mLog.info("AIRSPY UNPACKED - VECTOR 512: " + vector512);
                if(vector512 < bestScore)
                {
                    bestScore = vector512;
                    bestImplementation = Implementation.VECTOR_SIMD_512;
                }
            }
            case 8:
            {
                long vector256 = calibrateVector256(samples, residualI, residualQ);
                mLog.info("AIRSPY UNPACKED - VECTOR 256: " + vector256);
                if(vector256 < bestScore)
                {
                    bestScore = vector256;
                    bestImplementation = Implementation.VECTOR_SIMD_256;
                }
            }
            case 4:
            {
                long vector128 = calibrateVector128(samples, residualI, residualQ);
                mLog.info("AIRSPY UNPACKED - VECTOR 128: " + vector128);
                if(vector128 < bestScore)
                {
                    bestScore = vector128;
                    bestImplementation = Implementation.VECTOR_SIMD_128;
                }
            }
            case 2:
            {
                long vector64 = calibrateVector64(samples, residualI, residualQ);
                mLog.info("AIRSPY UNPACKED - VECTOR 64: " + vector64);
                if(vector64 < bestScore)
                {
                    bestImplementation = Implementation.VECTOR_SIMD_64;
                }
            }
        }

        mLog.info("AIRSPY UNPACKED - SETTING OPTIMAL OPERATION TO: " + bestImplementation);
        setImplementation(bestImplementation);
    }

    private long calibrateScalar(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < ITERATIONS; x++)
        {
            AirspyBufferIteratorScalar iterator = new AirspyBufferIteratorScalar(samples, residualI,
                    residualQ, 0.0f, System.currentTimeMillis());

            while(iterator.hasNext())
            {
                accumulator += iterator.next().i()[2];
            }
        }

        return System.currentTimeMillis() - start;
    }

    private long calibrateVector64(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < ITERATIONS; x++)
        {
            AirspyBufferIteratorVector64Bits iterator = new AirspyBufferIteratorVector64Bits(samples, residualI,
                    residualQ, 0.0f, System.currentTimeMillis());

            while(iterator.hasNext())
            {
                accumulator += iterator.next().i()[2];
            }
        }

        return System.currentTimeMillis() - start;
    }

    private long calibrateVector128(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < ITERATIONS; x++)
        {
            AirspyBufferIteratorVector128Bits iterator = new AirspyBufferIteratorVector128Bits(samples, residualI,
                    residualQ, 0.0f, System.currentTimeMillis());

            while(iterator.hasNext())
            {
                accumulator += iterator.next().i()[2];
            }
        }

        return System.currentTimeMillis() - start;
    }

    private long calibrateVector256(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < ITERATIONS; x++)
        {
            AirspyBufferIteratorVector256Bits iterator =
                    new AirspyBufferIteratorVector256Bits(samples, residualI, residualQ, 0.0f, System.currentTimeMillis());

            while(iterator.hasNext())
            {
                accumulator += iterator.next().i()[2];
            }
        }

        return System.currentTimeMillis() - start;
    }

    private long calibrateVector512(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < ITERATIONS; x++)
        {
            AirspyBufferIteratorVector512Bits iterator =
                    new AirspyBufferIteratorVector512Bits(samples, residualI, residualQ, 0.0f, System.currentTimeMillis());

            while(iterator.hasNext())
            {
                accumulator += iterator.next().i()[2];
            }
        }

        return System.currentTimeMillis() - start;
    }
}
