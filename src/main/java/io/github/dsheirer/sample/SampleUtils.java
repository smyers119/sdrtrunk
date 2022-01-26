package io.github.dsheirer.sample;

import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexSamples;
import org.apache.commons.math3.util.FastMath;

import java.util.Random;

public class SampleUtils
{
	public static final double TWO_PI = Math.PI * 2.0;

	/**
	 * Converts from an interleaved complex sample array to a sample record with the
	 * I and Q in separate arrays.
	 * @param samples that are interleaved complex samples
	 * @return deinterleaved complex samples instance
	 */
	public static ComplexSamples deinterleave(float[] samples)
	{
		float[] i = new float[samples.length / 2];
		float[] q = new float[samples.length / 2];

		int offset;

		for(int x = 0; x < i.length; x++)
		{
			offset = 2 * x;
			i[x] = samples[offset];
			q[x] = samples[offset + 1];
		}

		return new ComplexSamples(i, q);
	}

	/**
	 * Generates a buffer of complex samples where each sample's vector length is unity (1.0)
	 * @param count of buffer samples
	 * @return complex buffer
	 */
	public static ComplexSamples generateComplex(int count)
	{
		Random random = new Random();

		float[] i = new float[count];
		float[] q = new float[count];

		for(int x = 0; x < i.length; x++)
		{
			//Random angle in radians in range of -PI to + PI
			double angle = random.nextDouble() * TWO_PI - Math.PI;

			i[x] = (float)Math.cos(angle);
			q[x] = (float)Math.sin(angle);
		}

		return new ComplexSamples(i, q);
	}

	/**
	 * Generates random complex samples with vector length in range 0.5 to 1.0 (unity).
	 * @param count of samples
	 * @return complex sample buffer
	 */
	public static ComplexSamples generateComplexRandomVectorLength(int count)
	{
		Random random = new Random();
		ComplexSamples unitySamples = generateComplex(count);
		float[] i = unitySamples.i();
		float[] q = unitySamples.q();

		float gain;

		//Apply random gain reduction to samples in range 0.5 to 1.0 gain
		for(int x = 0; x < i.length; x++)
		{
			gain = 0.5f + random.nextFloat() / 2.0f;
			i[x] *= gain;
			q[x] *= gain;
		}

		return new ComplexSamples(i, q);
	}

    public static Complex multiply(Complex a, Complex b)
    {
        return new Complex((a.inphase() * b.inphase()) - (a.quadrature() * b.quadrature()),
                ((a.quadrature() * b.inphase()) + (a.inphase() * b.quadrature())));
    }

    public static Complex minus(Complex a, Complex b)
    {
        return new Complex(a.inphase() - b.inphase(), a.quadrature() - b.quadrature());
    }

    public static double magnitude(Complex sample)
    {
        return FastMath.sqrt(magnitudeSquared(sample));
    }

    public static int magnitudeSquared(Complex sample)
    {
        return (int)((sample.inphase() * sample.inphase()) + (sample.quadrature() * sample.quadrature()));
    }
}
