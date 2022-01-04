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
package io.github.dsheirer.dsp.filter.fir.real;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window;
import io.github.dsheirer.sample.buffer.ReusableBufferQueue;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;
import org.apache.commons.lang3.ArrayUtils;

import java.text.DecimalFormat;
import java.util.Random;

/**
 * Finite Impulse Response (FIR) filter for filtering individual float samples or float sample arrays.
 *
 * Note: filtering operations in this class are structured to leverage SIMD processor intrinsics when
 * available to the Java runtime.
 */
public class RealFIRFilter implements IRealFilter
{
    private ReusableBufferQueue mReusableBufferQueue = new ReusableBufferQueue("RealFIRFilter3");
    private float[] mBuffer;
    private float[] mCoefficients;
    private int mCoefficientsLengthMinus1;

    /**
     * Float sample FIR filter base class.
     *
     * @param coefficients - filter coefficients in normal order.
     */
    public RealFIRFilter(float[] coefficients)
    {
        //Reverse the order of the coefficients.
        mCoefficients = coefficients;
        ArrayUtils.reverse(mCoefficients);
        mCoefficientsLengthMinus1 = mCoefficients.length - 1;

        //We'll resize this later when we get the first sample buffer.  For now, make it non-null.
        mBuffer = new float[coefficients.length];
    }

    /**
     * Filters the sample array
     * @param samples to filter
     * @return filtered samples
     */
    public float[] filter(float[] samples)
    {
        int bufferLength = samples.length + mCoefficientsLengthMinus1;

        //Resize the data buffer if needed.  This shouldn't happen more than once since all buffers should be same size
        if(mBuffer.length != bufferLength)
        {
            float[] temp = new float[bufferLength];
            //Move residual samples from end of previous buffer to the beginning of the new temp buffer and reassign
            System.arraycopy(mBuffer, mBuffer.length - mCoefficientsLengthMinus1, temp, 0, mCoefficientsLengthMinus1);
            mBuffer = temp;
        }
        else
        {
            //Move residual samples from end of buffer to the beginning of the buffer
            System.arraycopy(mBuffer, samples.length, mBuffer, 0, mCoefficientsLengthMinus1);
        }

        //Copy new sample array into end of buffer
        System.arraycopy(samples, 0, mBuffer, mCoefficientsLengthMinus1, samples.length);

        float[] filtered = new float[samples.length];

        for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer++)
        {
            for(int coefficientPointer = 0; coefficientPointer < mCoefficients.length; coefficientPointer++)
            {
                filtered[bufferPointer] += mBuffer[bufferPointer + coefficientPointer] * mCoefficients[coefficientPointer];
            }
        }

        return filtered;
    }

    /**
     * Filters the samples contained in the unfilteredBuffer and returns a new reusable buffer with the
     * filtered samples.
     *
     * Note: user count on the returned (new) buffer is set to one and the user count is decremented on
     * the unfiltered buffer argument.
     *
     * @param unfilteredBuffer containing a sample array to be filtered
     * @return a new reusable buffer with the filtered samples.
     */
    public ReusableFloatBuffer filter(ReusableFloatBuffer unfilteredBuffer)
    {
        float[] filtered = filter(unfilteredBuffer.getSamples());
        ReusableFloatBuffer filteredBuffer = mReusableBufferQueue.getBuffer(filtered, unfilteredBuffer.getTimestamp());
        unfilteredBuffer.decrementUserCount();
        return filteredBuffer;
    }

    public static void main(String[] args)
    {
        Random random = new Random();

        int sampleSize = 2048;

        float[] samples = new float[sampleSize];
        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = random.nextFloat() * 2.0f - 1.0f;
        }

        float[] coefficients = FilterFactory.getLowPass(1000, 250, 99, Window.WindowType.BLACKMAN);

        RealFIRFilter filter = new RealFIRFilter(coefficients);
        VectorRealFIRFilter256Bit vectorFilter = new VectorRealFIRFilter256Bit(coefficients);

        double accumulator = 0.0d;

        int iterations = 1_000_000;

        long start = System.currentTimeMillis();

        for(int x = 0; x < iterations; x++)
        {
//            float[] filtered = filter.filter(samples);
            float[] filtered = vectorFilter.filter(samples);
//            float[] vfiltered = vectorFilter.filter(samples);
            accumulator += filtered[3];
        }

//        System.out.println("REG:" + Arrays.toString(filtered));
//        System.out.println("VEC:" + Arrays.toString(vfiltered));

        double elapsed = System.currentTimeMillis() - start;

        DecimalFormat df = new DecimalFormat("0.000");
        System.out.println("Accumulator: " + accumulator);
        System.out.println("Test Complete.  Elapsed Time: " + df.format(elapsed / 1000.0d) + " seconds");
    }
}