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

package io.github.dsheirer.vector.calibrate.gain;

import io.github.dsheirer.dsp.gain.complex.ComplexGain;
import io.github.dsheirer.dsp.gain.complex.ScalarComplexGain;
import io.github.dsheirer.dsp.gain.complex.VectorComplexGain;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines the optimal scalar vs vector implementation of complex gain.
 */
public class ComplexGainCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(ComplexGainCalibration.class);
    private static final int BUFFER_SIZE = 2048;
    private static final int BUFFER_ITERATIONS = 100_000;
    private static final int WARMUP_ITERATIONS = 100;
    private static final int TEST_ITERATIONS = 100;
    private static final float GAIN = 0.99f;

    /**
     * Constructs an instance
     */
    public ComplexGainCalibration()
    {
        super(CalibrationType.GAIN_COMPLEX);
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

        mLog.info("COMPLEX GAIN WARMUP - SCALAR:" + DECIMAL_FORMAT.format(scalarMean.getResult()));

        i = getFloatSamples(BUFFER_SIZE);
        q = getFloatSamples(BUFFER_SIZE);

        Mean vectorMean = new Mean();
        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long elapsed = testVector(i, q);
            vectorMean.increment(elapsed);
        }

        mLog.info("COMPLEX GAIN WARMUP - VECTOR:" + DECIMAL_FORMAT.format(vectorMean.getResult()));

        i = getFloatSamples(BUFFER_SIZE);
        q = getFloatSamples(BUFFER_SIZE);

        scalarMean.clear();;
        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long elapsed = testScalar(i, q);
            scalarMean.increment(elapsed);
        }

        mLog.info("COMPLEX GAIN - SCALAR:" + DECIMAL_FORMAT.format(scalarMean.getResult()));

        i = getFloatSamples(BUFFER_SIZE);
        q = getFloatSamples(BUFFER_SIZE);

        vectorMean.clear();
        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long elapsed = testVector(i, q);
            vectorMean.increment(elapsed);
        }

        mLog.info("COMPLEX GAIN - VECTOR:" + DECIMAL_FORMAT.format(vectorMean.getResult()));

        if(scalarMean.getResult() < vectorMean.getResult())
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("COMPLEX GAIN - SET IMPLEMENTATION TO:" + getImplementation());
    }

    private long testScalar(float[] i, float[] q)
    {
        float accumulator = 0.0f;

        ComplexGain scalar = new ScalarComplexGain(GAIN);

        long start = System.currentTimeMillis();

        for(int x = 0; x < BUFFER_ITERATIONS; x++)
        {
            ComplexSamples amplified = scalar.apply(i, q);
            accumulator += amplified.i()[2];
        }

        return System.currentTimeMillis() - start + (long)(accumulator * 0);
    }

    private long testVector(float[] i, float[] q)
    {
        float accumulator = 0.0f;

        ComplexGain scalar = new VectorComplexGain(GAIN);

        long start = System.currentTimeMillis();

        for(int x = 0; x < BUFFER_ITERATIONS; x++)
        {
            ComplexSamples amplified = scalar.apply(i, q);
            accumulator += amplified.i()[2];
        }

        return System.currentTimeMillis() - start + (long)(accumulator * 0);
    }
}
