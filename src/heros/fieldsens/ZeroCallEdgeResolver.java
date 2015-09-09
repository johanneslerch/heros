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
package heros.fieldsens;

import com.google.common.base.Optional;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.FlowFunction.Constraint;
import heros.fieldsens.structs.AccessPathAndResolver;

public class ZeroCallEdgeResolver<Field, Fact, Stmt, Method> extends CallEdgeResolver<Field, Fact, Stmt, Method> {

	private ZeroHandler<Field> zeroHandler;

	public ZeroCallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, ZeroHandler<Field> zeroHandler, Debugger<Field, Fact, Stmt, Method> debugger) {
		super(analyzer, debugger);
		this.zeroHandler = zeroHandler;
	}

	@Override
	public void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback) {
		AccessPath<Field> accPath = constraint.applyToAccessPath(new AccessPath<Field>());
		Optional<AccessPath<Field>> gen = zeroHandler.shouldGenerateAccessPath(accPath);
		if(gen.isPresent())
			callback.interest(analyzer, new AccessPathAndResolver<Field, Fact, Stmt, Method>(accPath.getDeltaToAsAccessPath(gen.get()), this));
	}
	
	@Override
	public void interest(AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
	}
	
	@Override
	protected ZeroCallEdgeResolver<Field, Fact, Stmt, Method> getOrCreateNestedResolver(AccessPath<Field> newAccPath) {
		return this;
	}

	@Override
	public String toString() {
		return "[0-Resolver"+super.toString()+"]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((zeroHandler == null) ? 0 : zeroHandler.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ZeroCallEdgeResolver other = (ZeroCallEdgeResolver) obj;
		if (zeroHandler == null) {
			if (other.zeroHandler != null)
				return false;
		} else if (!zeroHandler.equals(other.zeroHandler))
			return false;
		return true;
	}
}
