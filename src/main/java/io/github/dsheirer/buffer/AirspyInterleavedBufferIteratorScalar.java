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

import io.github.dsheirer.sample.complex.InterleavedComplexSamples;

/**
 * Scalar implementation for non-packed Airspy native buffers
 */
public class AirspyInterleavedBufferIteratorScalar extends AirspyBufferIterator<InterleavedComplexSamples>
{
    /**
     * Constructs an instance
     *
     * @param samples from the airspy, either packed or unpacked.
     * @param residualI samples from last buffer
     * @param residualQ samples from last buffer
     */
    public AirspyInterleavedBufferIteratorScalar(byte[] samples, float[] residualI, float[] residualQ, long timestamp)
    {
        super(samples, residualI, residualQ, timestamp);
    }

    @Override
    public InterleavedComplexSamples next()
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

        float[] samples = new float[FRAGMENT_SIZE * 2];

        float accumulator;

        for(int x = 0; x < FRAGMENT_SIZE; x++)
        {
            samples[2 * x] = mIBuffer[x];
            accumulator = 0;

            for(int tap = 0; tap < COEFFICIENTS.length; tap++)
            {
                accumulator += COEFFICIENTS[tap] * mQBuffer[x + tap];
            }

            samples[2 * x + 1] = accumulator;
        }

        //Copy residual end samples to beginning of buffers for the next iteration
        System.arraycopy(mIBuffer, FRAGMENT_SIZE, mIBuffer, 0, I_OVERLAP);
        System.arraycopy(mQBuffer, FRAGMENT_SIZE, mQBuffer, 0, Q_OVERLAP);

        mSamplesPointer += (FRAGMENT_SIZE * 4);

        return new InterleavedComplexSamples(samples, mTimestamp);
    }
}
