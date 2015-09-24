/*******************************************************************************
 * Copyright (c) 2015 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.ide.edgefunc.type;

import heros.ide.edgefunc.Joinable;

/**
 * An abstraction for types that form a Lattice.
 * The bottom most type is expected to be the most abstract one, e.g., Object. 
 *
 */
public interface Type<T> extends Joinable<T> {

	/**
	 * Returns the most specific type matching both types. 
	 */
	@Override
	public T join(T j);
	
	/**
	 * Returns the most abstract sub-type of both types.
	 */
	public T meet(T m);
	
	public boolean isSubTypeOf(T other);
}
