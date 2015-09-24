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

public class TypeBoundary<T extends Type<T>> implements Joinable<TypeBoundary<T>> {

	private T lowerBound;
	private T upperBound;
	private boolean isEmpty;

	public TypeBoundary(T lowerBound, T upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		isEmpty = !lowerBound.join(upperBound).equals(upperBound);
	}

	public T getLowerBound() {
		return lowerBound;
	}

	public T getUpperBound() {
		return upperBound;
	}

	public boolean isEmpty() {
		return isEmpty;
	}
	
	public TypeBoundary<T> intersection(TypeBoundary<T> other) {
		return new TypeBoundary<T>(lowerBound.join(other.lowerBound), upperBound.meet(other.upperBound));
	}

	@Override
	public TypeBoundary<T> join(TypeBoundary<T> j) {
		return new TypeBoundary<T>(lowerBound.meet(j.lowerBound), upperBound.join(j.upperBound));
	}

	@Override
	public String toString() {
		return "<"+lowerBound+":"+upperBound+">";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lowerBound == null) ? 0 : lowerBound.hashCode());
		result = prime * result + ((upperBound == null) ? 0 : upperBound.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypeBoundary other = (TypeBoundary) obj;
		if (lowerBound == null) {
			if (other.lowerBound != null)
				return false;
		} else if (!lowerBound.equals(other.lowerBound))
			return false;
		if (upperBound == null) {
			if (other.upperBound != null)
				return false;
		} else if (!upperBound.equals(other.upperBound))
			return false;
		return true;
	}
}
