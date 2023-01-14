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

package io.github.dsheirer.buffer.airspy;

import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vector SIMD implementation for non-packed Airspy native buffers
 */
public class AirspyInterleavedBufferIteratorVector64Bits extends AirspyBufferIterator<InterleavedComplexSamples>
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_64;

    /**
     * Constructs an instance
     *
     * @param samples from the airspy, either packed or unpacked.
     * @param residualI samples from last buffer
     * @param residualQ samples from last buffer
     * @param averageDc measured
     * @param timestamp of the buffer
     * @param samplesPerMillisecond to calculate sub-buffer fragment timestamps
     */
    public AirspyInterleavedBufferIteratorVector64Bits(short[] samples, short[] residualI, short[] residualQ, float averageDc,
                                                       long timestamp, float samplesPerMillisecond)
    {
        super(samples, residualI, residualQ, averageDc, timestamp, samplesPerMillisecond);
    }

    @Override
    public InterleavedComplexSamples next()
    {
        if(mSamplesPointer >= mSamples.length)
        {
            throw new IllegalStateException("End of buffer exceeded");
        }

        long timestamp = getFragmentTimestamp(mSamplesPointer);
        int offset = mSamplesPointer;
        int fragmentPointer = 0;
        float[] scaledSamples = new float[VECTOR_SPECIES.length()];

        while(fragmentPointer < FRAGMENT_SIZE)
        {
            for(int y = 0; y < VECTOR_SPECIES.length(); y++)
            {
                //Couldn't figure out an easy way to load as a short vector and recast to a float vector
                scaledSamples[y] = mSamples[offset++];
            }

            //SIMD scaling operation
            FloatVector.fromArray(VECTOR_SPECIES, scaledSamples, 0)
                    .sub(2048.0f)
                    .mul(SCALE_SIGNED_12_BIT_TO_FLOAT)
                    .sub(mAverageDc)
                    .intoArray(scaledSamples, 0);

            for(int y = 0; y < VECTOR_SPECIES.length(); y += 2)
            {
                mIBuffer[fragmentPointer + I_OVERLAP] = scaledSamples[y];
                mQBuffer[fragmentPointer++ + Q_OVERLAP] = scaledSamples[y + 1];
            }
        }

        mSamplesPointer = offset;

        float[] samples = new float[FRAGMENT_SIZE * 2];
        FloatVector accumulator;
        FloatVector f1 = FloatVector.fromArray(VECTOR_SPECIES, COEFFICIENTS, 0);
        FloatVector f2 = FloatVector.fromArray(VECTOR_SPECIES, COEFFICIENTS, 2);
        FloatVector f3 = FloatVector.fromArray(VECTOR_SPECIES, COEFFICIENTS, 4);
        FloatVector f4 = FloatVector.fromArray(VECTOR_SPECIES, COEFFICIENTS, 6);
        FloatVector f5 = FloatVector.fromArray(VECTOR_SPECIES, COEFFICIENTS, 8);
        FloatVector f6 = FloatVector.fromArray(VECTOR_SPECIES, COEFFICIENTS, 10);
        FloatVector f7 = FloatVector.fromArray(VECTOR_SPECIES, COEFFICIENTS, 12);
        FloatVector f8 = FloatVector.fromArray(VECTOR_SPECIES, COEFFICIENTS, 14);
        FloatVector f9 = FloatVector.fromArray(VECTOR_SPECIES, COEFFICIENTS, 16);
        FloatVector f10 = FloatVector.fromArray(VECTOR_SPECIES, COEFFICIENTS, 18);
        FloatVector f11 = FloatVector.fromArray(VECTOR_SPECIES, COEFFICIENTS, 20);
        FloatVector f12 = FloatVector.fromArray(VECTOR_SPECIES, COEFFICIENTS, 22);

        for(int x = 0; x < FRAGMENT_SIZE; x++)
        {
            accumulator = FloatVector.zero(VECTOR_SPECIES);
            accumulator = f1.fma(FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x), accumulator);
            accumulator = f2.fma(FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x + 2), accumulator);
            accumulator = f3.fma(FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x + 4), accumulator);
            accumulator = f4.fma(FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x + 6), accumulator);
            accumulator = f5.fma(FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x + 8), accumulator);
            accumulator = f6.fma(FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x + 10), accumulator);
            accumulator = f7.fma(FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x + 12), accumulator);
            accumulator = f8.fma(FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x + 14), accumulator);
            accumulator = f9.fma(FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x + 16), accumulator);
            accumulator = f10.fma(FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x + 18), accumulator);
            accumulator = f11.fma(FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x + 20), accumulator);
            accumulator = f12.fma(FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x + 22), accumulator);

            //Perform FS/2 frequency translation on final filtered values ... multiply sequence by 1, -1, etc.
            if(x % 2 == 0)
            {
                samples[2 * x] = mIBuffer[x];
                samples[2 * x + 1] = accumulator.reduceLanes(VectorOperators.ADD);
            }
            else
            {
                samples[2 * x] = -mIBuffer[x];
                samples[2 * x + 1] = -accumulator.reduceLanes(VectorOperators.ADD);
            }
        }

        //Copy residual end samples to beginning of buffers for the next iteration
        System.arraycopy(mIBuffer, FRAGMENT_SIZE, mIBuffer, 0, I_OVERLAP);
        System.arraycopy(mQBuffer, FRAGMENT_SIZE, mQBuffer, 0, Q_OVERLAP);

        return new InterleavedComplexSamples(samples, timestamp);
    }
}