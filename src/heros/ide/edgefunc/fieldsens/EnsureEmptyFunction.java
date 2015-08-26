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
package heros.ide.edgefunc.fieldsens;

import heros.ide.edgefunc.EdgeFunction;

public class EnsureEmptyFunction<Field> extends ChainableEdgeFunction<Field> {

	public EnsureEmptyFunction(Factory<Field> factory) {
		super(factory, null);
	}
	
	public EnsureEmptyFunction(Factory<Field> factory, ChainableEdgeFunction<Field> chainedFunction) {
		super(factory, chainedFunction);
	}

	@Override
	public EdgeFunction<AccessPathBundle<Field>> chain(ChainableEdgeFunction<Field> f) {
		return new EnsureEmptyFunction<Field>(factory, f);
	}

	@Override
	protected AccessPathBundle<Field> _computeTarget(AccessPathBundle<Field> source) {
		return source;
	}

	@Override
	protected boolean mayThisReturnTop() {
		return true;
	}

	@Override
	protected EdgeFunction<AccessPathBundle<Field>> _composeWith(ChainableEdgeFunction<Field> chainableFunction) {
		if(chainableFunction instanceof ReadFunction)
			return factory.allTop();
		if(chainableFunction instanceof OverwriteFunction)
			return this;
		return chainableFunction.chain(this);
	}

}
