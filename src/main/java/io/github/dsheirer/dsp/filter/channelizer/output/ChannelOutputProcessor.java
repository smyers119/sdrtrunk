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

import io.github.dsheirer.dsp.gain.complex.ComplexGain;
import io.github.dsheirer.dsp.gain.complex.ScalarComplexGain;
import io.github.dsheirer.dsp.mixer.ComplexMixer;
import io.github.dsheirer.dsp.mixer.ScalarComplexMixer;
import io.github.dsheirer.sample.IOverflowListener;
import io.github.dsheirer.sample.buffer.ComplexSamplesAssembler;
import io.github.dsheirer.sample.buffer.OverflowableReusableBufferTransferQueue;
import io.github.dsheirer.sample.buffer.ReusableChannelResultsBuffer;
import io.github.dsheirer.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class ChannelOutputProcessor implements IPolyphaseChannelOutputProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelOutputProcessor.class);

    private OverflowableReusableBufferTransferQueue<ReusableChannelResultsBuffer> mChannelResultsQueue;
    private List<ReusableChannelResultsBuffer> mChannelResultsToProcess = new ArrayList<>();
    private int mMaxResultsToProcess;

    private int mInputChannelCount;
    private ScalarComplexMixer mFrequencyCorrectionMixer;
    private boolean mFrequencyCorrectionEnabled;
    private ComplexGain mComplexGain;

    /**
     * Base class for polyphase channelizer output channel processing.  Provides built-in frequency translation
     * oscillator support to apply frequency correction to the channel sample stream as requested by sample consumer.
     *
     * @param inputChannelCount is the number of input channels for this output processor
     * @param sampleRate of the output channel.  This is used to match the oscillator's sample rate to the output
     * channel sample rate for frequency translation/correction.
     */
    public ChannelOutputProcessor(int inputChannelCount, double sampleRate, float gain)
    {
        mInputChannelCount = inputChannelCount;

        //We have to use scalar implementation for gain and mixer ... the vector implementation requires the generated
        //sample count to be a multiple of the SIMD lane width and we can't guarantee that.
        mComplexGain = new ScalarComplexGain(gain);
        mFrequencyCorrectionMixer = new ScalarComplexMixer(0, sampleRate, true);

        mMaxResultsToProcess = (int)(sampleRate / 10) * 2;  //process at 100 millis interval, twice the expected inflow rate

        mChannelResultsQueue = new OverflowableReusableBufferTransferQueue<>((int)(sampleRate * 3), (int)(sampleRate * 0.5));
    }

    protected ComplexGain getGain()
    {
        return mComplexGain;
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

    /**
     * Sets the frequency offset to apply to the incoming samples to mix the desired signal to baseband.
     * @param frequencyOffset in hertz
     */
    @Override
    public void setFrequencyOffset(long frequencyOffset)
    {
        mFrequencyCorrectionMixer.setFrequency(frequencyOffset);
        mFrequencyCorrectionEnabled = (frequencyOffset != 0);
    }

    /**
     * Oscillator/mixer to use for frequency correction against incoming sample stream
     */
    protected ComplexMixer getFrequencyCorrectionMixer()
    {
        return mFrequencyCorrectionMixer;
    }

    @Override
    public void receiveChannelResults(ReusableChannelResultsBuffer channelResults)
    {
        mChannelResultsQueue.offer(channelResults);
    }

    /**
     * Processes all enqueued polyphase channelizer results until the internal queue is empty
     * @param assembler to receive the processed channel results
     */
    @Override
    public void processChannelResults(ComplexSamplesAssembler assembler)
    {
        try
        {
            int toProcess = mChannelResultsQueue.drainTo(mChannelResultsToProcess, mMaxResultsToProcess);

            if(toProcess > 0)
            {
                process(mChannelResultsToProcess, assembler);
            }
        }
        catch(Throwable throwable)
        {
            mLog.error("Error while processing polyphase channel samples", throwable);
        }

        mChannelResultsToProcess.clear();
    }

    /**
     * Sub-class implementation to process one polyphase channelizer result array.
     * @param channelResults to process
     * @param assembler to receive the processed channelizer results
     */
    public abstract void process(List<ReusableChannelResultsBuffer> channelResults,
                                 ComplexSamplesAssembler assembler);


    /**
     * Sets the overflow listener to monitor the internal channelizer channel results queue overflow state
     */
    public void setSourceOverflowListener(Source source)
    {
        mChannelResultsQueue.setSourceOverflowListener(source);
    }

    /**
     * Removes the overflow listener from monitoring the internal channelizer channel results queue overflow state
     */
    public void removeOverflowListener(IOverflowListener listener)
    {
        mChannelResultsQueue.setOverflowListener(null);
    }

    @Override
    public int getInputChannelCount()
    {
        return mInputChannelCount;
    }

    /**
     * Indicates if this channel has a frequency correction offset
     */
    public boolean hasFrequencyCorrection()
    {
        return mFrequencyCorrectionEnabled;
    }
}
