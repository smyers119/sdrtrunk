package io.github.dsheirer.dsp.mixer;

import io.github.dsheirer.dsp.oscillator.IComplexOscillator;
import io.github.dsheirer.dsp.oscillator.OscillatorFactory;
import io.github.dsheirer.sample.complex.ComplexSamples;

import java.util.Arrays;

/**
 * Scalar implementation of a complex mixer
 */
public class ScalarComplexMixer extends ComplexMixer
{
    /**
     * Constructs an instance
     * @param frequency of the mixing oscillator
     * @param sampleRate of the mixing oscillator
     */
    public ScalarComplexMixer(double frequency, double sampleRate)
    {
        super(frequency, sampleRate);
    }

    /**
     * Mixes the complex I & Q samples with samples generated from an oscillator.
     * @param iSamples complex samples to mix
     * @param qSamples complex samples to mix
     * @return mixed samples
     */
    @Override public ComplexSamples mix(float[] iSamples, float[] qSamples)
    {
        ComplexSamples mixer = generate(iSamples.length);

        float[] iMixer = mixer.i();
        float[] qMixer = mixer.q();

        float inphase, quadrature;

        for(int x = 0; x < iSamples.length; x++)
        {
            inphase = (iMixer[x] * iSamples[x]) - (qMixer[x] * qSamples[x]);
            quadrature = (qMixer[x] * iSamples[x]) + (iMixer[x] * qSamples[x]);
            iMixer[x] = inphase;
            qMixer[x] = quadrature;
        }

        return mixer;
    }

    public static void main(String[] args)
    {
        IComplexOscillator oscillator = OscillatorFactory.getComplexOscillator(2, 10);
        ComplexSamples samples = oscillator.generateComplexSamples(32);

        double mixFrequency = 3.0;
        double mixSampleRate = 10.0;

        ComplexMixer scalar = new ScalarComplexMixer(mixFrequency, mixSampleRate);
        ComplexMixer vector = new VectorComplexMixer(mixFrequency, mixSampleRate);

        boolean validate = true;

        if(validate)
        {
            ComplexSamples scalarMixed = scalar.mix(samples);
            ComplexSamples vectorMixed = vector.mix(samples);

            System.out.println("Samples I:" + Arrays.toString(samples.i()));
            System.out.println("Samples Q:" + Arrays.toString(samples.q()));
            System.out.println("Scalar  I:" + Arrays.toString(scalarMixed.i()));
            System.out.println("Scalar  Q:" + Arrays.toString(scalarMixed.q()));
            System.out.println("Vector  I:" + Arrays.toString(vectorMixed.i()));
            System.out.println("Vector  Q:" + Arrays.toString(vectorMixed.q()));
        }
    }
}
