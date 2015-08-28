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

import heros.JoinLattice;
import heros.ide.edgefunc.AbstractFactory;
import heros.ide.edgefunc.AllBottom;
import heros.ide.edgefunc.AllTop;
import heros.ide.edgefunc.EdgeFunction;

public class Factory<T extends Type<T>> extends AbstractFactory<T> {

	private JoinLattice<T> lattice;
	private AllTop<T> allTop;
	private AllBottom<T> allBottom;

	public Factory(JoinLattice<T> lattice) {
		this.lattice = lattice;
		this.allTop = new AllTop<T>(lattice.topElement());
		this.allBottom = new AllBottom<T>(lattice.bottomElement());
	}
	
	@Override
	public EdgeFunction<T> allTop() {
		return allTop;
	}

	@Override
	public JoinLattice<T> getLattice() {
		return lattice;
	}

	public EdgeFunction<T> allBottom() {
		return allBottom;
	}
	
	public EdgeFunction<T> any() {
		return new AnyFunction<>(this, null);
	}
	
	public EdgeFunction<T> anyOrType(T type) {
		return new AnyOrTypeFunction<T>(type, this, null);
	}
	
	public EdgeFunction<T> init() {
		return new InitialSeedFunction<T>(this, null);
	}
	
	public EdgeFunction<T> pop() {
		return new PopFunction<T>(this, null);
	}
	
	public EdgeFunction<T> push() {
		return new PushFunction<T>(this, null);
	}
	
	public EdgeFunction<T> type(T type) {
		return new SetTypeFunction<T>(type, this, null);
	}

	public EdgeFunction<T> empty() {
		return new EnsureEmptyFunction<T>(this, null);
	}

	public EdgeFunction<T> upperBound(T type) {
		return new UpperBoundFunction<T>(type, this, null);
	}

	public EdgeFunction<T> anyOrUpperBound(T type) {
		return new AnyOrUpperBoundFunction<T>(type, this, null);
	}
}
