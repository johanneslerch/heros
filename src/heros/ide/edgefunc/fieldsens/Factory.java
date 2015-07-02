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

import heros.JoinLattice;
import heros.ide.edgefunc.AllBottom;
import heros.ide.edgefunc.AllTop;
import heros.ide.edgefunc.EdgeIdentity;

public class Factory<Field> {

	private AllTop<AccessPathBundle<Field>> allTop;
	private AllBottom<AccessPathBundle<Field>> allBottom;

	public Factory(JoinLattice<AccessPathBundle<Field>> lattice) {
		this.allTop = new AllTop<AccessPathBundle<Field>>(lattice.topElement());
		this.allBottom = new AllBottom<AccessPathBundle<Field>>(lattice.bottomElement());
	}
	
	public AllTop<AccessPathBundle<Field>> allTop() {
		return allTop;
	}
	
	public AllBottom<AccessPathBundle<Field>> allBottom() {
		return allBottom;
	}
	
	public EdgeIdentity<AccessPathBundle<Field>> id() {
		return EdgeIdentity.<AccessPathBundle<Field>> v();
	}
	
	public PrependFunction<Field> prepend(Field field) {
		return new PrependFunction<Field>(this, field);
	}
	
	public ReadFunction<Field> read(Field field) {
		return new ReadFunction<Field>(this, field);
	}
	
	public OverwriteFunction<Field> overwrite(Field field) {
		return new OverwriteFunction<Field>(this, field);
	}
}
