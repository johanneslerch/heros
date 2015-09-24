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

import heros.JoinLattice;
import heros.ide.edgefunc.AbstractFactory;
import heros.ide.edgefunc.AllBottom;
import heros.ide.edgefunc.AllTop;
import heros.ide.edgefunc.EdgeFunction;

public class Factory<T extends Type<T>> extends AbstractFactory<TypeBoundary<T>> {

	private JoinLattice<TypeBoundary<T>> lattice;
	private AllTop<TypeBoundary<T>> allTop;
	private AllBottom<TypeBoundary<T>> allBottom;
	private T bottomElement;
	private T topElement;

	public Factory(JoinLattice<TypeBoundary<T>> lattice, T bottomElement, T topElement) {
		this.lattice = lattice;
		this.bottomElement = bottomElement;
		this.topElement = topElement;
		this.allTop = new AllTop<TypeBoundary<T>>(lattice.topElement());
		this.allBottom = new AllBottom<TypeBoundary<T>>(lattice.bottomElement());
	}
	
	@Override
	public EdgeFunction<TypeBoundary<T>> allTop() {
		return allTop;
	}

	@Override
	public JoinLattice<TypeBoundary<T>> getLattice() {
		return lattice;
	}

	public EdgeFunction<TypeBoundary<T>> allBottom() {
		return allBottom;
	}
	
	public EdgeFunction<TypeBoundary<T>> any() {
		return new AnyFunction<T>(this, null);
	}
	
	public EdgeFunction<TypeBoundary<T>> init() {
		return new InitialSeedFunction<T>(this, null);
	}
	
	public EdgeFunction<TypeBoundary<T>> pop() {
		return new PopFunction<T>(this, null);
	}
	
	public EdgeFunction<TypeBoundary<T>> push() {
		return new PushFunction<T>(this, null);
	}
	
	public EdgeFunction<TypeBoundary<T>> empty() {
		return new EnsureEmptyFunction<T>(this, null);
	}

	public EdgeFunction<TypeBoundary<T>> bound(TypeBoundary<T> type) {
		return new BoundFunction<T>(type, this, null);
	}

	public EdgeFunction<TypeBoundary<T>> bound(T lowerBound, T upperBound) {
		return new BoundFunction<T>(new TypeBoundary<T>(lowerBound, upperBound), this, null);
	}
	
	public EdgeFunction<TypeBoundary<T>> lowerBound(T lowerBound) {
		return new BoundFunction<T>(new TypeBoundary<T>(lowerBound, topElement), this, null);
	}
	
	public EdgeFunction<TypeBoundary<T>> upperBound(T upperBound) {
		return new BoundFunction<T>(new TypeBoundary<T>(bottomElement, upperBound), this, null);
	}

	public EdgeFunction<TypeBoundary<T>> anyOrUpperBound(TypeBoundary<T> type) {
		return new AnyOrBoundFunction<T>(type, this, null);
	}
}
