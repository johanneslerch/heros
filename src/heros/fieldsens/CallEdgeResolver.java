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

import heros.fieldsens.structs.AccessPathAndResolver;
import heros.fieldsens.structs.WrappedFactAtStatement;

import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


public class CallEdgeResolver<Field, Fact, Stmt, Method> extends ResolverTemplate<Field, Fact, Stmt, Method, CallEdge<Field, Fact, Stmt, Method>>  {

	protected final PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer;
	private final Set<CallEdge<Field, Fact, Stmt, Method>> dismissedEdges = Sets.newHashSet();

	public CallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Debugger<Field, Fact, Stmt, Method> debugger) {
		this(analyzer, debugger, null);
	}
	
	public CallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Debugger<Field, Fact, Stmt, Method> debugger, CallEdgeResolver<Field, Fact, Stmt, Method> parent) {
		super(analyzer.getAccessPath(), parent, debugger);
		this.analyzer = analyzer;
		
		debugger.assertNewInstance(new HashedTuple(getClass(), analyzer.getMethod(), analyzer.getSourceFact(), analyzer.getAccessPath()), this);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void interestByIncoming(CallEdge<Field, Fact, Stmt, Method> inc) {
		if(!resolvedAccessPath.getExclusions().isEmpty() && inc.getCalleeSourceFact().getAccessPathAndResolver().resolver instanceof ZeroCallEdgeResolver
				/*&& resolvedAccessPath.getDeltaTo(getAccessPathOf(inc)).accesses.length == 0*/) {
			PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> zeroAnalyzer;
			AccessPath<Field> deltaTo = resolvedAccessPath.getDeltaToAsAccessPath(getAccessPathOf(inc));
			if(deltaTo.hasEmptyAccessPath()) {
				zeroAnalyzer = analyzer.createWithZeroCallEdgeResolver();
			} else {
				if(!getAccessPathOf(inc).getExclusions().isEmpty())
					System.out.println("not expected in FlowTwist: "+inc);
				deltaTo = AccessPath.<Field>empty().append(deltaTo.getFirstAccess());
				zeroAnalyzer = ((CallEdgeResolver)getOrCreateNestedResolver(resolvedAccessPath.append(deltaTo))).analyzer.createWithZeroCallEdgeResolver();
			}
			
			zeroAnalyzer.getCallEdgeResolver().incomingEdges.put(null, inc);
			zeroAnalyzer.getCallEdgeResolver().incomingEdgeValues.add(inc);
			interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(zeroAnalyzer, deltaTo, zeroAnalyzer.getCallEdgeResolver()));
		}
		else
			super.interestByIncoming(inc);
	}
	
	@Override
	protected Resolver<Field, Fact, Stmt, Method> getResolver(CallEdge<Field, Fact, Stmt, Method> inc) {
		return inc.getCalleeSourceFact().getAccessPathAndResolver().resolver;
	}
	
	@Override
	protected AccessPath<Field> getAccessPathOf(CallEdge<Field, Fact, Stmt, Method> inc) {
		return inc.getCalleeSourceFact().getAccessPathAndResolver().accessPath;
	}
	
	public PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer() {
		return analyzer;
	}

	@Override
	protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer(CallEdge<Field, Fact, Stmt, Method> inc) {
		return analyzer;
	}
	
	@Override
	protected void registerTransitiveResolverCallback(CallEdge<Field, Fact, Stmt, Method> inc,
		final TransitiveResolverCallback<Field, Fact, Stmt, Method> callback) {
		final Resolver<Field, Fact, Stmt, Method> incResolver = inc.getCalleeSourceFact().getAccessPathAndResolver().resolver;
		incResolver.registerTransitiveResolverCallback(new TransitiveResolverCallback<Field, Fact, Stmt, Method>() {
			@Override
			public void resolvedByIncomingAccessPath() {
				callback.resolvedBy(incResolver);
			}

			@Override
			public void resolvedBy(Resolver<Field, Fact, Stmt, Method> resolver) {
				callback.resolvedBy(resolver);
			}
		});
	}
	
	@Override
	public void registerTransitiveResolverCallback(TransitiveResolverCallback<Field, Fact, Stmt, Method> callback) {
		if(resolvedAccessPath.isEmpty())
			callback.resolvedByIncomingAccessPath();
		else {
			for(Resolver<Field, Fact, Stmt, Method> transRes : Lists.newLinkedList(incomingEdges.keySet())) {
				if(transRes == null)
					callback.resolvedByIncomingAccessPath();
				else
					callback.resolvedBy(transRes);
			}
		}
	}
	
	@Override
	protected void processIncomingGuaranteedPrefix(CallEdge<Field, Fact, Stmt, Method> inc) {
		analyzer.applySummaries(inc);
	}
	
	@Override
	protected void dismissByTransitiveResolver(CallEdge<Field, Fact, Stmt, Method> inc, Resolver<Field, Fact, Stmt, Method> resolver) {
		if(dismissedEdges.add(inc) && !incomingEdgeValues.contains(inc))
			analyzer.applySummaries(inc);
		//FIXME apply summaries of nested resolvers as well (if the incoming edge satisfies these)
		super.dismissByTransitiveResolver(inc, resolver);
	}
	
	@Override
	protected void processIncomingPotentialPrefix(CallEdge<Field, Fact, Stmt, Method> inc) {
		inc.registerInterestCallback(analyzer);
	}

	@Override
	protected ResolverTemplate<Field, Fact, Stmt, Method, CallEdge<Field, Fact, Stmt, Method>> createNestedResolver(AccessPath<Field> newAccPath) {
		return analyzer.createWithAccessPath(newAccPath).getCallEdgeResolver();
	}
	
	public void applySummaries(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		Set<CallEdge<Field, Fact, Stmt, Method>> edges = Sets.newHashSet(incomingEdgeValues);
		edges.addAll(dismissedEdges);
		for(CallEdge<Field, Fact, Stmt, Method> incEdge : edges) {
			analyzer.applySummary(incEdge, factAtStmt);
		}
	}
	
	@Override
	public String toString() {
		return "<"+analyzer.getAccessPath()+":CER-"+analyzer.getMethod()+">";
	}
	
	@Override
	protected void log(String message) {
		analyzer.log(message);
	}

	public boolean hasIncomingEdges() {
		return !incomingEdges.isEmpty();
	}


}