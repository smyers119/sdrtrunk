package io.github.dsheirer.vector.calibrate.filter;

import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.halfband.real.VectorRealHalfBandDecimationFilter63Tap128Bit;
import io.github.dsheirer.dsp.filter.halfband.real.VectorRealHalfBandDecimationFilter63Tap256Bit;
import io.github.dsheirer.dsp.filter.halfband.real.VectorRealHalfBandDecimationFilter63Tap512Bit;
import io.github.dsheirer.dsp.filter.halfband.real.VectorRealHalfBandDecimationFilter63Tap64Bit;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import jdk.incubator.vector.VectorSpecies;

/**
 * Calibration plugin for complex half-band filter
 */
public class RealHalfBand63TapFilterCalibration extends RealHalfBandBaseFilterCalibration
{
    private static final int COEFFICIENT_LENGTH = 63;

    /**
     * Constructs an instance
     */
    public RealHalfBand63TapFilterCalibration()
    {
        super(CalibrationType.FILTER_HALF_BAND_REAL_63_TAP, COEFFICIENT_LENGTH, 500_000);
    }

    @Override protected IRealDecimationFilter getVectorFilter(VectorSpecies<Float> species, float[] coefficients)
    {
        switch(species.length())
        {
            case 16:
                return new VectorRealHalfBandDecimationFilter63Tap512Bit(coefficients);
            case 8:
                return new VectorRealHalfBandDecimationFilter63Tap256Bit(coefficients);
            case 4:
                return new VectorRealHalfBandDecimationFilter63Tap128Bit(coefficients);
            case 2:
                return new VectorRealHalfBandDecimationFilter63Tap64Bit(coefficients);
            default:
                return null;
        }
    }
}
