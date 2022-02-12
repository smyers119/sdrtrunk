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

package io.github.dsheirer.buffer;

import io.github.dsheirer.sample.complex.ComplexSamples;

import java.util.Arrays;

/**
 * Scalar implementation for non-packed Airspy native buffers
 */
public class AirspyBufferIteratorScalar extends AirspyBufferIterator<ComplexSamples>
{
    /**
     * Constructs an instance
     *
     * @param samples from the airspy, either packed or unpacked.
     * @param residualI samples from last buffer
     * @param residualQ samples from last buffer
     * @param timestamp of the buffer
     */
    public AirspyBufferIteratorScalar(byte[] samples, float[] residualI, float[] residualQ, long timestamp)
    {
        super(samples, residualI, residualQ, timestamp);
    }

    @Override
    public ComplexSamples next()
    {
        if(mSamplesPointer >= mSamples.length)
        {
            throw new IllegalStateException("End of buffer exceeded");
        }

        int samplesPointer = mSamplesPointer;

        int offset;
        float iSample, qSample;

        for(int x = 0; x < FRAGMENT_SIZE; x++)
        {
            offset = samplesPointer + (x * 4);

            iSample = convertAndScale(mSamples[offset + 1], mSamples[offset]);
            mIBuffer[I_OVERLAP + x] = iSample;

            qSample = convertAndScale(mSamples[offset + 3], mSamples[offset + 2]);
            mQBuffer[Q_OVERLAP + x] = qSample;
        }

        //The i line is a simple delay line, so we'll just copy those as a new array here.
        float[] i = Arrays.copyOf(mIBuffer, FRAGMENT_SIZE);
        float[] q = new float[FRAGMENT_SIZE];

        float accumulator;

        for(int x = 0; x < FRAGMENT_SIZE; x++)
        {
            accumulator = 0;

            for(int tap = 0; tap < COEFFICIENTS.length; tap++)
            {
                accumulator += COEFFICIENTS[tap] * mQBuffer[x + tap];
            }

            q[x] = accumulator;
        }

        //Copy residual end samples to beginning of buffers for the next iteration
        System.arraycopy(mIBuffer, FRAGMENT_SIZE, mIBuffer, 0, I_OVERLAP);
        System.arraycopy(mQBuffer, FRAGMENT_SIZE, mQBuffer, 0, Q_OVERLAP);

        mSamplesPointer += (FRAGMENT_SIZE * 4);

        return new ComplexSamples(i, q);
    }
}
