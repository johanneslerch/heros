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
package heros.ide.edgefunc.uppertype;

import heros.ide.edgefunc.AbstractFactory;
import heros.ide.edgefunc.ChainableEdgeFunction;
import heros.ide.edgefunc.EdgeFunction;

public class AnyOrUpperBoundFunction<T extends Type<T>> extends ChainableEdgeFunction<T> {

	private T type;

	public AnyOrUpperBoundFunction(T type, AbstractFactory<T> factory, ChainableEdgeFunction<T> chainedFunction) {
		super(factory, chainedFunction);
		this.type = type;
	}

	@Override
	public EdgeFunction<T> chain(ChainableEdgeFunction<T> f) {
		if(f instanceof InitialSeedFunction || f instanceof EnsureEmptyFunction)
			return new AnyOrUpperBoundFunction<T>(type, factory, f);
		else
			throw new IllegalStateException();
	}

	@Override
	protected T _computeTarget(T source) {
		return type;
	}

	@Override
	protected boolean mayThisReturnTop() {
		return false;
	}

	@Override
	protected EdgeFunction<T> _composeWith(ChainableEdgeFunction<T> chainableFunction) {
		if(chainableFunction instanceof UpperBoundFunction) {
			T otherType = ((UpperBoundFunction<T>) chainableFunction).getType();
			T meet = otherType.meet(type);
			if(meet.equals(type))
				return this;
			else if(meet.equals(otherType))
				return new AnyOrUpperBoundFunction<T>(meet, factory, chainedFunction);
			else
				return factory.allTop();
		}
		if(chainableFunction instanceof EnsureEmptyFunction)
			return chainableFunction.chainIfNotNull(chainedFunction);
		if(chainableFunction instanceof PopFunction)
			return new AnyFunction<T>(factory, chainedFunction);
		if(chainableFunction instanceof SetTypeFunction) {
			T setType = ((SetTypeFunction<T>) chainableFunction).getType();
			if(setType.meet(type).equals(setType))
				return new AnyOrTypeFunction<T>(setType, factory, chainedFunction);
			else
				return factory.allTop();
		}
		return chainableFunction.chain(this);
	}

	@Override
	public String toString() {
		return "anyOrType("+type+")"+super.toString();
	}

	public T getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		AnyOrUpperBoundFunction other = (AnyOrUpperBoundFunction) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}  
	
	
}
