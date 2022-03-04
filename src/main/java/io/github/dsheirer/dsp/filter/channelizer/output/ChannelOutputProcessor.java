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
package io.github.dsheirer.dsp.filter.channelizer.output;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.OverflowableTransferQueue;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class ChannelOutputProcessor implements IPolyphaseChannelOutputProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelOutputProcessor.class);

    private OverflowableTransferQueue<List<float[]>> mChannelResultsQueue;
    private int mMaxResultsToProcess;
    private int mInputChannelCount;
    protected Listener<ComplexSamples> mComplexSamplesListener;

    /**
     * Base class for polyphase channelizer output channel processing.  Provides built-in frequency translation
     * oscillator support to apply frequency correction to the channel sample stream as requested by sample consumer.
     *
     * @param inputChannelCount is the number of input channels for this output processor
     * @param sampleRate of the output channel.  This is used to match the oscillator's sample rate to the output
     * channel sample rate for frequency translation/correction.
     */
    public ChannelOutputProcessor(int inputChannelCount, double sampleRate)
    {
        mInputChannelCount = inputChannelCount;
        mMaxResultsToProcess = (int)(sampleRate / 10) * 2;  //process at 100 millis interval, twice the expected inflow rate
        mChannelResultsQueue = new OverflowableTransferQueue<>((int)(sampleRate * 3), (int)(sampleRate * 0.5));
    }

    /**
     * Registers the listener to receive the assembled complex sample buffers from this processor.
     */
    @Override
    public void setListener(Listener<ComplexSamples> listener)
    {
        mComplexSamplesListener = listener;
    }

    @Override
    public int getPolyphaseChannelIndexCount()
    {
        return mInputChannelCount;
    }

    public void dispose()
    {
        if(mChannelResultsQueue != null)
        {
            mChannelResultsQueue.dispose();
        }
    }

    @Override
    public void receiveChannelResults(List<float[]> channelResultsList)
    {
        mChannelResultsQueue.offer(channelResultsList);
    }

    /**
     * Processes all enqueued polyphase channelizer results until the internal queue is empty
     */
    @Override
    public void processChannelResults()
    {
        List<List<float[]>> channelResultsToProcess = new ArrayList<>();
        int toProcess = mChannelResultsQueue.drainTo(channelResultsToProcess, mMaxResultsToProcess);

        if(toProcess > 0)
        {
            try
            {
                process(channelResultsToProcess);
            }
            catch(Throwable throwable)
            {
                mLog.error("Error while processing polyphase channel samples", throwable);
            }
        }
    }

    /**
     * Sub-class implementation to process one polyphase channelizer result array.
     * @param channelResults to process
     */
    public abstract void process(List<List<float[]>> channelResults);

    /**
     * Sets the overflow listener to monitor the internal channelizer channel results queue overflow state
     */
    public void setSourceOverflowListener(Source source)
    {
        mChannelResultsQueue.setSourceOverflowListener(source);
    }

    @Override
    public int getInputChannelCount()
    {
        return mInputChannelCount;
    }
}
