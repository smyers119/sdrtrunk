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

import io.github.dsheirer.dsp.filter.hilbert.HilbertTransform;

import java.util.Iterator;

/**
 * Base complex samples iterator for raw Airspy sample buffers.  Incorporates a Hilbert transform filter
 * for converting the real sample array to complex samples.
 * @param <T> either ComplexSamples or InterleavedComplexSamples
 */
public abstract class AirspyBufferIterator<T> implements Iterator<T>
{
    protected static final float SCALE_SIGNED_12_BIT_TO_FLOAT = 1.0f / 2048.0f;
    protected static final int FRAGMENT_SIZE = 2048;
    protected static final float[] COEFFICIENTS =
            HilbertTransform.convertHalfBandToHilbert(HilbertTransform.HALF_BAND_FILTER_47_TAP);

    public static final int I_OVERLAP = 11;
    public static final int Q_OVERLAP = 23;

    protected float[] mIBuffer = new float[FRAGMENT_SIZE + I_OVERLAP];
    protected float[] mQBuffer = new float[FRAGMENT_SIZE + Q_OVERLAP];
    protected byte[] mSamples;
    protected int mSamplesPointer = 0;
    protected long mTimestamp;

    /**
     * Constructs an instance
     * @param samples from the airspy, either packed or unpacked.
     */
    public AirspyBufferIterator(byte[] samples, float[] residualI, float[] residualQ, long timestamp)
    {
        if(residualI.length != I_OVERLAP || residualQ.length != Q_OVERLAP)
        {
            throw new IllegalArgumentException("Residual I length [" + residualI.length +
                    "] must be " + I_OVERLAP + " and Residual Q length [" + residualQ.length +
                    "] must be " + Q_OVERLAP);
        }

        int requiredInterval = FRAGMENT_SIZE * 4; //requires 4 bytes (2 samples) per fragment

        if(samples.length % requiredInterval != 0)
        {
            throw new IllegalArgumentException("Sample byte array length [" + mSamples.length +
                    "]must be an integer multiple of " + requiredInterval);
        }

        mSamples = samples;
        System.arraycopy(residualI, 0, mIBuffer, 0, residualI.length);
        System.arraycopy(residualQ, 0, mQBuffer, 0, residualQ.length);
        mTimestamp = timestamp;
    }

    @Override
    public boolean hasNext()
    {
        return mSamplesPointer < mSamples.length;
    }

    /**
     * Converts the two-byte sample into a signed 32-bit float sample that has not been scaled.
     * @param msb most significant byte
     * @param lsb least significant byte
     * @return converted sample
     */
    public static float convert(byte msb, byte lsb)
    {
        return (((lsb & 0xFF) | (msb << 8)) & 0xFFF) - 2048.0f;
    }

    /**
     * Converts the two-byte sample into a signed and scaled 32-bit float sample
     * @param msb most significant byte
     * @param lsb least significant byte
     * @return converted sample
     */
    public static float convertAndScale(byte msb, byte lsb)
    {
        return convert(msb, lsb) * SCALE_SIGNED_12_BIT_TO_FLOAT;
    }
}
