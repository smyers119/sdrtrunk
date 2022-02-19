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
package io.github.dsheirer.dsp.filter.resample;

import com.laszlosystems.libresample4j.Resampler;
import com.laszlosystems.libresample4j.SampleBuffers;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class RealResampler
{
    protected static final Logger mLog = LoggerFactory.getLogger(RealResampler.class);

    private Resampler mResampler;
    private Listener<float[]> mResampledListener;
    private BufferManager mBufferManager;
    private double mResampleFactor;

    /**
     * Resampler for real sample buffers.
     * @param inputRate
     * @param outputRate
     */
    public RealResampler(double inputRate, double outputRate, int inputBufferSize, int outputBufferSize)
    {
        mResampleFactor = outputRate / inputRate;
        mResampler = new Resampler(true, mResampleFactor, mResampleFactor);
        mBufferManager = new BufferManager(inputBufferSize, outputBufferSize);
    }

    /**
     * Primary input method to the resampler
     * @param samples to resample
     */
    public void resample(float[] samples)
    {
        mBufferManager.load(samples);
        mResampler.process(mResampleFactor, mBufferManager, false);
    }

    /**
     * Registers the listener to receive the resampled buffer output
     * @param listener to receive buffers
     */
    public void setListener(Listener<float[]> listener)
    {
        mResampledListener = listener;
    }

    /**
     * Buffer manager for input and output buffers used during the resampling process.  The manager will
     * auto-resize the input and output buffers to meet the size of the incoming sample buffers.
     */
    public class BufferManager implements SampleBuffers
    {
        private int mOutputBufferSize;
        private FloatBuffer mInputBuffer;
        private FloatBuffer mOutputBuffer;

        public BufferManager(int inputBufferSize, int outputBufferSize)
        {
            mOutputBufferSize = outputBufferSize;

            //The size of the underlying byte buffer in bytes is 4 times the requested input buffer size in floats
            mInputBuffer = ByteBuffer.allocate(inputBufferSize * 4).asFloatBuffer();
            mOutputBuffer = ByteBuffer.allocate(inputBufferSize * 4).asFloatBuffer();
        }

        /**
         * Queues the buffer sample for resampling
         */
        public void load(float[] samples)
        {
            mInputBuffer.put(samples);
        }

        @Override
        public int getInputBufferLength()
        {
            return mInputBuffer.position();
        }

        @Override
        public int getOutputBufferLength()
        {
            return mOutputBuffer.remaining();
        }

        /**
         * Provides input to the resampler
         */
        @Override
        public void produceInput(float[] samples, int offset, int length)
        {
            mInputBuffer.flip();
            mInputBuffer.get(samples, offset, length);
            mInputBuffer.compact();
        }

        /**
         * Receives output from the resampler and dispatches output buffers periodically.
         */
        @Override
        public void consumeOutput(float[] samples, int offset, int length)
        {
            mOutputBuffer.put(samples, offset, length);

            while(mOutputBuffer.position() > mOutputBufferSize)
            {
                float[] resampled = new float[mOutputBufferSize];

                mOutputBuffer.flip();
                mOutputBuffer.get(resampled);
                mOutputBuffer.compact();

                if(mResampledListener != null)
                {
                    mResampledListener.receive(resampled);
                }
            }
        }
    }
}
