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

public class PushFunction<T extends Type<T>> extends ChainableEdgeFunction<T> {

	public PushFunction(AbstractFactory<T> factory, ChainableEdgeFunction<T> chainedFunction) {
		super(factory, chainedFunction);
	}

	@Override
	public EdgeFunction<T> chain(ChainableEdgeFunction<T> f) {
		return new PushFunction<T>(factory, f);
	}

	@Override
	protected T _computeTarget(T source) {
		return factory.getLattice().bottomElement();
	}

	@Override
	protected boolean mayThisReturnTop() {
		return false;
	}

	@Override
	protected EdgeFunction<T> _composeWith(ChainableEdgeFunction<T> chainableFunction) {
		if(chainableFunction instanceof PopFunction)
			return chainedFunction();
		if(chainableFunction instanceof EnsureEmptyFunction)
			return factory.allTop();
		
		return chainableFunction.chain(this);
	}

	@Override
	public String toString() {
		return "push"+super.toString();
	}
}
