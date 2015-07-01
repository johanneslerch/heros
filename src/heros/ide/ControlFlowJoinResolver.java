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

import heros.fieldsens.FactMergeHandler;
import heros.ide.structs.FactEdgeFnResolverTuple;
import heros.ide.structs.FactEdgeResolverStatementTuple;

public class ControlFlowJoinResolver<Fact, Stmt, Method, Value> extends ResolverTemplate<Fact, Stmt, Method, Value, FactEdgeFnResolverTuple<Fact, Stmt, Method, Value>> {

	private Stmt joinStmt;
	private boolean propagated = false;
	private Fact sourceFact;
	private EdgeFunction<Value> resolvedEdgeFunction;
	private FactMergeHandler<Fact> factMergeHandler;

	public ControlFlowJoinResolver(FactMergeHandler<Fact> factMergeHandler, PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Stmt joinStmt) {
		this(factMergeHandler, analyzer, joinStmt, null, EdgeIdentity.<Value>v(), null);
		this.factMergeHandler = factMergeHandler;
		propagated=false;
	}
	
	private ControlFlowJoinResolver(FactMergeHandler<Fact> factMergeHandler, PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, 
			Stmt joinStmt, Fact sourceFact, EdgeFunction<Value> resolvedEdgeFunction, ControlFlowJoinResolver<Fact, Stmt, Method, Value> parent) {
		super(analyzer, parent);
		this.factMergeHandler = factMergeHandler;
		this.joinStmt = joinStmt;
		this.sourceFact = sourceFact;
		this.resolvedEdgeFunction = resolvedEdgeFunction;
		propagated=true;
	}

	@Override
	protected EdgeFunction<Value> getEdgeFunction(FactEdgeFnResolverTuple<Fact, Stmt, Method, Value> inc) {
		return inc.getEdgeFunction();
	}
	
	@Override
	protected void processIncomingGuaranteedPrefix(FactEdgeFnResolverTuple<Fact, Stmt, Method, Value> fact) {
		if(propagated) {
			factMergeHandler.merge(sourceFact, fact.getFact());
		}
		else {
			propagated=true;
			sourceFact = fact.getFact();
			analyzer.processFlowFromJoinStmt(new FactEdgeResolverStatementTuple<Fact, Stmt, Method, Value>(
					fact.getFact(), fact.getEdgeFunction(), this, joinStmt));
		}
	};
	
	@Override
	protected void processIncomingPotentialPrefix(FactEdgeFnResolverTuple<Fact, Stmt, Method, Value> fact) {
		lock();
		fact.getResolver().resolve(fact.getEdgeFunction().composeWith(resolvedEdgeFunction), new InterestCallback<Fact, Stmt, Method, Value>() {
			@Override
			public void interest(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, 
					Resolver<Fact, Stmt, Method, Value> resolver) {
				ControlFlowJoinResolver.this.interest();
			}

			@Override
			public void continueBalancedTraversal(EdgeFunction<Value> edgeFunction) {
				ControlFlowJoinResolver.this.continueBalancedTraversal(edgeFunction);
			}
		});
		unlock();
	}
	
	@Override
	protected ResolverTemplate<Fact, Stmt, Method, Value, FactEdgeFnResolverTuple<Fact, Stmt, Method, Value>> createNestedResolver(EdgeFunction<Value> edgeFunction) {
		return new ControlFlowJoinResolver<Fact, Stmt, Method, Value>(factMergeHandler, analyzer, joinStmt, sourceFact, edgeFunction, this);
	}

	@Override
	protected void log(String message) {
		analyzer.log("Join Stmt "+toString()+": "+message);
	}

	@Override
	public String toString() {
		return "<"+resolvedEdgeFunction+":"+joinStmt+">";
	}

	@Override
	protected EdgeFunction<Value> getResolvedFunction() {
		return resolvedEdgeFunction;
	}

	public Stmt getJoinStmt() {
		return joinStmt;
	}
}
