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
 *  15-tap half-band filters
 *  256-bit/8-lane SIMD instructions.
 */
public class VectorComplexHalfBandDecimationFilter15Tap256Bit implements IComplexDecimationFilter
{
    private static final int COEFFICIENT_LENGTH = 15;
    private static final int CENTER_I_INDEX = 14;
    private static final int CENTER_Q_INDEX = 15;
    private static final float CENTER_COEFFICIENT = 0.5f;

    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_256;
    private static final VectorMask<Float> I_VECTOR_MASK = VectorUtilities.getIVectorMask(VECTOR_SPECIES);
    private static final VectorMask<Float> Q_VECTOR_MASK = VectorUtilities.getQVectorMask(VECTOR_SPECIES);

    private float[] mCoefficient0_x_2_x = new float[8];
    private float[] mCoefficientx_4_x_6 = new float[8];
    private float[] mCoefficient8_x_10_x = new float[8];
    private float[] mCoefficientx_12_x_14 = new float[8];
    private float[] mBuffer;
    private int mBufferOverlap;

    /**
     * Creates a half band filter with inherent decimation by two.
     *
     * @param coefficients of the half-band filter that is odd-length where all odd index coefficients are
     * zero valued except for the middle odd index coefficient which should be valued 0.5
     */
    public VectorComplexHalfBandDecimationFilter15Tap256Bit(float[] coefficients)
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

        //Arrange the coefficients for loading as 8-lane vectors
        mCoefficient0_x_2_x[0] = coefficients[0];
        mCoefficient0_x_2_x[1] = coefficients[0];
        mCoefficient0_x_2_x[4] = coefficients[2];
        mCoefficient0_x_2_x[5] = coefficients[2];

        mCoefficientx_4_x_6[2] = coefficients[4];
        mCoefficientx_4_x_6[3] = coefficients[4];
        mCoefficientx_4_x_6[6] = coefficients[6];
        mCoefficientx_4_x_6[7] = coefficients[6];

        mCoefficient8_x_10_x[0] = coefficients[8];
        mCoefficient8_x_10_x[1] = coefficients[8];
        mCoefficient8_x_10_x[4] = coefficients[10];
        mCoefficient8_x_10_x[5] = coefficients[10];

        mCoefficientx_12_x_14[2] = coefficients[12];
        mCoefficientx_12_x_14[3] = coefficients[12];
        mCoefficientx_12_x_14[6] = coefficients[14];
        mCoefficientx_12_x_14[7] = coefficients[14];

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

        FloatVector filterA = FloatVector.fromArray(VECTOR_SPECIES, mCoefficient0_x_2_x, 0);
        FloatVector filterB = FloatVector.fromArray(VECTOR_SPECIES, mCoefficientx_4_x_6, 0);
        FloatVector filterC = FloatVector.fromArray(VECTOR_SPECIES, mCoefficient8_x_10_x, 0);
        FloatVector filterD = FloatVector.fromArray(VECTOR_SPECIES, mCoefficientx_12_x_14, 0);

        FloatVector fv0_2, fv4_6, fv8_10, fv10_12, accumulator;

        for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer += 4)
        {
            fv0_2 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer);
            fv4_6 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 6);
            fv8_10 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 16);
            fv10_12 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 22);

            accumulator = filterA.mul(fv0_2).add(filterB.mul(fv4_6).add(filterC.mul(fv8_10).add(filterD.mul(fv10_12))));

            int half = bufferPointer / 2;

            filtered[half] = accumulator.reduceLanes(VectorOperators.ADD, I_VECTOR_MASK);
            filtered[half] += (mBuffer[bufferPointer + CENTER_I_INDEX] * CENTER_COEFFICIENT);
            filtered[half + 1] = accumulator.reduceLanes(VectorOperators.ADD, Q_VECTOR_MASK);
            filtered[half + 1] += (mBuffer[bufferPointer + CENTER_Q_INDEX] * CENTER_COEFFICIENT);
        }

        return filtered;
    }
}
