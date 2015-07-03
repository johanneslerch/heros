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


public class ReturnEdge<Fact, Stmt, Method, Value> {


	public final Fact incFact;
	public final EdgeFunction<Value> incEdgeFunction;
	public final Resolver<Fact, Stmt, Method, Value> incResolver;
	public final Resolver<Fact, Stmt, Method, Value> resolverIntoCallee;
	public final EdgeFunction<Value> edgeFunctionIntoCallee;

	public ReturnEdge(FactEdgeFnResolverTuple<Fact, Stmt, Method, Value> fact, 
			Resolver<Fact, Stmt, Method, Value> resolverIntoCallee, EdgeFunction<Value> edgeFunctionIntoCallee) {
		this(fact.getFact(), fact.getEdgeFunction(), fact.getResolver(), resolverIntoCallee, edgeFunctionIntoCallee);
	}
	
	private ReturnEdge(Fact incFact, 
			EdgeFunction<Value> incEdgeFunction, 
			Resolver<Fact, Stmt, Method, Value> incResolver, 
			Resolver<Fact, Stmt, Method, Value> resolverIntoCallee, 
			EdgeFunction<Value> edgeFunctionIntoCallee) {
		this.incFact = incFact;
		this.incEdgeFunction = incEdgeFunction;
		this.incResolver = incResolver;
		this.resolverIntoCallee = resolverIntoCallee;
		this.edgeFunctionIntoCallee = edgeFunctionIntoCallee;
	}
	
	public ReturnEdge<Fact, Stmt, Method, Value> copyWithIncomingResolver(
			Resolver<Fact, Stmt, Method, Value> incResolver, EdgeFunction<Value> incEdgeFunction) {
		return new ReturnEdge<Fact, Stmt, Method, Value>(incFact, incEdgeFunction, incResolver, resolverIntoCallee, edgeFunctionIntoCallee);
	}
	
	public ReturnEdge<Fact, Stmt, Method, Value> copyWithoutIncomingResolver(
			EdgeFunction<Value> edgeFunctionIntoCallee) {
		return new ReturnEdge<Fact, Stmt, Method, Value>(incFact, EdgeIdentity.<Value>v(), null, resolverIntoCallee, edgeFunctionIntoCallee);
	}
	
	
	public ReturnEdge<Fact, Stmt, Method, Value> copyWithResolverAtCaller(
			Resolver<Fact, Stmt, Method, Value> resolverAtCaller, EdgeFunction<Value> edgeFunctionIntoCallee) {
		return new ReturnEdge<Fact, Stmt, Method, Value>(incFact, EdgeIdentity.<Value>v(), null, resolverAtCaller, edgeFunctionIntoCallee);
	}
	
	@Override
	public String toString() {
		return String.format("IncFact: %s%s, IncResolver: %s, ResolverAtCallSite: %s, EdgeIntoCallee: %s", incFact, incEdgeFunction, incResolver, resolverIntoCallee, edgeFunctionIntoCallee);
	}
	
	
}