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
import heros.ide.edgefunc.AllTop;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.edgefunc.EdgeIdentity;
import heros.ide.structs.FactEdgeFnResolverTuple;
import heros.ide.structs.FactEdgeFnResolverStatementTuple;
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
	public EdgeFunction<Value> getResolvedFunction() {
		return resolvedEdgeFunction;
	}
	
	@Override
	protected EdgeFunction<Value> getEdgeFunction(ReturnEdge<Fact,Stmt,Method,Value> inc) {
		return inc.incEdgeFunction;
	}
	
	public void addIncomingWithoutCheck(final FactEdgeFnResolverTuple<Fact, Stmt, Method, Value> fact, 
			Resolver<Fact, Stmt, Method, Value> resolverAtCaller, EdgeFunction<Value> edgeFunctionIntoCallee) {
		addIncomingWithoutCheck(new ReturnEdge<Fact, Stmt, Method, Value>(fact, resolverAtCaller, edgeFunctionIntoCallee));
	}
	
	protected void processIncomingGuaranteedPrefix(ReturnEdge<Fact, Stmt, Method, Value> retEdge) {
		if(propagated) {
			factMergeHandler.merge(sourceFact, retEdge.incFact);
		} 
		else {
			propagated=true;
			sourceFact = retEdge.incFact;
			analyzer.scheduleEdgeTo(new FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value>(sourceFact, EdgeIdentity.<Value>v(), this, returnSite));
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
	
	private boolean isNullOrCallEdgeResolver(Resolver<Fact, Stmt, Method, Value> resolver) {
		if(resolver == null)
			return true;
		if(resolver instanceof CallEdgeResolver) {
			return !(resolver instanceof ZeroCallEdgeResolver);
		}
		return false;
	}
	
	private void resolveViaDelta(final ReturnEdge<Fact, Stmt, Method, Value> retEdge) {
		if(isNullOrCallEdgeResolver(retEdge.incResolver)) {
			resolveViaDeltaAndPotentiallyDelegateToCallSite(retEdge, retEdge.incEdgeFunction);
		} else {
			//resolve via incoming facts resolver
			EdgeFunction<Value> constraint = retEdge.incEdgeFunction.composeWith(resolvedEdgeFunction);
			retEdge.incResolver.resolve(constraint, new InterestCallback<Fact, Stmt, Method, Value>() {

				@Override
				public void interest(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Resolver<Fact, Stmt, Method, Value> resolver, EdgeFunction<Value> edgeFunction) {
					EdgeFunction<Value> composedFunction = edgeFunction.composeWith(retEdge.incEdgeFunction);
					if(resolver instanceof ZeroCallEdgeResolver) {
						ReturnSiteResolver.this.resolvedUnbalanced(composedFunction,
								((ZeroCallEdgeResolver<Fact, Stmt, Method, Value>) resolver).copyWithAnalyzer(ReturnSiteResolver.this.analyzer));
					} else {
						addIncomingWithoutCheck(retEdge.copyWithIncomingResolver(resolver, composedFunction));
					}
				}
				
				@Override
				public void continueBalancedTraversal(EdgeFunction<Value> edgeFunction) {
					EdgeFunction<Value> composedFn = edgeFunction.composeWith(retEdge.incEdgeFunction);
					resolveViaDeltaAndPotentiallyDelegateToCallSite(retEdge, composedFn);
				}
			});
		}			
	}

	private void resolveViaDeltaAndPotentiallyDelegateToCallSite(final ReturnEdge<Fact, Stmt, Method, Value> retEdge, EdgeFunction<Value> composedFunction) {
		composedFunction = retEdge.edgeFunctionIntoCallee.composeWith(composedFunction);
		EdgeFunction<Value> constraint = composedFunction.composeWith(resolvedEdgeFunction);
		if(constraint.mayReturnTop()) {
			if(constraint instanceof AllTop)
				return;
			resolveViaCallSiteResolver(retEdge, composedFunction);
		}
		else {
			addIncomingWithoutCheck(retEdge.copyWithoutIncomingResolver(composedFunction));
		}
	}

	protected void resolveViaCallSiteResolver(final ReturnEdge<Fact, Stmt, Method, Value> retEdge, final EdgeFunction<Value> composedFunction) {
		if(isNullOrCallEdgeResolver(retEdge.resolverIntoCallee)) {
			continueBalancedTraversal(composedFunction);
		} else {
			EdgeFunction<Value> constraint = composedFunction.composeWith(resolvedEdgeFunction);
			retEdge.resolverIntoCallee.resolve(constraint, new InterestCallback<Fact, Stmt, Method, Value>() {
				@Override
				public void interest(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Resolver<Fact, Stmt, Method, Value> resolver, EdgeFunction<Value> edgeFunction) {
					ReturnSiteResolver.this.resolvedUnbalanced(edgeFunction.composeWith(composedFunction), resolver);
				}
				
				@Override
				public void continueBalancedTraversal(EdgeFunction<Value> edgeFunction) {
					EdgeFunction<Value> composedFn = edgeFunction.composeWith(composedFunction);
					ReturnSiteResolver.this.continueBalancedTraversal(composedFn);
				}
			});
		}
	}
}
