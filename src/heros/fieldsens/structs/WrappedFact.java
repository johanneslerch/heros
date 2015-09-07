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
package heros.fieldsens.structs;

import heros.fieldsens.AccessPath;
import heros.fieldsens.FlowFunction;
import heros.fieldsens.Resolver;
import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.FlowFunction.Constraint;

public class WrappedFact<Field, Fact, Stmt, Method>{

	private final Fact fact;
	private final AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver;
	
	public WrappedFact(Fact fact, AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
		assert fact != null;
		this.accPathResolver = accPathResolver;
		this.fact = fact;
	}
	
	public Fact getFact() {
		return fact;
	}

//	public WrappedFact<Field, Fact, Stmt, Method> applyConstraint(Constraint<Field> constraint, Fact zeroValue) {
//		if(fact.equals(zeroValue))
//			return this;
//		else
//			return new WrappedFact<Field, Fact, Stmt, Method>(fact, constraint.applyToAccessPath(accessPath), resolver);
//	}
	
	@Override
	public String toString() {
		return fact.toString()+accPathResolver.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accPathResolver == null) ? 0 : accPathResolver.hashCode());
		result = prime * result + ((fact == null) ? 0 : fact.hashCode());
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
		WrappedFact other = (WrappedFact) obj;
		if (accPathResolver == null) {
			if (other.accPathResolver != null)
				return false;
		} else if (!accPathResolver.equals(other.accPathResolver))
			return false;
		if (fact == null) {
			if (other.fact != null)
				return false;
		} else if (!fact.equals(other.fact))
			return false;
		return true;
	}

	public AccessPathAndResolver<Field, Fact, Stmt, Method> getAccessPathAndResolver() {
		return accPathResolver;
	}
}
