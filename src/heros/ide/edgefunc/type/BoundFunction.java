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

import heros.ide.edgefunc.AbstractFactory;
import heros.ide.edgefunc.ChainableEdgeFunction;
import heros.ide.edgefunc.EdgeFunction;

public class BoundFunction<T extends Type<T>> extends ChainableEdgeFunction<TypeBoundary<T>> {

	private TypeBoundary<T> typeBoundary;

	public BoundFunction(TypeBoundary<T> type, AbstractFactory<TypeBoundary<T>> factory, ChainableEdgeFunction<TypeBoundary<T>> chainedFunction) {
		super(factory, chainedFunction);
		this.typeBoundary = type;
	}

	@Override
	public EdgeFunction<TypeBoundary<T>> chain(ChainableEdgeFunction<TypeBoundary<T>> f) {
		return new BoundFunction<T>(typeBoundary, factory, f);
	}

	@Override
	protected TypeBoundary<T> _computeTarget(TypeBoundary<T> source) {
		return source.intersection(typeBoundary);
	}

	@Override
	protected boolean mayThisReturnTop() {
		if(chainedFunction == null)
			return true;
		if(chainedFunction instanceof InitialSeedFunction)
			return false;
		if(chainedFunction instanceof EnsureEmptyFunction)
			return false;
		if(chainedFunction instanceof PushFunction)
			return false;
		return true;
	}

	@Override
	protected EdgeFunction<TypeBoundary<T>> _composeWith(ChainableEdgeFunction<TypeBoundary<T>> chainableFunction) {
		if(chainableFunction instanceof BoundFunction) {
			TypeBoundary<T> otherTypeBoundary = ((BoundFunction<T>) chainableFunction).typeBoundary;
			if(otherTypeBoundary.equals(typeBoundary))
				return this;
			TypeBoundary<T> intersection = otherTypeBoundary.intersection(typeBoundary);
			if(intersection.isEmpty())
				return factory.allTop();
			else
				return new BoundFunction<T>(intersection, factory, chainedFunction);
		}
		if(chainableFunction instanceof PopFunction && chainedFunction instanceof PushFunction)
			return chainedFunction.chainedFunction();
		if(chainableFunction instanceof EnsureEmptyFunction) {
			if(chainedFunction instanceof InitialSeedFunction)
				return chainedFunction;
			if(chainedFunction instanceof PopFunction)
				return chainableFunction.chain(chainedFunction);
			if(chainedFunction instanceof EnsureEmptyFunction)
				return chainedFunction;
		}
		
		return chainableFunction.chain(this);
	}

	@Override
	public String toString() {
		return "bound("+typeBoundary+")"+super.toString();
	}

	public TypeBoundary<T> getTypeBoundary() {
		return typeBoundary;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((typeBoundary == null) ? 0 : typeBoundary.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		BoundFunction other = (BoundFunction) obj;
		if (typeBoundary == null) {
			if (other.typeBoundary != null)
				return false;
		} else if (!typeBoundary.equals(other.typeBoundary))
			return false;
		return true;
	}
}
