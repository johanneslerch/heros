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


public class WrappedFactAtStatement<Fact, Stmt, Method, Value> {

	private WrappedFact<Fact, Stmt, Method, Value> fact;
	private Stmt stmt;

	public WrappedFactAtStatement(Stmt stmt, WrappedFact<Fact, Stmt, Method, Value> fact) {
		this.stmt = stmt;
		this.fact = fact;
	}

	public WrappedFact<Fact, Stmt, Method, Value> getWrappedFact() {
		return fact;
	}
	
	public Fact getFact() {
		return fact.getFact();
	}
	
	public Resolver<Fact, Stmt, Method, Value> getResolver() {
		return fact.getResolver();
	}

	public Stmt getStatement() {
		return stmt;
	}
	
	public FactAtStatement<Fact, Stmt> getAsFactAtStatement() {
		return new FactAtStatement<Fact, Stmt>(fact.getFact(), stmt);
	}
	
	public FactEdgeResolverStatementTuple<Fact, Stmt, Method, Value> withEdgeFunction(EdgeFunction<Value> edgeFunction) {
		return new FactEdgeResolverStatementTuple<Fact, Stmt, Method, Value>(fact.getFact(), edgeFunction, fact.getResolver(), stmt);
	}
	
	@Override
	public String toString() {
		return fact+" @ "+stmt;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fact == null) ? 0 : fact.hashCode());
		result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
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
		WrappedFactAtStatement other = (WrappedFactAtStatement) obj;
		if (fact == null) {
			if (other.fact != null)
				return false;
		} else if (!fact.equals(other.fact))
			return false;
		if (stmt == null) {
			if (other.stmt != null)
				return false;
		} else if (!stmt.equals(other.stmt))
			return false;
		return true;
	}

}
