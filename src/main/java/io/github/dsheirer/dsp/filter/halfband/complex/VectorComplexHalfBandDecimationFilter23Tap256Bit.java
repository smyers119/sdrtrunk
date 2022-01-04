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
import io.github.dsheirer.vector.VectorUtilities;
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
public class VectorComplexHalfBandDecimationFilter23Tap256Bit implements IComplexDecimationFilter
{
    private static final int COEFFICIENT_LENGTH = 23;

    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_256;
    private static final VectorMask<Float> I_VECTOR_MASK = VectorUtilities.getIVectorMask(VECTOR_SPECIES);
    private static final VectorMask<Float> Q_VECTOR_MASK = VectorUtilities.getQVectorMask(VECTOR_SPECIES);

    private float[] mCoefficient0_x_2_x = new float[8];
    private float[] mCoefficient4_x_6_x = new float[8];
    private float[] mCoefficient8_x_10_11 = new float[8];
    private float[] mCoefficient12_x_14_x = new float[8];
    private float[] mCoefficient16_x_18_x = new float[8];
    private float[] mCoefficientx_20_x_22 = new float[8];
    private float[] mBuffer;
    private int mBufferOverlap;

    /**
     * Creates a half band filter with inherent decimation by two.
     *
     * @param coefficients of the half-band filter that is odd-length where all odd index coefficients are
     * zero valued except for the middle odd index coefficient which should be valued 0.5
     */
    public VectorComplexHalfBandDecimationFilter23Tap256Bit(float[] coefficients)
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

        mCoefficient4_x_6_x[0] = coefficients[4];
        mCoefficient4_x_6_x[1] = coefficients[4];
        mCoefficient4_x_6_x[4] = coefficients[6];
        mCoefficient4_x_6_x[5] = coefficients[6];

        mCoefficient8_x_10_11[0] = coefficients[8];
        mCoefficient8_x_10_11[1] = coefficients[8];
        mCoefficient8_x_10_11[4] = coefficients[10];
        mCoefficient8_x_10_11[5] = coefficients[10];
        mCoefficient8_x_10_11[6] = coefficients[11];
        mCoefficient8_x_10_11[7] = coefficients[11];

        mCoefficient12_x_14_x[0] = coefficients[12];
        mCoefficient12_x_14_x[1] = coefficients[12];
        mCoefficient12_x_14_x[4] = coefficients[14];
        mCoefficient12_x_14_x[5] = coefficients[14];

        mCoefficient16_x_18_x[0] = coefficients[16];
        mCoefficient16_x_18_x[1] = coefficients[16];
        mCoefficient16_x_18_x[4] = coefficients[18];
        mCoefficient16_x_18_x[5] = coefficients[18];

        mCoefficientx_20_x_22[2] = coefficients[20];
        mCoefficientx_20_x_22[3] = coefficients[20];
        mCoefficientx_20_x_22[6] = coefficients[22];
        mCoefficientx_20_x_22[7] = coefficients[22];

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
        FloatVector filterB = FloatVector.fromArray(VECTOR_SPECIES, mCoefficient4_x_6_x, 0);
        FloatVector filterC = FloatVector.fromArray(VECTOR_SPECIES, mCoefficient8_x_10_11, 0);
        FloatVector filterD = FloatVector.fromArray(VECTOR_SPECIES, mCoefficient12_x_14_x, 0);
        FloatVector filterE = FloatVector.fromArray(VECTOR_SPECIES, mCoefficient16_x_18_x, 0);
        FloatVector filterF = FloatVector.fromArray(VECTOR_SPECIES, mCoefficientx_20_x_22, 0);

        FloatVector fv0_2, fv4_6, fv8_10_11, fv12_14, fv16_18, fv20_22, accumulator;

        for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer += 4)
        {
            fv0_2 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer);
            fv4_6 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 8);
            fv8_10_11 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 16);
            fv12_14 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 24);
            fv16_18 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 32);
            fv20_22 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 38);

            accumulator = filterA.mul(fv0_2)
                    .add(filterB.mul(fv4_6))
                    .add(filterC.mul(fv8_10_11))
                    .add(filterD.mul(fv12_14))
                    .add(filterE.mul(fv16_18))
                    .add(filterF.mul(fv20_22));

            filtered[bufferPointer / 2] = accumulator.reduceLanes(VectorOperators.ADD, I_VECTOR_MASK);
            filtered[bufferPointer / 2 + 1] = accumulator.reduceLanes(VectorOperators.ADD, Q_VECTOR_MASK);
        }

        return filtered;
    }
}
