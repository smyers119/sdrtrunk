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

package io.github.dsheirer.dsp.mixer;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * Real oscillator that uses SIMD vector intrinsics from JDK17+ Project Panama.
 */
public class VectorRealOscillator extends AbstractRealOscillator
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_256;
    private float[] mOffsets;

    public VectorRealOscillator(double frequency, double sampleRate)
    {
        super(frequency, sampleRate);

        mOffsets = new float[VECTOR_SPECIES.length()];
        for(int x = 0; x < mOffsets.length; x++)
        {
            mOffsets[x] = x + 1.0f;
        }
    }

    @Override
    public float[] generate(int sampleCount)
    {
        float[] samples = new float[sampleCount];

        for(int samplePointer = 0; samplePointer < sampleCount; samplePointer += VECTOR_SPECIES.length())
        {
            FloatVector generated = FloatVector.fromArray(VECTOR_SPECIES, mOffsets, 0);
            generated = generated.mul(mAnglePerSample);
            generated = generated.add(mCurrentPhase);
            mCurrentPhase = generated.lane(VECTOR_SPECIES.length() - 1);
            mCurrentPhase %= TWO_PI;
            generated = generated.lanewise(VectorOperators.SIN);
            generated.intoArray(samples, samplePointer);
        }

        return samples;
    }

    @Override
    public float[] mix(float[] complexSamples)
    {
        return new float[0];
    }

    public static void main(String[] args)
    {
        double frequency = 5.0d;
        double sampleRate = 100.0d;
        int samplesToGenerate = 2048;
        int iterations = 10_000_000;

        boolean validation = false;

        Oscillator legacyO = new Oscillator(frequency, sampleRate);
        RealOscillator scalarO = new RealOscillator(frequency, sampleRate);
        VectorRealOscillator vectorO = new VectorRealOscillator(frequency, sampleRate);

        if(validation)
        {
            float[] legacySamples = legacyO.generateReal(samplesToGenerate);
            float[] scalarSamples = scalarO.generate(samplesToGenerate);
            float[] vectorSamples = vectorO.generate(samplesToGenerate);
            System.out.println("LEGACY:" + Arrays.toString(legacySamples));
            System.out.println("SCALAR:" + Arrays.toString(scalarSamples));
            System.out.println("VECTOR:" + Arrays.toString(vectorSamples));
        }
        else
        {
            System.out.println("Test Starting ...");
            long start = System.currentTimeMillis();

            double accumulator = 0.0;

            for(int i = 0; i < iterations; i++)
            {
//                float[] samples = legacyO.generateReal(samplesToGenerate);
//                float[] samples = scalarO.generate(samplesToGenerate);
                float[] samples = vectorO.generate(samplesToGenerate);
                accumulator += samples[3];
            }

            double elapsed = System.currentTimeMillis() - start;

            DecimalFormat df = new DecimalFormat("0.000");
            System.out.println("Accumulator: " + accumulator);
            System.out.println("Test Complete.  Elapsed Time: " + df.format(elapsed / 1000.0d) + " seconds");
        }
    }
}
