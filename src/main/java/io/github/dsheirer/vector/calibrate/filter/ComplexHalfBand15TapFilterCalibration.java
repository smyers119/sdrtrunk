package io.github.dsheirer.vector.calibrate.filter;

import io.github.dsheirer.dsp.filter.decimate.IComplexDecimationFilter;
import io.github.dsheirer.dsp.filter.halfband.complex.VectorComplexHalfBandDecimationFilter15Tap128Bit;
import io.github.dsheirer.dsp.filter.halfband.complex.VectorComplexHalfBandDecimationFilter15Tap256Bit;
import io.github.dsheirer.dsp.filter.halfband.complex.VectorComplexHalfBandDecimationFilter15Tap512Bit;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import jdk.incubator.vector.VectorSpecies;

/**
 * Calibration plugin for complex half-band filter
 */
public class ComplexHalfBand15TapFilterCalibration extends ComplexHalfBandBaseFilterCalibration
{
    private static final int COEFFICIENT_LENGTH = 15;

    /**
     * Constructs an instance
     */
    public ComplexHalfBand15TapFilterCalibration()
    {
        super(CalibrationType.FILTER_HALF_BAND_COMPLEX_15_TAP, COEFFICIENT_LENGTH);
    }

    @Override protected IComplexDecimationFilter getVectorFilter(VectorSpecies<Float> species, float[] coefficients)
    {
        switch(species.length())
        {
            case 16:
                return new VectorComplexHalfBandDecimationFilter15Tap512Bit(coefficients);
            case 8:
                return new VectorComplexHalfBandDecimationFilter15Tap256Bit(coefficients);
            case 4:
                return new VectorComplexHalfBandDecimationFilter15Tap128Bit(coefficients);
            case 2:
            default:
                return null;
        }
    }
}
