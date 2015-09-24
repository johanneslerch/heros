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

public class EnsureEmptyFunction<T extends Type<T>> extends ChainableEdgeFunction<TypeBoundary<T>> {

	public EnsureEmptyFunction(AbstractFactory<TypeBoundary<T>> factory, ChainableEdgeFunction<TypeBoundary<T>> chainedFunction) {
		super(factory, chainedFunction);
	}

	@Override
	public EdgeFunction<TypeBoundary<T>> chain(ChainableEdgeFunction<TypeBoundary<T>> f) {
		return new EnsureEmptyFunction<T>(factory, f);
	}

	@Override
	protected TypeBoundary<T> _computeTarget(TypeBoundary<T> source) {
		return source;
	}

	@Override
	protected boolean mayThisReturnTop() {
		return true;
	}

	@Override
	protected EdgeFunction<TypeBoundary<T>> _composeWith(ChainableEdgeFunction<TypeBoundary<T>> chainableFunction) {
		if(chainableFunction instanceof PopFunction)
			return factory.allTop();
		if(chainableFunction instanceof EnsureEmptyFunction)
			return this;
		if(chainableFunction instanceof BoundFunction) {
			
		}
		
		return chainableFunction.chain(this);
	}

	@Override
	public String toString() {
		return "empty"+super.toString();
	}
}
