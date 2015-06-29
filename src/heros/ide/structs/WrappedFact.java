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
package heros.ide.structs;

import heros.ide.Resolver;


public class WrappedFact<Fact, Stmt, Method, Value>{

	private final Fact fact;
	private final Resolver<Fact, Stmt, Method, Value> resolver;
	
	public WrappedFact(Fact fact, Resolver<Fact, Stmt, Method, Value> resolver) {
		assert fact != null;
		assert resolver != null;
		
		this.fact = fact;
		this.resolver = resolver;
	}
	
	public Fact getFact() {
		return fact;
	}
	
	@Override
	public String toString() {
		String result = fact.toString();
		if(resolver != null)
			result+=resolver.toString();
		return result;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fact == null) ? 0 : fact.hashCode());
		result = prime * result + ((resolver == null) ? 0 : resolver.hashCode());
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
		if (fact == null) {
			if (other.fact != null)
				return false;
		} else if (!fact.equals(other.fact))
			return false;
		if (resolver == null) {
			if (other.resolver != null)
				return false;
		} else if (!resolver.equals(other.resolver))
			return false;
		return true;
	}

	public Resolver<Fact, Stmt, Method, Value> getResolver() {
		return resolver;
	}
	
	
}
