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

import heros.ide.edgefunc.ChainableEdgeFunction;
import heros.ide.edgefunc.EdgeFunction;

public class InitialSeedFunction<Field> extends ChainableEdgeFunction<AccessPathBundle<Field>> {

	public InitialSeedFunction(Factory<Field> factory) {
		super(factory, null);
	}

	@Override
	public EdgeFunction<AccessPathBundle<Field>> chain(ChainableEdgeFunction<AccessPathBundle<Field>> f) {
		throw new IllegalStateException();
	}

	@Override
	protected AccessPathBundle<Field> _computeTarget(AccessPathBundle<Field> source) {
		return factory.getLattice().bottomElement();
	}

	@Override
	protected boolean mayThisReturnTop() {
		return false;
	}

	@Override
	protected EdgeFunction<AccessPathBundle<Field>> _composeWith(ChainableEdgeFunction<AccessPathBundle<Field>> chainableFunction) {
		if(chainableFunction instanceof EnsureEmptyFunction)
			return this;
		if(chainableFunction instanceof OverwriteFunction)
			return this;
		if(chainableFunction instanceof PrependFunction)
			return chainableFunction.chain(this);
		if(chainableFunction instanceof ReadFunction)
			return factory.allTop();
		if(chainableFunction instanceof AnyFieldsFunction)
			return chainableFunction.chain(this);
		throw new IllegalStateException();
	}
	
	@Override
	public String toString() {
		return "init" + super.toString();
	}
}
