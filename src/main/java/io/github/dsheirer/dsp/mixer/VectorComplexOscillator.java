package io.github.dsheirer.dsp.mixer;

import io.github.dsheirer.dsp.filter.vector.VectorUtilities;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import org.apache.commons.math3.util.FastMath;

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * Complex oscillator that uses JDK17 SIMD vector operations to generate complex sample arrays.
 *
 * Note: this class uses a bank of oscillators that are each rotated synchronously, where the oscillator is similar to
 * the ComplexOscillator class, but where each oscillator is offset in phase by one sample more than the previous and
 * the entire bank is rotated at the sample phase times the SIMD lane width for each sample generation increment.
 */
public class VectorComplexOscillator extends BaseOscillator implements IComplexOscillator
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;

    private float[] mPreviousInphases;
    private float[] mPreviousQuadratures;
    private float[] mGainInitials; //Set to 3.0f as the first constant in the gain calculation

    /**
     * Constructs an instance
     *
     * @param frequency  in hertz
     * @param sampleRate in hertz
     */
    public VectorComplexOscillator(double frequency, double sampleRate)
    {
        super(frequency, sampleRate);

        mGainInitials = new float[VECTOR_SPECIES.length()];
        Arrays.fill(mGainInitials, 3.0f);
    }

    @Override
    protected void update()
    {
        super.update();

        float cosineAngle = (float)FastMath.cos(getAnglePerSample());
        float sineAngle = (float)FastMath.sin(getAnglePerSample());

        if(mPreviousInphases == null || mPreviousQuadratures == null)
        {
            mPreviousInphases = new float[VECTOR_SPECIES.length()];
            mPreviousQuadratures = new float[VECTOR_SPECIES.length()];

            mPreviousInphases[0] = 1.0f;
        }

        float gain;

        //Setup the previous sample arrays where each index is offset one sample of rotation from the previous sample.
        // We don't touch index 0 so that it can maintain the previous phase offset and all other indices are updated
        // relative to index 0.
        for(int x = 1; x < VECTOR_SPECIES.length(); x++)
        {
            gain = (3.0f - ((mPreviousInphases[x - 1] * mPreviousInphases[x - 1]) +
                    (mPreviousQuadratures[x - 1] * mPreviousQuadratures[x - 1]))) / 2.0f;
            mPreviousInphases[x] = ((mPreviousInphases[x - 1] * cosineAngle) - (mPreviousQuadratures[x - 1] * sineAngle)) * gain;
            mPreviousQuadratures[x] = ((mPreviousInphases[x - 1] * sineAngle) + (mPreviousQuadratures[x -1] * cosineAngle)) * gain;
        }
    }

    /**
     * Generates complex samples.
     * @param sampleCount number of samples to generate and length of the resulting float array.
     * @return generated samples
     */
    @Override public float[] generate(int sampleCount)
    {
        if(sampleCount % VECTOR_SPECIES.length() != 0)
        {
            throw new IllegalArgumentException("Requested sample count [" + sampleCount +
                    "] must be a power of 2 and a multiple of the SIMD lane width [" + VECTOR_SPECIES.length() + "]");
        }

        float[] samples = new float[sampleCount * 2];

        FloatVector previousInphase = FloatVector.fromArray(VECTOR_SPECIES, mPreviousInphases, 0);
        FloatVector previousQuadrature = FloatVector.fromArray(VECTOR_SPECIES, mPreviousQuadratures, 0);
        FloatVector gainInitials = FloatVector.fromArray(VECTOR_SPECIES, mGainInitials, 0);

        //Sine and cosine angle per sample, with the rotation angle multiplied by the SIMD lane width
        float cosAngle = (float)(FastMath.cos(getAnglePerSample() * VECTOR_SPECIES.length()));
        float sinAngle = (float)(FastMath.sin(getAnglePerSample() * VECTOR_SPECIES.length()));

        int gainCounter = 0;

        FloatVector gain, inphase, quadrature;

        for(int samplePointer = 0; samplePointer < sampleCount; samplePointer += VECTOR_SPECIES.length())
        {
            if(++gainCounter % 100 == 0)
            {
                gainCounter = 0;
                gain = gainInitials.sub(previousInphase.pow(2.0f).add(previousQuadrature.pow(2.0f))).div(2.0f);
                inphase = previousInphase.mul(cosAngle).sub(previousQuadrature.mul(sinAngle)).mul(gain);
                quadrature = previousInphase.mul(sinAngle).add(previousQuadrature.mul(cosAngle)).mul(gain);
            }
            else
            {
                inphase = previousInphase.mul(cosAngle).sub(previousQuadrature.mul(sinAngle));
                quadrature = previousInphase.mul(sinAngle).add(previousQuadrature.mul(cosAngle));
            }

            float[] interleaved = VectorUtilities.interleave(inphase, quadrature);
            System.arraycopy(interleaved, 0, samples, samplePointer * 2, interleaved.length);

            previousInphase = inphase;
            previousQuadrature = quadrature;
        }

        return samples;
    }

    public static void main(String[] args)
    {
        double frequency = 5.0d;
        double sampleRate = 100.0d;
        int samplesToGenerate = 2048;
        int iterations = 10_000_000;

        boolean validation = true;

        LowPhaseNoiseOscillator legacy = new LowPhaseNoiseOscillator(frequency, sampleRate);
        ComplexOscillator scalar = new ComplexOscillator(frequency, sampleRate);
        VectorComplexOscillator vector = new VectorComplexOscillator(frequency, sampleRate);

        if(validation)
        {
            float[] legacySamples = legacy.generateComplex(samplesToGenerate);
            float[] scalarSamples = scalar.generate(samplesToGenerate);
            float[] vector2Samples = vector.generate(samplesToGenerate);
            System.out.println("LEGACY:" + Arrays.toString(legacySamples));
            System.out.println("SCALAR:" + Arrays.toString(scalarSamples));
            System.out.println("VECTOR:" + Arrays.toString(vector2Samples));
        }
        else
        {
            System.out.println("Test Starting ...");
            long start = System.currentTimeMillis();

            double accumulator = 0.0;

            for(int i = 0; i < iterations; i++)
            {
                //                float[] samples = legacy.generateComplex(samplesToGenerate);
                //                float[] samples = scalar.generate(samplesToGenerate);
                float[] samples = vector.generate(samplesToGenerate);
                accumulator += samples[3];
            }

            double elapsed = System.currentTimeMillis() - start;

            DecimalFormat df = new DecimalFormat("0.000");
            System.out.println("Accumulator: " + accumulator);
            System.out.println("Test Complete.  Elapsed Time: " + df.format(elapsed / 1000.0d) + " seconds");
        }
    }
}
