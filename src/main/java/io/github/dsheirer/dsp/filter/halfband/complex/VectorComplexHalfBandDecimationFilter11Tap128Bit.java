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
 *  128-bit/4-lane SIMD instructions.
 */
public class VectorComplexHalfBandDecimationFilter11Tap128Bit implements IComplexDecimationFilter
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_128;
    private static final VectorMask<Float> I_VECTOR_MASK = VectorUtilities.getIVectorMask(VECTOR_SPECIES);
    private static final VectorMask<Float> Q_VECTOR_MASK = VectorUtilities.getQVectorMask(VECTOR_SPECIES);
    private static final VectorMask<Float> MASK_RIGHT = VectorMask.fromArray(VECTOR_SPECIES, new boolean[]{true,true,false,false}, 0);
    private static final VectorMask<Float> MASK_LEFT = VectorMask.fromArray(VECTOR_SPECIES, new boolean[]{false,false,true,true}, 0);

    private float[] mCoefficient0_2 = new float[4];
    private float[] mCoefficient4_5 = new float[4];
    private float[] mBuffer;
    private int mBufferOverlap;

    /**
     * Creates a half band filter with inherent decimation by two.
     *
     * @param coefficients of the half-band filter that is odd-length where all odd index coefficients are
     * zero valued except for the middle odd index coefficient which should be valued 0.5
     */
    public VectorComplexHalfBandDecimationFilter11Tap128Bit(float[] coefficients)
    {
        VectorUtilities.checkSpecies(VECTOR_SPECIES);

        if(coefficients.length != 11)
        {
            throw new IllegalArgumentException("This half-band filter coefficients must be 11 taps long");
        }

        //Size the coefficients array to a multiple of the vector species length, large enough to hold the taps;
        int arrayLength = VECTOR_SPECIES.length();

        while(arrayLength < (coefficients.length * 2))
        {
            arrayLength += VECTOR_SPECIES.length();
        }

        //Arrange the coefficients for loading as 4-lane vectors
        mCoefficient0_2[0] = coefficients[0];
        mCoefficient0_2[1] = coefficients[0];
        mCoefficient0_2[2] = coefficients[2];
        mCoefficient0_2[3] = coefficients[2];

        mCoefficient4_5[0] = coefficients[4];
        mCoefficient4_5[1] = coefficients[4];
        mCoefficient4_5[2] = coefficients[5]; //Center coefficient
        mCoefficient4_5[3] = coefficients[5];

        //Set buffer overlap to larger of the length of the SIMD lanes minus 2 or double the coefficient's length minus 2
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

        FloatVector filterA = FloatVector.fromArray(VECTOR_SPECIES, mCoefficient0_2, 0);
        FloatVector filterB = FloatVector.fromArray(VECTOR_SPECIES, mCoefficient4_5, 0);

        for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer += 4)
        {
            FloatVector vector0 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer, MASK_RIGHT);
            FloatVector vector2 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 2, MASK_LEFT);
            FloatVector vector8 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 14, MASK_LEFT);
            FloatVector vector10 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 20, MASK_RIGHT);

            FloatVector vector4_5 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 8);
            FloatVector vector6 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 12, MASK_RIGHT);
            FloatVector accumulator = filterA.mul(vector0.add(vector2).add(vector8).add(vector10))
                    .add(filterB.mul(vector4_5.add(vector6)));

            filtered[bufferPointer / 2] = accumulator.reduceLanes(VectorOperators.ADD, I_VECTOR_MASK);
            filtered[bufferPointer / 2 + 1] = accumulator.reduceLanes(VectorOperators.ADD, Q_VECTOR_MASK);
        }

        return filtered;
    }
}
