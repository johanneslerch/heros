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
import heros.ide.edgefunc.EdgeFunction;

public class FactEdgeFnResolverTuple<Fact, Stmt, Method, Value> {

	private final Fact fact;
	private final EdgeFunction<Value> edgeFunction;
	private final Resolver<Fact, Stmt, Method, Value> resolver;

	public FactEdgeFnResolverTuple(Fact fact, EdgeFunction<Value> edgeFunction, Resolver<Fact, Stmt, Method, Value> resolver) {
		this.fact = fact;
		this.edgeFunction = edgeFunction;
		this.resolver = resolver;
	}
	
	public Fact getFact() {
		return fact;
	}
	
	public EdgeFunction<Value> getEdgeFunction() {
		return edgeFunction;
	}
	
	public Resolver<Fact, Stmt, Method, Value> getResolver() {
		return resolver;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((edgeFunction == null) ? 0 : edgeFunction.hashCode());
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
		FactEdgeFnResolverTuple other = (FactEdgeFnResolverTuple) obj;
		if (edgeFunction == null) {
			if (other.edgeFunction != null)
				return false;
		} else if (!edgeFunction.equals(other.edgeFunction))
			return false;
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
}
