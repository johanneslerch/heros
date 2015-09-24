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

public class AnyOrBoundFunction<T extends Type<T>> extends ChainableEdgeFunction<TypeBoundary<T>> {

	private TypeBoundary<T> typeBoundary;

	public AnyOrBoundFunction(TypeBoundary<T> type, AbstractFactory<TypeBoundary<T>> factory, ChainableEdgeFunction<TypeBoundary<T>> chainedFunction) {
		super(factory, chainedFunction);
		this.typeBoundary = type;
	}

	@Override
	public EdgeFunction<TypeBoundary<T>> chain(ChainableEdgeFunction<TypeBoundary<T>> f) {
		if(f instanceof InitialSeedFunction || f instanceof EnsureEmptyFunction)
			return new AnyOrBoundFunction<T>(typeBoundary, factory, f);
		else
			throw new IllegalStateException();
	}

	@Override
	protected TypeBoundary<T> _computeTarget(TypeBoundary<T> source) {
		return typeBoundary;
	}

	@Override
	protected boolean mayThisReturnTop() {
		return false;
	}

	@Override
	protected EdgeFunction<TypeBoundary<T>> _composeWith(ChainableEdgeFunction<TypeBoundary<T>> chainableFunction) {
		if(chainableFunction instanceof BoundFunction) {
			TypeBoundary<T> otherType = ((BoundFunction<T>) chainableFunction).getTypeBoundary();
			if(typeBoundary.equals(otherType))
				return this;
			TypeBoundary<T> intersection = otherType.intersection(typeBoundary);
			if(intersection.isEmpty())
				return factory.allTop();
			else
				return new AnyOrBoundFunction<T>(intersection, factory, chainedFunction);
		}
		if(chainableFunction instanceof EnsureEmptyFunction)
			return chainableFunction.chainIfNotNull(chainedFunction);
		if(chainableFunction instanceof PopFunction)
			return new AnyFunction<T>(factory, chainedFunction);
		return chainableFunction.chain(this);
	}

	@Override
	public String toString() {
		return "anyOrType("+typeBoundary+")"+super.toString();
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
		AnyOrBoundFunction other = (AnyOrBoundFunction) obj;
		if (typeBoundary == null) {
			if (other.typeBoundary != null)
				return false;
		} else if (!typeBoundary.equals(other.typeBoundary))
			return false;
		return true;
	}  
	
	
}
