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

package io.github.dsheirer.dsp.filter.halfband.complex;

import io.github.dsheirer.dsp.filter.decimate.IComplexDecimationFilter;
import io.github.dsheirer.dsp.filter.vector.VectorUtilities;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Implements a half-band filter that produces one filtered output for every two input samples.
 *
 * This filter uses the Java Vector API for SIMD available in JDK 17+.
 *
 * This filter is optimized for:
 *  11-tap half-band filters
 *  256-bit/8-lane SIMD instructions.
 */
public class VectorComplexHalfBandDecimationFilter11Tap256Bit implements IComplexDecimationFilter
{
    private static final int COEFFICIENT_LENGTH = 11;
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_256;
    private static final VectorMask<Float> I_VECTOR_MASK = VectorUtilities.getIVectorMask(VECTOR_SPECIES);
    private static final VectorMask<Float> Q_VECTOR_MASK = VectorUtilities.getQVectorMask(VECTOR_SPECIES);
    private static final VectorMask<Float> MASK_0_x_2_x =
            VectorMask.fromArray(VECTOR_SPECIES, new boolean[]{true,true,false,false,true,true,false,false}, 0);
    private static final VectorMask<Float> MASK_x_8_x_10 =
            VectorMask.fromArray(VECTOR_SPECIES, new boolean[]{false,false,true,true,false,false,true,true}, 0);

    private float[] mCoefficient0_2_2_0 = new float[8];
    private float[] mCoefficient4_5_4_x = new float[8];
    private float[] mBuffer;
    private int mBufferOverlap;

    /**
     * Creates a half band filter with inherent decimation by two.
     *
     * @param coefficients of the half-band filter that is odd-length where all odd index coefficients are
     * zero valued except for the middle odd index coefficient which should be valued 0.5
     */
    public VectorComplexHalfBandDecimationFilter11Tap256Bit(float[] coefficients)
    {
        VectorUtilities.checkSpecies(VECTOR_SPECIES);

        if(coefficients.length != COEFFICIENT_LENGTH)
        {
            throw new IllegalArgumentException("This half-band filter coefficients must be " + COEFFICIENT_LENGTH + " taps long");
        }

        //Size the coefficients array to a multiple of the vector species length, large enough to hold the taps;
        int arrayLength = VECTOR_SPECIES.length();

        while(arrayLength < (coefficients.length * 2))
        {
            arrayLength += VECTOR_SPECIES.length();
        }

        //Arrange the coefficients for loading as 4-lane vectors
        mCoefficient0_2_2_0[0] = coefficients[0];
        mCoefficient0_2_2_0[1] = coefficients[0];
        mCoefficient0_2_2_0[2] = coefficients[2];
        mCoefficient0_2_2_0[3] = coefficients[2];
        mCoefficient0_2_2_0[4] = coefficients[2];
        mCoefficient0_2_2_0[5] = coefficients[2];
        mCoefficient0_2_2_0[6] = coefficients[0];
        mCoefficient0_2_2_0[7] = coefficients[0];

        mCoefficient4_5_4_x[0] = coefficients[4];
        mCoefficient4_5_4_x[1] = coefficients[4];
        mCoefficient4_5_4_x[2] = coefficients[5]; //Center coefficient
        mCoefficient4_5_4_x[3] = coefficients[5];
        mCoefficient4_5_4_x[4] = coefficients[4]; //Mirror of 6
        mCoefficient4_5_4_x[5] = coefficients[4];

        //Set buffer overlap to larger of the length of the SIMD lanes minus 2 or double the coefficients length minus 2
        //to ensure we don't get an index out of bounds exception when loading samples from the buffer.
        mBufferOverlap = Math.max(arrayLength - 2, (coefficients.length * 2) - 2);
    }

    public float[] decimateComplex(float[] samples)
    {
        if(samples.length % 4 != 0)
        {
            throw new IllegalArgumentException("Samples array length must be an integer multiple of 4");
        }

        int bufferLength = samples.length + mBufferOverlap;

        if(mBuffer == null)
        {
            mBuffer = new float[bufferLength];
        }
        else if(mBuffer.length != bufferLength)
        {
            float[] temp = new float[bufferLength];
            //Move residual samples from end of old buffer to the beginning of the new temp buffer
            System.arraycopy(mBuffer, mBuffer.length - mBufferOverlap, temp, 0, mBufferOverlap);
            mBuffer = temp;
        }
        else
        {
            //Move residual samples from end of buffer to the beginning of the buffer
            System.arraycopy(mBuffer, samples.length, mBuffer, 0, mBufferOverlap);
        }

        //Copy new sample array into end of buffer
        System.arraycopy(samples, 0, mBuffer, mBufferOverlap, samples.length);

        float[] filtered = new float[samples.length / 2];

        FloatVector filterA = FloatVector.fromArray(VECTOR_SPECIES, mCoefficient0_2_2_0, 0);
        FloatVector filterB = FloatVector.fromArray(VECTOR_SPECIES, mCoefficient4_5_4_x, 0);

        for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer += 4)
        {
            FloatVector vector0_x_2_x = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer, MASK_0_x_2_x);
            FloatVector vectorx_8_x_10 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 14, MASK_x_8_x_10);

            //We don't mask lanes 7 and 8 here, because the filter coefficients are zero anyways.
            FloatVector vector4_5_6 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 8);

            FloatVector accumulator = filterA.mul(vector0_x_2_x.add(vectorx_8_x_10)).add(filterB.mul(vector4_5_6));

            filtered[bufferPointer / 2] = accumulator.reduceLanes(VectorOperators.ADD, I_VECTOR_MASK);
            filtered[bufferPointer / 2 + 1] = accumulator.reduceLanes(VectorOperators.ADD, Q_VECTOR_MASK);
        }

        return filtered;
    }
}
