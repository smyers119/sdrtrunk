/*
 * *****************************************************************************
 * Copyright (C) 2014-2021 Dennis Sheirer
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

package io.github.dsheirer.dsp.filter.decimate;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.halfband.complex.ComplexHalfBandDecimationFilter;
import io.github.dsheirer.dsp.window.WindowType;

/**
 * Decimate by 256 filter for complex valued sample buffers.
 */
public class ComplexDecimateX256Filter extends ComplexDecimateX128Filter
{
    private static final int VALIDATION_LENGTH = 256 * 2;
    private static final int DECIMATE_BY_256_FILTER_LENGTH = 11;
    private static final WindowType DECIMATE_BY_256_WINDOW_TYPE = WindowType.BLACKMAN;
    private ComplexHalfBandDecimationFilter mFilter;

    /**
     * Constructs the decimation filter.
     */
    public ComplexDecimateX256Filter()
    {
        mFilter = new ComplexHalfBandDecimationFilter(FilterFactory.getHalfBand(DECIMATE_BY_256_FILTER_LENGTH,
                DECIMATE_BY_256_WINDOW_TYPE));
    }

    @Override
    public float[] decimateComplex(float[] samples)
    {
        validate(samples, VALIDATION_LENGTH);

        //Decimate by this filter, then by the parent decimation filter
        return super.decimateComplex(mFilter.decimateComplex(samples));
    }
}
