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
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates optimal implementation (SCALAR vs VECTOR) for non-interleaved airspy native buffers.
 */
public class AirspyUnpackedCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(AirspyUnpackedCalibration.class);
    private static final int BUFFER_SIZE = 131072;
    private static final int BUFFER_ITERATIONS = 30;
    private static final int WARMUP_ITERATIONS = 50;
    private static final int TEST_ITERATIONS = 100;

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

        //Warm-Up Phase ....
        Mean scalarMean = new Mean();

        for(int warmup = 0; warmup < WARMUP_ITERATIONS; warmup++)
        {
            long elapsed = testScalar(samples, residualI, residualQ);
            scalarMean.increment(elapsed);
        }

        mLog.info("AIRSPY UNPACKED WARMUP - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        switch(FloatVector.SPECIES_PREFERRED.length())
        {
            //Deliberate fall-through of each case statement so that we can test the largest
            //SIMD lane width supported by hardware down to the smallest SIMD lane width.
            case 16:
            {
                Mean vector512Mean = new Mean();
                for(int warmup = 0; warmup < WARMUP_ITERATIONS; warmup++)
                {
                    long elapsed = testVector512(samples, residualI, residualQ);
                    vector512Mean.increment(elapsed);
                }

                mLog.info("AIRSPY UNPACKED WARMUP - VECTOR 512: " + DECIMAL_FORMAT.format(vector512Mean.getResult()));
            }
            case 8:
            {
                Mean vector256Mean = new Mean();
                for(int warmup = 0; warmup < WARMUP_ITERATIONS; warmup++)
                {
                    long elapsed = testVector256(samples, residualI, residualQ);
                    vector256Mean.increment(elapsed);
                }

                mLog.info("AIRSPY UNPACKED WARMUP - VECTOR 256: " + DECIMAL_FORMAT.format(vector256Mean.getResult()));
            }
            case 4:
            {
                Mean vector128Mean = new Mean();
                for(int warmup = 0; warmup < WARMUP_ITERATIONS; warmup++)
                {
                    long elapsed = testVector128(samples, residualI, residualQ);
                    vector128Mean.increment(elapsed);
                }

                mLog.info("AIRSPY UNPACKED WARMUP - VECTOR 128: " + DECIMAL_FORMAT.format(vector128Mean.getResult()));
            }
            case 2:
            {
                Mean vector64Mean = new Mean();
                for(int warmup = 0; warmup < WARMUP_ITERATIONS; warmup++)
                {
                    long elapsed = testVector64(samples, residualI, residualQ);
                    vector64Mean.increment(elapsed);
                }

                mLog.info("AIRSPY UNPACKED WARMUP - VECTOR 64: " + DECIMAL_FORMAT.format(vector64Mean.getResult()));
            }
        }

        //Test Phase ....
        scalarMean.clear();

        for(int test = 0; test < TEST_ITERATIONS; test++)
        {
            long elapsed = testScalar(samples, residualI, residualQ);
            scalarMean.increment(elapsed);
        }

        double bestScore = scalarMean.getResult();
        setImplementation(Implementation.SCALAR);
        mLog.info("AIRSPY UNPACKED - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        switch(FloatVector.SPECIES_PREFERRED.length())
        {
            //Deliberate fall-through of each case statement so that we can test the largest
            //SIMD lane width supported by hardware down to the smallest SIMD lane width.
            case 16:
            {
                Mean vector512Mean = new Mean();
                for(int test = 0; test < TEST_ITERATIONS; test++)
                {
                    long elapsed = testVector512(samples, residualI, residualQ);
                    vector512Mean.increment(elapsed);
                }

                mLog.info("AIRSPY UNPACKED - VECTOR 512: " + DECIMAL_FORMAT.format(vector512Mean.getResult()));
                if(vector512Mean.getResult() < bestScore)
                {
                    bestScore = vector512Mean.getResult();
                    setImplementation(Implementation.VECTOR_SIMD_512);
                }
            }
            case 8:
            {
                Mean vector256Mean = new Mean();
                for(int test = 0; test < TEST_ITERATIONS; test++)
                {
                    long elapsed = testVector256(samples, residualI, residualQ);
                    vector256Mean.increment(elapsed);
                }

                mLog.info("AIRSPY UNPACKED - VECTOR 256: " + DECIMAL_FORMAT.format(vector256Mean.getResult()));
                if(vector256Mean.getResult() < bestScore)
                {
                    bestScore = vector256Mean.getResult();
                    setImplementation(Implementation.VECTOR_SIMD_256);
                }
            }
            case 4:
            {
                Mean vector128Mean = new Mean();
                for(int test = 0; test < TEST_ITERATIONS; test++)
                {
                    long elapsed = testVector128(samples, residualI, residualQ);
                    vector128Mean.increment(elapsed);
                }

                mLog.info("AIRSPY UNPACKED - VECTOR 128: " + DECIMAL_FORMAT.format(vector128Mean.getResult()));
                if(vector128Mean.getResult() < bestScore)
                {
                    bestScore = vector128Mean.getResult();
                    setImplementation(Implementation.VECTOR_SIMD_128);
                }
            }
            case 2:
            {
                Mean vector64Mean = new Mean();
                for(int test = 0; test < TEST_ITERATIONS; test++)
                {
                    long elapsed = testVector64(samples, residualI, residualQ);
                    vector64Mean.increment(elapsed);
                }

                mLog.info("AIRSPY UNPACKED - VECTOR 64: " + DECIMAL_FORMAT.format(vector64Mean.getResult()));
                if(vector64Mean.getResult() < bestScore)
                {
                    setImplementation(Implementation.VECTOR_SIMD_64);
                }
            }
        }

        mLog.info("AIRSPY UNPACKED - SET OPTIMAL IMPLEMENTATION TO: " + getImplementation());
    }

    private long testScalar(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < BUFFER_ITERATIONS; x++)
        {
            AirspyBufferIteratorScalar iterator = new AirspyBufferIteratorScalar(samples, residualI,
                    residualQ, 0.0f, System.currentTimeMillis());

            while(iterator.hasNext())
            {
                accumulator += iterator.next().i()[2];
            }
        }

        return System.currentTimeMillis() - start + (accumulator * 0);
    }

    private long testVector64(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < BUFFER_ITERATIONS; x++)
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

    private long testVector128(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < BUFFER_ITERATIONS; x++)
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

    private long testVector256(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < BUFFER_ITERATIONS; x++)
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

    private long testVector512(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        for(int x = 0; x < BUFFER_ITERATIONS; x++)
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
