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

package io.github.dsheirer.dsp.filter.vector;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for working with Project Panama SIMD vectors in JDK 17+.
 */
public class VectorUtilities
{
    private static final Logger mLog = LoggerFactory.getLogger(VectorUtilities.class);

    /**
     * Checks the species to determine if it is compatible with the preferred species for the runtime CPU
     * and logs a warning if the species' lane width is wider than the preferred species ... which would be
     * hugely inefficient.
     *
     * @param species to test
     */
    public static void checkSpecies(VectorSpecies<Float> species)
    {
        if(FloatVector.SPECIES_PREFERRED.length() < species.length())
        {
            mLog.warn("CPU does not support SIMD instructions of at least " + species +
                    ".  This filter WILL perform poorly -- consider using a filter implementation that is matched to " +
                    " this CPU: " + FloatVector.SPECIES_PREFERRED);
        }
    }

    /**
     * Creates a vector mask for deinterleaving I samples from an interleaved complex sample vector.
     * @param species of SIMD
     * @return vector mask
     */
    public static VectorMask<Float> getIVectorMask(VectorSpecies<Float> species)
    {
        switch(species.length())
        {
            case 2:
                return VectorMask.fromArray(species, new boolean[]{true,false}, 0);
            case 4:
                return VectorMask.fromArray(species, new boolean[]{true,false,true,false}, 0);
            case 8:
                return VectorMask.fromArray(species, new boolean[]{true,false,true,false,true,false,true,false}, 0);
            case 16:
                return VectorMask.fromArray(species, new boolean[]{true,false,true,false,true,false,true,false,true,
                        false,true,false,true,false,true,false}, 0);
            default:
                throw new IllegalArgumentException("Unrecognized vector species: " + species);
        }
    }

    /**
     * Creates a vector mask for deinterleaving Q samples from an interleaved complex sample vector.
     * @param species of SIMD
     * @return vector mask
     */
    public static VectorMask<Float> getQVectorMask(VectorSpecies<Float> species)
    {
        switch(species.length())
        {
            case 2:
                return VectorMask.fromArray(species, new boolean[]{false,true}, 0);
            case 4:
                return VectorMask.fromArray(species, new boolean[]{false,true,false,true}, 0);
            case 8:
                return VectorMask.fromArray(species, new boolean[]{false,true,false,true,false,true,false,true}, 0);
            case 16:
                return VectorMask.fromArray(species, new boolean[]{false,true,false,true,false,true,false,true,false,true,
                        false,true,false,true,false,true}, 0);
            default:
                throw new IllegalArgumentException("Unrecognized vector species: " + species);
        }
    }
}
