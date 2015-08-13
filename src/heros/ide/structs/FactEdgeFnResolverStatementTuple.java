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
import heros.ide.edgefunc.EdgeIdentity;

public class FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> {

	private final Fact fact;
	private final EdgeFunction<Value> edgeFunction;
	private final Resolver<Fact, Stmt, Method, Value> resolver;
	private final Stmt statement;

	public FactEdgeFnResolverStatementTuple(Fact fact, EdgeFunction<Value> edgeFunction, Resolver<Fact, Stmt, Method, Value> resolver, Stmt statement) {
		this.fact = fact;
		this.edgeFunction = edgeFunction;
		this.resolver = resolver;
		this.statement = statement;
	}

	public FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> copyWithResolver(Resolver<Fact, Stmt, Method, Value> resolver) {
		return new FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value>(fact, edgeFunction, resolver, statement);
	}
	
	@Override
	public String toString() {
		return "["+fact+";"+edgeFunction+";"+resolver+";"+statement+"]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((edgeFunction == null) ? 0 : edgeFunction.hashCode());
		result = prime * result + ((fact == null) ? 0 : fact.hashCode());
		result = prime * result + ((resolver == null) ? 0 : resolver.hashCode());
		result = prime * result + ((statement == null) ? 0 : statement.hashCode());
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
		FactEdgeFnResolverStatementTuple other = (FactEdgeFnResolverStatementTuple) obj;
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
		if (statement == null) {
			if (other.statement != null)
				return false;
		} else if (!statement.equals(other.statement))
			return false;
		return true;
	}

	public Fact getFact() {
		return fact;
	}

	public EdgeFunction<Value> getEdgeFunction() {
		return edgeFunction;
	}

	public FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> copyWithEdgeFunction(EdgeFunction<Value> edgeFunction) {
		return new FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value>(fact, edgeFunction, resolver, statement);
	}

	public Stmt getStatement() {
		return statement;
	}

	public Resolver<Fact, Stmt, Method, Value> getResolver() {
		return resolver;
	}

	public WrappedFactAtStatement<Fact, Stmt, Method, Value> getWithoutEdgeFunction() {
		return new WrappedFactAtStatement<Fact, Stmt, Method, Value>(statement, new WrappedFact<Fact, Stmt, Method, Value>(fact, resolver));
	}

	public FactAtStatement<Fact, Stmt> getAsFactAtStatement() {
		return new FactAtStatement<Fact, Stmt>(fact, statement);
	}

	public FactEdgeFnResolverTuple<Fact, Stmt, Method, Value> withoutStatement() {
		return new FactEdgeFnResolverTuple<Fact, Stmt, Method, Value>(fact, edgeFunction, resolver);
	}
}
