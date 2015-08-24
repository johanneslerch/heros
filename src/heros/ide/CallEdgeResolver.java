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
package heros.ide;

import heros.ide.edgefunc.EdgeFunction;
import heros.ide.structs.FactEdgeFnResolverStatementTuple;
import heros.ide.structs.WrappedFactAtStatement;

import com.google.common.collect.Lists;


class CallEdgeResolver<Fact, Stmt, Method, Value> extends ResolverTemplate<Fact, Stmt, Method, Value, CallEdge<Fact, Stmt, Method, Value>>  {

	public CallEdgeResolver(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer) {
		this(analyzer, null);
	}
	
	public CallEdgeResolver(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, CallEdgeResolver<Fact, Stmt, Method, Value> parent) {
		super(analyzer, parent);
	}

	@Override
	public EdgeFunction<Value> getResolvedFunction() {
		return analyzer.getConstraint();
	}
	
	@Override
	protected EdgeFunction<Value> getEdgeFunction(CallEdge<Fact, Stmt, Method, Value> inc) {
		return inc.getEdgeFunctionAtCallee();
	}
	
	@Override
	protected void processIncomingGuaranteedPrefix(CallEdge<Fact, Stmt, Method, Value> inc) {
		analyzer.applySummaries(inc);
	}
	
	@Override
	protected void processIncomingPotentialPrefix(CallEdge<Fact, Stmt, Method, Value> inc) {
		lock();
		inc.registerInterestCallback(analyzer);
		unlock();
	}

	@Override
	protected ResolverTemplate<Fact, Stmt, Method, Value, CallEdge<Fact, Stmt, Method, Value>> createNestedResolver(EdgeFunction<Value> edgeFunction) {
		return analyzer.createWithConstraint(edgeFunction).getCallEdgeResolver();
	}
	
	public void applySummaries(FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factAtStmt) {
		for(CallEdge<Fact, Stmt, Method, Value> incEdge : Lists.newLinkedList(incomingEdges)) {
			analyzer.applySummary(incEdge, factAtStmt);
		}
	}
	
	@Override
	public String toString() {
		return "";
	}
	
	@Override
	protected void log(String message) {
		analyzer.log(message);
	}

	public boolean hasIncomingEdges() {
		return !incomingEdges.isEmpty();
	}
}