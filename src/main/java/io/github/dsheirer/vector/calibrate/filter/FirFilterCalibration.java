package io.github.dsheirer.vector.calibrate.filter;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilter128Bit;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilter256Bit;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilter512Bit;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilter64Bit;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilterDefaultBit;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calibration plugin for FIR filters
 */
public class FirFilterCalibration extends Calibration
{
    private static final Logger mLog = LoggerFactory.getLogger(FirFilterCalibration.class);
    private static final int ITERATIONS = 250_000;
    private static final int SAMPLE_BUFFER_SIZE = 2048;

    /**
     * Constructs an instance
     */
    public FirFilterCalibration()
    {
        super(CalibrationType.FILTER_FIR);
    }

    /**
     * Performs calibration to determine optimal (Scalar vs Vector) operation type.
     * @throws CalibrationException
     */
    @Override public void calibrate() throws CalibrationException
    {
        float[] samples = getSamples(SAMPLE_BUFFER_SIZE);

        float[] coefficients;

        try
        {
            coefficients = FilterFactory.getSinc(0.25, 31, Window.WindowType.BLACKMAN);
        }
        catch(FilterDesignException fde)
        {
            throw new CalibrationException("Error creating FIR filter coefficients", fde);
        }

        long bestScore = calculateScalar(coefficients, samples, ITERATIONS);
        mLog.info("FIR SCALAR:" + bestScore);
        Implementation operation = Implementation.SCALAR;

        long vectorPreferred = calculateVector(FloatVector.SPECIES_PREFERRED, coefficients, samples, ITERATIONS);
        mLog.info("FIR VECTOR PREFERRED:" + vectorPreferred);

        if(vectorPreferred < bestScore)
        {
            bestScore = vectorPreferred;
            operation = Implementation.VECTOR_SIMD_PREFERRED;
        }

        switch(FloatVector.SPECIES_PREFERRED.length())
        {
            //Fall through for each switch case is the intended behavior
            case 16:
                long vector512 = calculateVector(FloatVector.SPECIES_512, coefficients, samples, ITERATIONS);
                mLog.info("FIR VECTOR 512:" + vector512);
                if(vector512 < bestScore)
                {
                    bestScore = vector512;
                    operation = Implementation.VECTOR_SIMD_512;
                }
            case 8:
                long vector256 = calculateVector(FloatVector.SPECIES_256, coefficients, samples, ITERATIONS);
                mLog.info("FIR VECTOR 256:" + vector256);
                if(vector256 < bestScore)
                {
                    bestScore = vector256;
                    operation = Implementation.VECTOR_SIMD_256;
                }
            case 4:
                long vector128 = calculateVector(FloatVector.SPECIES_128, coefficients, samples, ITERATIONS);
                mLog.info("FIR VECTOR 128:" + vector128);
                if(vector128 < bestScore)
                {
                    bestScore = vector128;
                    operation = Implementation.VECTOR_SIMD_128;
                }
            case 2:
                long vector64 = calculateVector(FloatVector.SPECIES_128, coefficients, samples, ITERATIONS);
                mLog.info("FIR VECTOR 64:" + vector64);
                if(vector64 < bestScore)
                {
                    operation = Implementation.VECTOR_SIMD_64;
                }
        }

        mLog.info("FIR - SETTING OPTIMAL OPERATION TO: " + operation);
        setImplementation(operation);
    }

    /**
     * Calculates the time duration to process the sample buffer with the filter coefficients.
     * @param coefficients of the filter
     * @param samples buffer
     * @param iterations count of how many times to (re)filter the samples buffer
     * @return time duration in milliseconds
     */
    private static long calculateScalar(float[] coefficients, float[] samples, int iterations)
    {
        float accumulator = 0.0f;

        RealFIRFilter scalar = new RealFIRFilter(coefficients);

        long start = System.currentTimeMillis();

        for(int i = 0; i < iterations; i++)
        {
            float[] filtered = scalar.filter(samples);
            accumulator += filtered[0];
        }

        return System.currentTimeMillis() - start;
    }

    /**
     * Calculates the time duration to process the sample buffer with the filter coefficients.
     * @param species of vector for the SIMD instruction lane width
     * @param coefficients of the filter
     * @param samples buffer
     * @param iterations count of how many times to (re)filter the samples buffer
     * @return time duration in milliseconds
     */
    private static long calculateVector(VectorSpecies<Float> species, float[] coefficients, float[] samples, int iterations)
    {
        float accumulator = 0.0f;

        IRealFilter vector = getFilter(species, coefficients);

        long start = System.currentTimeMillis();

        for(int i = 0; i < iterations; i++)
        {
            float[] filtered = vector.filter(samples);
            accumulator += filtered[0];
        }

        return System.currentTimeMillis() - start;
    }

    private static IRealFilter getFilter(VectorSpecies<Float> species, float[] coefficients)
    {
        if(species.equals(FloatVector.SPECIES_PREFERRED))
        {
            return new VectorRealFIRFilterDefaultBit(coefficients);
        }

        switch(species.length())
        {
            case 16:
                return new VectorRealFIRFilter512Bit(coefficients);
            case 8:
                return new VectorRealFIRFilter256Bit(coefficients);
            case 4:
                return new VectorRealFIRFilter128Bit(coefficients);
            case 2:
                return new VectorRealFIRFilter64Bit(coefficients);
        }

        throw new IllegalArgumentException("Unrecognized vector species:" + species);
    }

}
