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

package io.github.dsheirer.dsp.filter.decimate;

import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;

/**
 * Wrapper to add reusable buffer support to a complex decimation filter
 */
public class ComplexDecimationReusableBufferWrapper
{
    private IComplexDecimationFilter mFilter;
    private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("complex decimation filter wrapper");

    public ComplexDecimationReusableBufferWrapper(IComplexDecimationFilter filter)
    {
        mFilter = filter;
    }

    /**
     * Decimates the complex samples and returns a buffer of decimated samples.
     * @param buffer to decimate
     * @return decimated buffer.
     */
    public ReusableComplexBuffer decimate(ReusableComplexBuffer buffer)
    {
        float[] decimated = mFilter.decimateComplex(buffer.getSamples());
        buffer.decrementUserCount();
        return mReusableComplexBufferQueue.getBuffer(decimated, buffer.getTimestamp());
    }
}
