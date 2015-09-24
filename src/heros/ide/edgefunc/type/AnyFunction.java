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

public class AnyFunction<T extends Type<T>> extends ChainableEdgeFunction<TypeBoundary<T>> {

	public AnyFunction(AbstractFactory<TypeBoundary<T>> factory, ChainableEdgeFunction<TypeBoundary<T>> chainedFunction) {
		super(factory, chainedFunction);
	}

	@Override
	public EdgeFunction<TypeBoundary<T>> chain(ChainableEdgeFunction<TypeBoundary<T>> f) {
		if(f instanceof InitialSeedFunction || f instanceof EnsureEmptyFunction)
			return new AnyFunction<T>(factory, f);
		else
			throw new IllegalStateException();
	}

	@Override
	protected TypeBoundary<T> _computeTarget(TypeBoundary<T> source) {
		return source;
	}

	@Override
	protected boolean mayThisReturnTop() {
		return false;
	}

	@Override
	protected EdgeFunction<TypeBoundary<T>> _composeWith(ChainableEdgeFunction<TypeBoundary<T>> chainableFunction) {
		if(chainableFunction instanceof EnsureEmptyFunction)
			return chainableFunction.chainIfNotNull(chainedFunction);
		if(chainableFunction instanceof PopFunction)
			return this;
		if(chainableFunction instanceof BoundFunction) {
			return new AnyOrBoundFunction<T>(((BoundFunction<T>)chainableFunction).getTypeBoundary(), factory, chainedFunction);
		}			
		
		return chainableFunction.chain(this);
	}

	@Override
	public String toString() {
		return "any"+super.toString();
	}
}
