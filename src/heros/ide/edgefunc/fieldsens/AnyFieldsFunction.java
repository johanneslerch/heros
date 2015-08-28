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

import heros.ide.edgefunc.AbstractFactory;
import heros.ide.edgefunc.ChainableEdgeFunction;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.edgefunc.Joinable;

public class AnyFieldsFunction<Field> extends ChainableEdgeFunction<AccessPathBundle<Field>> {

	public AnyFieldsFunction(AbstractFactory<AccessPathBundle<Field>> factory) {
		super(factory, null);
	}

	public AnyFieldsFunction(AbstractFactory<AccessPathBundle<Field>> factory, ChainableEdgeFunction<AccessPathBundle<Field>> f) {
		super(factory, f);
	}

	@Override
	public EdgeFunction<AccessPathBundle<Field>> chain(ChainableEdgeFunction<AccessPathBundle<Field>> f) {
		if(!(f instanceof InitialSeedFunction))
			throw new IllegalStateException();
		return new AnyFieldsFunction<Field>(factory, f);
	}
	
	@Override
	protected AccessPathBundle<Field> _computeTarget(AccessPathBundle<Field> source) {
		return source;
	}
	
	@Override
	protected EdgeFunction<AccessPathBundle<Field>> _composeWith(ChainableEdgeFunction<AccessPathBundle<Field>> chainableFunction) {
		if(chainableFunction instanceof ReadFunction)
			return this;
		if(chainableFunction instanceof EnsureEmptyFunction)
			return chainedFunction();
		return chainableFunction.chain(this);
	}
	
	@Override
	protected boolean mayThisReturnTop() {
		return false;
	}
	
	@Override
	public String toString() {
		return "anyFields";
	}
}
