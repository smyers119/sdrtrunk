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
import io.github.dsheirer.dsp.fm.ScalarSquelchingFMDemodulator;
import io.github.dsheirer.dsp.fm.VectorSquelchingFMDemodulator;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calibrates squelching FM demodulator options
 */
public class SquelchingFmDemodulatorCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(SquelchingFmDemodulatorCalibration.class);
    private static final float POWER_SQUELCH_ALPHA_DECAY = 0.0004f;
    private static final float POWER_SQUELCH_THRESHOLD_DB = -78.0f;
    private static final int POWER_SQUELCH_RAMP = 4;
    private static final int BUFFER_SIZE = 2048;
    private static final int BUFFER_ITERATIONS = 400;
    private static final int WARMUP_ITERATIONS = 50;
    private static final int TEST_ITERATIONS = 50;

    private IFmDemodulator mScalar = new ScalarSquelchingFMDemodulator(POWER_SQUELCH_ALPHA_DECAY,
            POWER_SQUELCH_THRESHOLD_DB, POWER_SQUELCH_RAMP);
    private IFmDemodulator mVector = new VectorSquelchingFMDemodulator(POWER_SQUELCH_ALPHA_DECAY,
            POWER_SQUELCH_THRESHOLD_DB, POWER_SQUELCH_RAMP);

    /**
     * Constructs an instance
     */
    public SquelchingFmDemodulatorCalibration()
    {
        super(CalibrationType.SQUELCHING_FM_DEMODULATOR);
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

        mLog.info("SQUELCHING FM DEMODULATOR WARMUP - SCALAR:" + DECIMAL_FORMAT.format(scalarMean.getResult()));

        Mean vectorMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long elapsed = testVector(i, q);
            vectorMean.increment(elapsed);
        }

        mLog.info("SQUELCHING FM DEMODULATOR WARMUP - VECTOR:" + DECIMAL_FORMAT.format(vectorMean.getResult()));

        //Test begins ...
        scalarMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long elapsed = testScalar(i, q);
            scalarMean.increment(elapsed);
        }

        mLog.info("SQUELCHING FM DEMODULATOR - SCALAR:" + DECIMAL_FORMAT.format(scalarMean.getResult()));

        vectorMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long elapsed = testVector(i, q);
            vectorMean.increment(elapsed);
        }

        mLog.info("SQUELCHING FM DEMODULATOR - VECTOR:" + DECIMAL_FORMAT.format(vectorMean.getResult()));

        if(scalarMean.getResult() < vectorMean.getResult())
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("SQUELCHING FM DEMODULATOR - SET OPTIMAL IMPLEMENTATION TO:" + getImplementation());
    }

    private long testScalar(float[] i, float[] q)
    {
        double accumulator = 0.0;

        long start = System.currentTimeMillis();

        for(int x = 0; x < BUFFER_ITERATIONS; x++)
        {
            float[] demodulated = mScalar.demodulate(i, q);
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
            float[] demodulated = mVector.demodulate(i, q);
            accumulator += demodulated[1];
        }

        return System.currentTimeMillis() - start + (long)(accumulator * 0);
    }
}
