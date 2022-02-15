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

package io.github.dsheirer.vector.calibrate.hilbert;

import io.github.dsheirer.dsp.filter.hilbert.HilbertTransform;
import io.github.dsheirer.dsp.filter.hilbert.ScalarHilbertTransform;
import io.github.dsheirer.dsp.filter.hilbert.VectorHilbertTransform128Bits;
import io.github.dsheirer.dsp.filter.hilbert.VectorHilbertTransform256Bits;
import io.github.dsheirer.dsp.filter.hilbert.VectorHilbertTransform512Bits;
import io.github.dsheirer.dsp.filter.hilbert.VectorHilbertTransform64Bits;
import io.github.dsheirer.dsp.filter.hilbert.VectorHilbertTransformDefaultBits;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HilbertCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(HilbertCalibration.class);
    private static final int ITERATIONS = 1_000_000;
    private static final int SAMPLE_BUFFER_SIZE = 2048;

    /**
     * Constructs an instance
     */
    public HilbertCalibration()
    {
        super(CalibrationType.HILBERT_TRANSFORM);
    }

    /**
     * Performs calibration to determine optimal (Scalar vs Vector) operation type.
     * @throws CalibrationException
     */
    @Override public void calibrate() throws CalibrationException
    {
        float[] samples = getFloatSamples(SAMPLE_BUFFER_SIZE);

        long bestScore = calculateScalar(samples, ITERATIONS);
        mLog.info("HILBERT SCALAR:" + bestScore);
        Implementation operation = Implementation.SCALAR;

        long vectorPreferred = calculateVector(FloatVector.SPECIES_PREFERRED, samples, ITERATIONS);
        mLog.info("HILBERT VECTOR PREFERRED:" + vectorPreferred);

        if(vectorPreferred < bestScore)
        {
            bestScore = vectorPreferred;
            operation = Implementation.VECTOR_SIMD_PREFERRED;
        }

        switch(FloatVector.SPECIES_PREFERRED.length())
        {
            //Fall through for each switch case is the intended behavior
            case 16:
                long vector512 = calculateVector(FloatVector.SPECIES_512, samples, ITERATIONS);
                mLog.info("HILBERT VECTOR 512:" + vector512);
                if(vector512 < bestScore)
                {
                    bestScore = vector512;
                    operation = Implementation.VECTOR_SIMD_512;
                }
            case 8:
                long vector256 = calculateVector(FloatVector.SPECIES_256, samples, ITERATIONS);
                mLog.info("HILBERT VECTOR 256:" + vector256);
                if(vector256 < bestScore)
                {
                    bestScore = vector256;
                    operation = Implementation.VECTOR_SIMD_256;
                }
            case 4:
                long vector128 = calculateVector(FloatVector.SPECIES_128, samples, ITERATIONS);
                mLog.info("HILBERT VECTOR 128:" + vector128);
                if(vector128 < bestScore)
                {
                    bestScore = vector128;
                    operation = Implementation.VECTOR_SIMD_128;
                }
            case 2:
                long vector64 = calculateVector(FloatVector.SPECIES_64, samples, ITERATIONS);
                mLog.info("HILBERT VECTOR 64:" + vector64);
                if(vector64 < bestScore)
                {
                    operation = Implementation.VECTOR_SIMD_64;
                }
        }

        mLog.info("HILBERT - SETTING OPTIMAL OPERATION TO: " + operation);
        setImplementation(operation);
    }

    /**
     * Calculates the time duration to process the sample buffer.
     * @param samples buffer
     * @param iterations count of how many times to process the samples buffer
     * @return time duration in milliseconds
     */
    private static long calculateScalar(float[] samples, int iterations)
    {
        float accumulator = 0.0f;

        ScalarHilbertTransform scalar = new ScalarHilbertTransform();

        long start = System.currentTimeMillis();

        for(int i = 0; i < iterations; i++)
        {
            ComplexSamples filtered = scalar.filter(samples);
            accumulator += filtered.i()[0];
        }

        return System.currentTimeMillis() - start;
    }

    /**
     * Calculates the time duration to process the sample buffer.
     * @param species of vector for the SIMD instruction lane width
     * @param samples buffer
     * @param iterations count of how many times to (re)filter the samples buffer
     * @return time duration in milliseconds
     */
    private static long calculateVector(VectorSpecies<Float> species, float[] samples, int iterations)
    {
        float accumulator = 0.0f;

        HilbertTransform vector = getFilter(species);

        long start = System.currentTimeMillis();

        for(int i = 0; i < iterations; i++)
        {
            ComplexSamples filtered = vector.filter(samples);
            accumulator += filtered.i()[0];
        }

        return System.currentTimeMillis() - start;
    }

    private static HilbertTransform getFilter(VectorSpecies<Float> species)
    {
        if(species.equals(FloatVector.SPECIES_PREFERRED))
        {
            return new VectorHilbertTransformDefaultBits();
        }

        switch(species.length())
        {
            case 16:
                return new VectorHilbertTransform512Bits();
            case 8:
                return new VectorHilbertTransform256Bits();
            case 4:
                return new VectorHilbertTransform128Bits();
            case 2:
                return new VectorHilbertTransform64Bits();
        }

        throw new IllegalArgumentException("Unrecognized vector species:" + species);
    }
}
