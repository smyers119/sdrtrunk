package io.github.dsheirer.vector.calibrate.filter;

import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.halfband.real.VectorRealHalfBandDecimationFilter23Tap128Bit;
import io.github.dsheirer.dsp.filter.halfband.real.VectorRealHalfBandDecimationFilter23Tap256Bit;
import io.github.dsheirer.dsp.filter.halfband.real.VectorRealHalfBandDecimationFilter23Tap512Bit;
import io.github.dsheirer.dsp.filter.halfband.real.VectorRealHalfBandDecimationFilter23Tap64Bit;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import jdk.incubator.vector.VectorSpecies;

/**
 * Calibration plugin for complex half-band filter
 */
public class RealHalfBand23TapFilterCalibration extends RealHalfBandBaseFilterCalibration
{
    private static final int COEFFICIENT_LENGTH = 23;

    /**
     * Constructs an instance
     */
    public RealHalfBand23TapFilterCalibration()
    {
        super(CalibrationType.FILTER_HALF_BAND_REAL_23_TAP, COEFFICIENT_LENGTH);
    }

    @Override protected IRealDecimationFilter getVectorFilter(VectorSpecies<Float> species, float[] coefficients)
    {
        switch(species.length())
        {
            case 16:
                return new VectorRealHalfBandDecimationFilter23Tap512Bit(coefficients);
            case 8:
                return new VectorRealHalfBandDecimationFilter23Tap256Bit(coefficients);
            case 4:
                return new VectorRealHalfBandDecimationFilter23Tap128Bit(coefficients);
            case 2:
                return new VectorRealHalfBandDecimationFilter23Tap64Bit(coefficients);
            default:
                return null;
        }
    }
}
