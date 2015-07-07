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
package heros.fieldsens;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.AccessPath.PrefixTestResult;
import heros.fieldsens.structs.DeltaConstraint;
import heros.fieldsens.structs.ReturnEdge;
import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;

public class ReturnSiteResolver<Field, Fact, Stmt, Method> extends ResolverTemplate<Field, Fact, Stmt, Method, ReturnEdge<Field, Fact, Stmt, Method>> {

	private Stmt returnSite;
	private AccessPath<Field> resolvedAccPath;
	private boolean propagated = false;
	private Fact sourceFact;
	private FactMergeHandler<Fact> factMergeHandler;

	public ReturnSiteResolver(FactMergeHandler<Fact> factMergeHandler, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Stmt returnSite) {
		this(factMergeHandler, analyzer, returnSite, null, new AccessPath<Field>(), null);
		this.factMergeHandler = factMergeHandler;
		propagated = false;
	}

	private ReturnSiteResolver(FactMergeHandler<Fact> factMergeHandler, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Stmt returnSite, 
			Fact sourceFact, AccessPath<Field> resolvedAccPath, ReturnSiteResolver<Field, Fact, Stmt, Method> parent) {
		super(analyzer, parent);
		this.factMergeHandler = factMergeHandler;
		this.returnSite = returnSite;
		this.sourceFact = sourceFact;
		this.resolvedAccPath = resolvedAccPath;
		propagated=true;
	}
	
	@Override
	public String toString() {
		return "<"+resolvedAccPath+":"+returnSite+">";
	}
	
	@Override
	public AccessPath<Field> getResolvedAccessPath() {
		return resolvedAccPath;
	}
	
	protected AccessPath<Field> getAccessPathOf(ReturnEdge<Field, Fact, Stmt, Method> inc) {
		if(inc.incResolver == null)
			return inc.callDelta.applyTo(new AccessPath<Field>());
		else if(inc.incResolver instanceof CallEdgeResolver)
			return inc.callDelta.applyTo(inc.incAccessPath);
		else
			return inc.incAccessPath;
	}
	
	public void addIncoming(final WrappedFact<Field, Fact, Stmt, Method> fact, 
			Resolver<Field, Fact, Stmt, Method> resolverAtCaller, 
			Delta<Field> callDelta) {
		
		addIncoming(new ReturnEdge<Field, Fact, Stmt, Method>(fact, resolverAtCaller, callDelta));
	}
	
	@Override
	protected void delegate(final ReturnEdge<Field, Fact, Stmt, Method> inc, final DeltaConstraint<Field> deltaConstraint,
			final InterestCallback<Field, Fact, Stmt, Method> callback) {
		if(inc.incResolver == null || inc.incResolver instanceof CallEdgeResolver) {
			if(inc.resolverAtCaller == null || inc.resolverAtCaller instanceof CallEdgeResolver)
				callback.canBeResolvedEmpty();
			else
				inc.resolverAtCaller.resolve(deltaConstraint, callback);
		}
		else
			inc.incResolver.resolve(deltaConstraint, new InterestCallback<Field, Fact, Stmt, Method>() {
				@Override
				public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver) {
					ReturnSiteResolver<Field,Fact,Stmt,Method> returnSiteResolver = new ReturnSiteResolver<Field, Fact, Stmt, Method>(
							factMergeHandler, ReturnSiteResolver.this.analyzer, returnSite, sourceFact, new AccessPath<Field>(), ReturnSiteResolver.this);
					returnSiteResolver.guaranteedIncomingEdges.add(new ReturnEdge<Field, Fact, Stmt, Method>(new WrappedFact<Field, Fact, Stmt, Method>(sourceFact, new AccessPath<Field>(), resolver), 
							inc.resolverAtCaller, inc.callDelta));
					callback.interest(ReturnSiteResolver.this.analyzer, returnSiteResolver);
				}
	
				@Override
				public void canBeResolvedEmpty() {
					addIncoming(inc.copyWithIncomingResolver(null, inc.usedAccessPathOfIncResolver));
				}
			});
	}
	
	@Override
	protected void resolvePotentialIncoming(final ReturnEdge<Field, Fact, Stmt, Method> inc, final DeltaConstraint<Field> deltaConstraint) {
		addPotentialIncomingEdge(inc);
	}
	
//	private void resolvePotentialViaDelta(ReturnEdge<Field, Fact, Stmt, Method> inc, DeltaConstraint<Field> deltaConstraint) {
//		AccessPath<Field> resolvedAccPath = inc.callDelta.applyTo(new AccessPath<Field>());
//		AccessPath<Field> constraint = resolvedAccPath.getDeltaTo(this.resolvedAccPath).applyTo(new AccessPath<Field>());
//		
//		if(constraint.isPrefixOf(resolvedAccPath) == PrefixTestResult.GUARANTEED_PREFIX) {
//			guaranteedIncomingEdges.add(inc.copyWithIncomingResolver(null, inc.usedAccessPathOfIncResolver));
//			interest();
//		} else if(resolvedAccPath.isPrefixOf(constraint).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
//			addPotentialIncomingEdge(inc);
//		}
//	}

	protected void processIncomingGuaranteedPrefix(ReturnEdge<Field, Fact, Stmt, Method> retEdge) {
		if(propagated) {
			factMergeHandler.merge(sourceFact, retEdge.incFact);
		} 
		else {
			propagated=true;
			sourceFact = retEdge.incFact;
			analyzer.scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, 
					new WrappedFact<Field, Fact, Stmt, Method>(retEdge.incFact, new AccessPath<Field>(), this)));
		}
	};
	
	protected void log(String message) {
		analyzer.log("Return Site "+toString()+": "+message);
	}

	@Override
	protected ResolverTemplate<Field, Fact, Stmt, Method, ReturnEdge<Field, Fact, Stmt, Method>> createNestedResolver(AccessPath<Field> newAccPath) {
		return new ReturnSiteResolver<Field, Fact, Stmt, Method>(factMergeHandler, analyzer, returnSite, sourceFact, newAccPath, this);
	}
	
	public Stmt getReturnSite() {
		return returnSite;
	}
	
}
