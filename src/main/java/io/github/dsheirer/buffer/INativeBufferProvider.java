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
package io.github.dsheirer.buffer;

import io.github.dsheirer.sample.Listener;

/**
 * Interface for a provider of interleaved complex samples
 */
public interface INativeBufferProvider
{
    /**
     * Adds the listener to receive buffers
     */
    void addBufferListener(Listener<INativeBuffer> listener);

    /**
     * Removes the listener from receiving buffers
     */
    void removeBufferListener(Listener<INativeBuffer> listener);

    /**
     * Indicates if there are any buffer listeners registered
     */
    boolean hasBufferListeners();
}
