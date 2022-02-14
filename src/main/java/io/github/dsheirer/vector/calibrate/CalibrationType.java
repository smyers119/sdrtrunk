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

package io.github.dsheirer.vector.calibrate;

public enum CalibrationType
{
    AIRSPY_SAMPLE_CONVERTER("Airspy Sample Converter"),
    COMPLEX_GAIN_CONTROL("Complex Gain Control"),
    COMPLEX_GAIN("Complex Gain"),
    COMPLEX_MIXER("Complex Mixer"),
    DC_REMOVAL_REAL("Real DC Removal Filter"),
    FILTER_FIR("FIR Filter"),
    FILTER_HALF_BAND_COMPLEX_11_TAP("Complex Half-Band Decimation Filter - 11 Tap"),
    FILTER_HALF_BAND_COMPLEX_15_TAP("Complex Half-Band Decimation Filter - 15 Tap"),
    FILTER_HALF_BAND_COMPLEX_23_TAP("Complex Half-Band Decimation Filter - 23 Tap"),
    FILTER_HALF_BAND_COMPLEX_63_TAP("Complex Half-Band Decimation Filter - 63 Tap"),
    FILTER_HALF_BAND_REAL_11_TAP("Real Half-Band Decimation Filter - 11 Tap"),
    FILTER_HALF_BAND_REAL_15_TAP("Real Half-Band Decimation Filter - 15 Tap"),
    FILTER_HALF_BAND_REAL_23_TAP("Real Half-Band Decimation Filter - 23 Tap"),
    FILTER_HALF_BAND_REAL_63_TAP("Real Half-Band Decimation Filter - 63 Tap"),
    FILTER_HALF_BAND_REAL_DEFAULT("Real Half-Band Decimation Filter - Default"),
    FM_DEMODULATOR("FM Demodulator"),
    HILBERT_TRANSFORM("Hilbert Transform"),
    OSCILLATOR_COMPLEX("Complex Oscillator"),
    OSCILLATOR_REAL("Real Oscillator"),
    SQUELCHING_FM_DEMODULATOR("Squelching FM Demodulator");

    private String mDescription;

    CalibrationType(String description)
    {
        mDescription = description;
    }

    @Override public String toString()
    {
        return mDescription;
    }
}
