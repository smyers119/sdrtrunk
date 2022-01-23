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
 *  11-tap half-band filters
 *  512-bit/16-lane SIMD instructions (Intel AVX-512).
 */
public class VectorComplexHalfBandDecimationFilter11Tap512Bit implements IComplexDecimationFilter
{
    private static final int COEFFICIENT_LENGTH = 11;
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_512;
    private static final VectorMask<Float> I_VECTOR_MASK = VectorUtilities.getIVectorMask(VECTOR_SPECIES);
    private static final VectorMask<Float> Q_VECTOR_MASK = VectorUtilities.getQVectorMask(VECTOR_SPECIES);
    private static final VectorMask<Float> MASK_0_x_2_x_4_5_6_x =
            VectorMask.fromArray(VECTOR_SPECIES, new boolean[]{true,true,false,false,true,true,false,false,
            true,true,true,true,true,true,false,false}, 0);
    private static final VectorMask<Float> MASK_x_8_x_10_x_x_x_x =
            VectorMask.fromArray(VECTOR_SPECIES, new boolean[]{false,false,true,true,false,false,true,true,
            false,false,false,false,false,false,false,false}, 0);

    private float[] mCoefficients = new float[16];
    private float[] mBuffer;
    private int mBufferOverlap;

    /**
     * Creates a half band filter with inherent decimation by two.
     *
     * @param coefficients of the half-band filter that is odd-length where all odd index coefficients are
     * zero valued except for the middle odd index coefficient which should be valued 0.5
     */
    public VectorComplexHalfBandDecimationFilter11Tap512Bit(float[] coefficients)
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

        //Re-arrange the coefficients for mirrored vector operations
        mCoefficients[0] = coefficients[0];
        mCoefficients[1] = coefficients[0];
        mCoefficients[2] = coefficients[2];
        mCoefficients[3] = coefficients[2];
        mCoefficients[4] = coefficients[2];
        mCoefficients[5] = coefficients[2];
        mCoefficients[6] = coefficients[0];
        mCoefficients[7] = coefficients[0];
        mCoefficients[8] = coefficients[4];
        mCoefficients[9] = coefficients[4];
        mCoefficients[10] = coefficients[5];
        mCoefficients[11] = coefficients[5];
        mCoefficients[12] = coefficients[4];
        mCoefficients[13] = coefficients[4];
        //coefficients 14 and 15 are left zero

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

        FloatVector filter = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 0);

        for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer += 4)
        {
            FloatVector vector0_x_2_x_4_5_6_x = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer, MASK_0_x_2_x_4_5_6_x);
            FloatVector vectorx_8_x_10 = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 14, MASK_x_8_x_10_x_x_x_x);
            FloatVector accumulator = filter.mul(vector0_x_2_x_4_5_6_x.add(vectorx_8_x_10));

            filtered[bufferPointer / 2] = accumulator.reduceLanes(VectorOperators.ADD, I_VECTOR_MASK);
            filtered[bufferPointer / 2 + 1] = accumulator.reduceLanes(VectorOperators.ADD, Q_VECTOR_MASK);
        }

        return filtered;
    }
}
