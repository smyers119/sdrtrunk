package io.github.dsheirer.sample.complex;

import org.apache.commons.math3.util.FastMath;

public class ComplexSampleUtils
{
	/**
	 * Converts an interleaved complex sample array and deinterleaves the I and Q sample
	 * arrays.
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
			i[x] = samples[offset + 1];
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
