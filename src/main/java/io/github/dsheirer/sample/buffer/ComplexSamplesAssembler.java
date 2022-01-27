package io.github.dsheirer.sample.buffer;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSampleListener;
import io.github.dsheirer.sample.complex.ComplexSamples;

public class ComplexSamplesAssembler implements ComplexSampleListener
{
    private static final int BUFFER_SIZE = 2048;

    private Listener<ComplexSamples> mListener;
    private float[] mISamples = new float[BUFFER_SIZE];
    private float[] mQSamples = new float[BUFFER_SIZE];
    private int mPointer = 0;

    @Override public void receive(float i, float q)
    {
        mISamples[mPointer] = i;
        mQSamples[mPointer] = q;

        mPointer++;

        if(mPointer >= BUFFER_SIZE)
        {
            if(mListener != null)
            {
                mListener.receive(new ComplexSamples(mISamples, mQSamples));
            }

            mISamples = new float[BUFFER_SIZE];
            mQSamples = new float[BUFFER_SIZE];
            mPointer = 0;
        }
    }

    /**
     * Set the listener to receive assembled buffers
     */
    public void setListener(Listener<ComplexSamples> listener)
    {
        mListener = listener;
    }
}
