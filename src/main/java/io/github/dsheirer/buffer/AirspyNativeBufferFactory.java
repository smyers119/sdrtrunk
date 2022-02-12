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

import java.util.Arrays;

/**
 * Implements a factory for creating SignedByteNativeBuffer instances
 */
public class AirspyNativeBufferFactory implements INativeBufferFactory
{
    private static final int BYTES_PER_SAMPLE_PACKED = 3;
    private static final int BYTES_PER_SAMPLE_UNPACKED = 4;

    private boolean mSamplePacking = false;
    private float[] mResidualI = new float[AirspyBufferIterator.I_OVERLAP];
    private float[] mResidualQ = new float[AirspyBufferIterator.Q_OVERLAP];

    /**
     * Constructs an instance
     */
    public AirspyNativeBufferFactory()
    {
    }

    /**
     * Sample packing places two 12-bit samples into 3 bytes when enabled or
     * places two 12-bit samples into 4 bytes when disabled.
     *
     * @param enabled
     */
    public void setSamplePacking(boolean enabled)
    {
        mSamplePacking = enabled;
    }

    @Override
    public INativeBuffer getBuffer(byte[] samples, long timestamp)
    {
        //TODO: handle packed vs non-packed, and switch on SCALAR vs VECTOR implementations
        INativeBuffer buffer = new AirspyNativeBufferScalar(samples,
                Arrays.copyOf(mResidualI, mResidualI.length),
                Arrays.copyOf(mResidualQ, mResidualQ.length), timestamp);

        extractResidual(samples);

        return buffer;
    }

    /**
     * Extracts the residual overlap samples needed for continuity in the Hilbert transform filter.
     * @param samples to extract residual from
     */
    private void extractResidual(byte[] samples)
    {
        if(mSamplePacking)
        {
            //TODO: this ...
        }
        else
        {
            int offset = samples.length - (AirspyBufferIterator.I_OVERLAP * BYTES_PER_SAMPLE_UNPACKED);

            for(int i = 0; i < AirspyBufferIterator.I_OVERLAP; i++)
            {
                mResidualI[i] = AirspyBufferIterator.convertAndScale(samples[offset], samples[offset + 1]);
                offset += BYTES_PER_SAMPLE_UNPACKED;
            }

            offset = samples.length - (AirspyBufferIterator.Q_OVERLAP * BYTES_PER_SAMPLE_UNPACKED) + 2;

            for(int q = 0; q < AirspyBufferIterator.Q_OVERLAP; q++)
            {
                mResidualQ[q] = AirspyBufferIterator.convertAndScale(samples[offset], samples[offset + 1]);
                offset += BYTES_PER_SAMPLE_UNPACKED;
            }
        }
    }
}
