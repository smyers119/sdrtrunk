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
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;

import java.util.Iterator;

/**
 * Native buffer scalar implementation for Airspy non-packed samples.
 */
public class AirspyNativeBufferScalar implements INativeBuffer
{
    private byte[] mSamples;
    private float[] mResidualI;
    private float[] mResidualQ;
    private long mTimestamp;

    /**
     * Constructs an instance
     * @param samples (non-packed) from the airspy device
     * @param residualI samples from previous buffer
     * @param residualQ samples from previous buffer
     * @param timestamp of the buffer
     */
    public AirspyNativeBufferScalar(byte[] samples, float[] residualI, float[] residualQ, long timestamp)
    {
        //Ensure we're an even multiple of the fragment size.  Typically, this will be 64k or 128k
        if(samples.length % AirspyBufferIterator.FRAGMENT_SIZE != 0)
        {
            throw new IllegalArgumentException("Samples byte[] length [" + samples.length +
                    "] must be an even multiple of " + AirspyBufferIterator.FRAGMENT_SIZE);
        }

        mSamples = samples;
        mResidualI = residualI;
        mResidualQ = residualQ;
        mTimestamp = timestamp;
    }

    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    @Override
    public Iterator<ComplexSamples> iterator()
    {
        return new AirspyBufferIteratorScalar(mSamples, mResidualI, mResidualQ, mTimestamp);
    }

    @Override
    public Iterator<InterleavedComplexSamples> iteratorInterleaved()
    {
        return new AirspyInterleavedBufferIteratorScalar(mSamples, mResidualI, mResidualQ, mTimestamp);
    }
}