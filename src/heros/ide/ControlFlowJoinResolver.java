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
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.edgefunc.EdgeIdentity;
import heros.ide.structs.FactEdgeFnResolverTuple;
import heros.ide.structs.FactEdgeFnResolverStatementTuple;

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
			analyzer.processFlowFromJoinStmt(new FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value>(
					fact.getFact(), EdgeIdentity.<Value>v(), this, joinStmt));
		}
	};
	
	private boolean isNullOrCallEdgeResolver(Resolver<Fact, Stmt, Method, Value> resolver) {
		if(resolver == null)
			return true;
		if(resolver instanceof CallEdgeResolver) {
			return !(resolver instanceof ZeroCallEdgeResolver);
		}
		return false;
	}
	
	@Override
	protected void processIncomingPotentialPrefix(final FactEdgeFnResolverTuple<Fact, Stmt, Method, Value> fact) {
		if(isNullOrCallEdgeResolver(fact.getResolver())) {
			continueBalancedTraversal(fact.getEdgeFunction());
		}
		else {
			lock();
			fact.getResolver().resolve(fact.getEdgeFunction().composeWith(resolvedEdgeFunction), new InterestCallback<Fact, Stmt, Method, Value>() {
				@Override
				public void interest(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, 
						Resolver<Fact, Stmt, Method, Value> resolver, EdgeFunction<Value> edgeFunction) {
					addIncomingWithoutCheck(new FactEdgeFnResolverTuple<Fact, Stmt, Method, Value>(
							fact.getFact(), edgeFunction.composeWith(fact.getEdgeFunction())//TODO effect of this composition not tested!
							, resolver));
					ControlFlowJoinResolver.this.resolvedUnbalanced(edgeFunction, resolver);
				}
	
				@Override
				public void continueBalancedTraversal(EdgeFunction<Value> edgeFunction) {
					ControlFlowJoinResolver.this.continueBalancedTraversal(edgeFunction.composeWith(fact.getEdgeFunction()));
				}
			});
			unlock();
		}
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
	public EdgeFunction<Value> getResolvedFunction() {
		return resolvedEdgeFunction;
	}

	public Stmt getJoinStmt() {
		return joinStmt;
	}
}
