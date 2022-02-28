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

package io.github.dsheirer.vector.calibrate.demodulator;

import io.github.dsheirer.dsp.fm.IFmDemodulator;
import io.github.dsheirer.dsp.fm.ScalarFMDemodulator;
import io.github.dsheirer.dsp.fm.VectorFMDemodulator;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * Calibrates FM demodulator options
 */
public class FmDemodulatorCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 2048;
    private static final int BUFFER_ITERATIONS = 1_000;
    private static final int WARMUP_ITERATIONS = 50;
    private static final int TEST_ITERATIONS = 50;
    private IFmDemodulator mScalarDemodulator = new ScalarFMDemodulator();
    private IFmDemodulator mVectorDemodulator = new VectorFMDemodulator();


    /**
     * Constructs an instance
     */
    public FmDemodulatorCalibration()
    {
        super(CalibrationType.FM_DEMODULATOR);
    }

    @Override public void calibrate() throws CalibrationException
    {
        float[] i = getFloatSamples(BUFFER_SIZE);
        float[] q = getFloatSamples(BUFFER_SIZE);

        Mean scalarMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long elapsed = testScalar(i, q);
            scalarMean.increment(elapsed);
        }

        mLog.info("FM DEMODULATOR WARMUP - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        Mean vectorMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long elapsed = testVector(i, q);
            vectorMean.increment(elapsed);
        }

        mLog.info("FM DEMODULATOR WARMUP - VECTOR: " + DECIMAL_FORMAT.format(vectorMean.getResult()));

        //Start tests
        scalarMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long elapsed = testScalar(i, q);
            scalarMean.increment(elapsed);
        }

        mLog.info("FM DEMODULATOR - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        vectorMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long elapsed = testVector(i, q);
            vectorMean.increment(elapsed);
        }

        mLog.info("FM DEMODULATOR - VECTOR: " + DECIMAL_FORMAT.format(vectorMean.getResult()));

        if(scalarMean.getResult() < vectorMean.getResult())
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("FM DEMODULATOR - SET OPTIMAL IMPLEMENTATION TO:" + getImplementation());
    }

    private long testScalar(float[] i, float[] q)
    {
        double accumulator = 0.0;
        long start = System.currentTimeMillis();

        for(int x = 0; x < BUFFER_ITERATIONS; x++)
        {
            float[] demodulated = mScalarDemodulator.demodulate(i, q);
            accumulator += demodulated[1];
        }

        return System.currentTimeMillis() - start + (long)(accumulator * 0);
    }

    private long testVector(float[] i, float[] q)
    {
        double accumulator = 0.0;
        long start = System.currentTimeMillis();

        for(int x = 0; x < BUFFER_ITERATIONS; x++)
        {
            float[] demodulated = mVectorDemodulator.demodulate(i, q);
            accumulator += demodulated[1];
        }

        return System.currentTimeMillis() - start + (long)(accumulator * 0);
    }
}
