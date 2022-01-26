package io.github.dsheirer.vector.calibrate;

public enum CalibrationType
{
    COMPLEX_GAIN("Complex Gain"),
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
