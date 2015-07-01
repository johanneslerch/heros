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
import heros.ide.structs.ReturnEdge;

public class ReturnSiteResolver<Fact, Stmt, Method, Value> extends ResolverTemplate<Fact, Stmt, Method, Value, ReturnEdge<Fact, Stmt, Method, Value>> {

	private Stmt returnSite;
	private EdgeFunction<Value> resolvedEdgeFunction;
	private boolean propagated = false;
	private Fact sourceFact;
	private FactMergeHandler<Fact> factMergeHandler;

	public ReturnSiteResolver(FactMergeHandler<Fact> factMergeHandler, PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Stmt returnSite) {
		this(factMergeHandler, analyzer, returnSite, null, EdgeIdentity.<Value>v(), null);
		this.factMergeHandler = factMergeHandler;
		propagated = false;
	}

	private ReturnSiteResolver(FactMergeHandler<Fact> factMergeHandler, PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Stmt returnSite, 
			Fact sourceFact, EdgeFunction<Value> resolvedEdgeFunction, ReturnSiteResolver<Fact, Stmt, Method, Value> parent) {
		super(analyzer, parent);
		this.factMergeHandler = factMergeHandler;
		this.returnSite = returnSite;
		this.sourceFact = sourceFact;
		this.resolvedEdgeFunction = resolvedEdgeFunction;
		propagated=true;
	}
	
	@Override
	public String toString() {
		return "<"+resolvedEdgeFunction+":"+returnSite+">";
	}
	
	@Override
	protected EdgeFunction<Value> getResolvedFunction() {
		return resolvedEdgeFunction;
	}
	
	@Override
	protected EdgeFunction<Value> getEdgeFunction(ReturnEdge<Fact,Stmt,Method,Value> inc) {
		return inc.incEdgeFunction;
	}
	
	public void addIncoming(final FactEdgeFnResolverTuple<Fact, Stmt, Method, Value> fact, 
			Resolver<Fact, Stmt, Method, Value> resolverAtCaller, EdgeFunction<Value> edgeFunctionIntoCallee) {
		addIncoming(new ReturnEdge<Fact, Stmt, Method, Value>(fact, resolverAtCaller, edgeFunctionIntoCallee));
	}
	
	protected void processIncomingGuaranteedPrefix(ReturnEdge<Fact, Stmt, Method, Value> retEdge) {
		if(propagated) {
			factMergeHandler.merge(sourceFact, retEdge.incFact);
		} 
		else {
			propagated=true;
			sourceFact = retEdge.incFact;
			analyzer.scheduleEdgeTo(new FactEdgeResolverStatementTuple<Fact, Stmt, Method, Value>(sourceFact, EdgeIdentity.<Value>v(), this, returnSite));
		}
	};
	
	protected void processIncomingPotentialPrefix(ReturnEdge<Fact, Stmt, Method, Value> retEdge) {
		log("Incoming potential prefix:  "+retEdge);
		resolveViaDelta(retEdge);
	};
	
	protected void log(String message) {
		analyzer.log("Return Site "+toString()+": "+message);
	}

	@Override
	protected ResolverTemplate<Fact, Stmt, Method, Value, ReturnEdge<Fact, Stmt, Method, Value>> createNestedResolver(EdgeFunction<Value> edgeFunction) {
		return new ReturnSiteResolver<Fact, Stmt, Method, Value>(factMergeHandler, analyzer, returnSite, sourceFact, edgeFunction, this);
	}
	
	public Stmt getReturnSite() {
		return returnSite;
	}
	
	private void resolveViaDelta(final ReturnEdge<Fact, Stmt, Method, Value> retEdge) {
		if(retEdge.incResolver == null || retEdge.incResolver instanceof CallEdgeResolver) {
			resolveViaDeltaAndPotentiallyDelegateToCallSite(retEdge);
		} else {
			//resolve via incoming facts resolver
			EdgeFunction<Value> constraint = retEdge.incEdgeFunction.composeWith(resolvedEdgeFunction);
			retEdge.incResolver.resolve(constraint, new InterestCallback<Fact, Stmt, Method, Value>() {

				@Override
				public void interest(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Resolver<Fact, Stmt, Method, Value> resolver) {
					incomingEdges.add(retEdge.copyWithIncomingResolver(resolver, retEdge.incAccessPath.getDeltaTo(getResolvedAccessPath())));
					ReturnSiteResolver.this.interest();
				}
				
				@Override
				public void continueBalancedTraversal(EdgeFunction<Value> edgeFunction) {
					resolveViaDeltaAndPotentiallyDelegateToCallSite(retEdge);
				}
				
				@Override
				public void canBeResolvedEmpty() {
					resolveViaDeltaAndPotentiallyDelegateToCallSite(retEdge);
				}
			});
		}			
	}

	private void resolveViaDeltaAndPotentiallyDelegateToCallSite(final ReturnEdge<Field, Fact, Stmt, Method> retEdge) {
		final AccessPath<Field> currAccPath = retEdge.callDelta.applyTo(retEdge.usedAccessPathOfIncResolver.applyTo(retEdge.incAccessPath));
		if(getResolvedAccessPath().isPrefixOf(currAccPath) == PrefixTestResult.GUARANTEED_PREFIX) {
			incomingEdges.add(retEdge.copyWithIncomingResolver(null, retEdge.usedAccessPathOfIncResolver));
			interest();
		} else if(currAccPath.isPrefixOf(getResolvedAccessPath()).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
			resolveViaCallSiteResolver(retEdge, currAccPath);
		}
	}

	protected void resolveViaCallSiteResolver(final ReturnEdge<Field, Fact, Stmt, Method> retEdge, AccessPath<Field> currAccPath) {
		if(retEdge.resolverAtCaller == null || retEdge.resolverAtCaller instanceof CallEdgeResolver) {
			canBeResolvedEmpty();
		} else {
			retEdge.resolverAtCaller.resolve(new DeltaConstraint<Field>(currAccPath.getDeltaTo(getResolvedAccessPath())), new InterestCallback<Field, Fact, Stmt, Method>() {
				@Override
				public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver) {
					incomingEdges.add(retEdge.copyWithResolverAtCaller(resolver, retEdge.incAccessPath.getDeltaTo(getResolvedAccessPath())));
					ReturnSiteResolver.this.interest();
				}
				
				@Override
				public void canBeResolvedEmpty() {
					ReturnSiteResolver.this.canBeResolvedEmpty();
				}
			});
		}
	}
}
