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

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window;
import io.github.dsheirer.dsp.filter.decimate.IComplexDecimationFilter;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;

/**
 * Complex half-band filter that processes samples on a per-array basis, versus a per-sample basis.
 */
public class ComplexHalfBandDecimationFilter implements IComplexDecimationFilter
{
    private static final float CENTER_COEFFICIENT = 0.5f;
    private float[] mCoefficients;
    private float[] mBuffer;
    private int mBufferOverlap;

    /**
     * Constructs a complex sample half-band decimate x2 filter using the specified filter coefficients.
     * @param coefficients for the half-band filter where the middle coefficient is 0.5, and the even-numbered
     * coefficients are non-zero and symmetrical, and the odd-numbered coefficients are all zero valued.
     */
    public ComplexHalfBandDecimationFilter(float[] coefficients)
    {
        if((coefficients.length + 1) % 4 != 0)
        {
            throw new IllegalArgumentException("Half-band filter coefficients must be odd-length and " +
                    "symmetrical where L = [x * 4 - 1]");
        }

        mCoefficients = new float[coefficients.length * 2];
        for(int x = 0; x < coefficients.length; x++)
        {
            mCoefficients[2 * x] = coefficients[x];
            mCoefficients[2 * x + 1] = coefficients[x];
        }

        mBufferOverlap = mCoefficients.length - 2;
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

        int half = mCoefficients.length / 2 - 1;
        int halfPlus1 = half + 1;

        for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer += 4)
        {
            float iAccumulator = 0.0f;
            float qAccumulator = 0.0f;

            for(int coefficientPointer = 0; coefficientPointer < half; coefficientPointer += 4)
            {
                //Half band filter coefficients are mirrored, so we add the mirrored samples and then multiply by
                //one of the coefficients to achieve the same effect.
                iAccumulator += mCoefficients[coefficientPointer] *
                        (mBuffer[bufferPointer + coefficientPointer] +
                                mBuffer[bufferPointer + (mBufferOverlap - coefficientPointer)]);

                qAccumulator += mCoefficients[coefficientPointer] *
                        (mBuffer[bufferPointer + coefficientPointer + 1] +
                                mBuffer[bufferPointer + (mBufferOverlap - coefficientPointer) + 1]);
            }

            iAccumulator += mBuffer[bufferPointer + half] * CENTER_COEFFICIENT;
            qAccumulator += mBuffer[bufferPointer + halfPlus1] * CENTER_COEFFICIENT;

            filtered[bufferPointer / 2] = iAccumulator;
            filtered[bufferPointer / 2 + 1] = qAccumulator;
        }

        return filtered;
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

        float[] coefficients = FilterFactory.getHalfBand(23, Window.WindowType.BLACKMAN);

//        ComplexHalfBandDecimationFilter filter = new ComplexHalfBandDecimationFilter(coefficients);
//        VectorComplexHalfBandDecimationFilterDefaultBit vectorFilter = new VectorComplexHalfBandDecimationFilterDefaultBit(coefficients);
        VectorComplexHalfBandDecimationFilter23Tap512Bit vectorFilter = new VectorComplexHalfBandDecimationFilter23Tap512Bit(coefficients);

        double accumulator = 0.0d;

        int iterations = 10_000_000;

        long start = System.currentTimeMillis();

        for(int x = 0; x < iterations; x++)
        {
//            float[] filtered = filter.decimateComplex(samples);
            float[] filtered = vectorFilter.decimateComplex(samples);
//            float[] vfiltered = vectorFilter.decimateComplex(samples);
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
