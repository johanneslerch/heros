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

import heros.fieldsens.structs.WrappedFactAtStatement;

import com.google.common.collect.Lists;


public class CallEdgeResolver<Field, Fact, Stmt, Method> extends ResolverTemplate<Field, Fact, Stmt, Method, CallEdge<Field, Fact, Stmt, Method>>  {

	protected final PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer;

	public CallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Debugger<Field, Fact, Stmt, Method> debugger) {
		this(analyzer, debugger, null);
	}
	
	public CallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Debugger<Field, Fact, Stmt, Method> debugger, CallEdgeResolver<Field, Fact, Stmt, Method> parent) {
		super(analyzer.getAccessPath(), parent, debugger);
		this.analyzer = analyzer;
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
	
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	@Override
//	protected void interestByIncoming(CallEdge<Field, Fact, Stmt, Method> inc) {
//		AccessPathAndResolver<Field, Fact, Stmt, Method> incAccPathRes = inc.getCalleeSourceFact().getAccessPathAndResolver();
//		if(!resolvedAccessPath.isEmpty() && incAccPathRes.resolver != this && incAccPathRes.resolver.isParentOf(this)) {
//			Delta repeatDelta = ((CallEdgeResolver)incAccPathRes.resolver).resolvedAccessPath.getDeltaTo(resolvedAccessPath);
//			PerAccessPathMethodAnalyzer<Field,Fact,Stmt,Method> repeatingAnalyzer = incAccPathRes.resolver.analyzer.createWithRepeatingResolver(repeatDelta);
//			AccessPath<Field> accPath = resolvedAccessPath.getDeltaToAsAccessPath(incAccPathRes.accessPath);
//			interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(accPath, repeatingAnalyzer.getCallEdgeResolver()));
//		}
//		else
//			super.interestByIncoming(inc);
//	}
	
	@Override
	protected boolean addSameTransitiveResolver() {
		return true;
	}
	
	@Override
	protected void registerTransitiveResolverCallback(CallEdge<Field, Fact, Stmt, Method> inc,
			final TransitiveResolverCallback<Field, Fact, Stmt, Method> callback) {
		if(resolvedAccessPath.isEmpty())
			callback.resolvedByIncomingAccessPath();
		else {
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
	}
	
	@Override
	protected void processIncomingGuaranteedPrefix(CallEdge<Field, Fact, Stmt, Method> inc) {
		analyzer.applySummaries(inc);
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
		for(CallEdge<Field, Fact, Stmt, Method> incEdge : Lists.newLinkedList(incomingEdges.values())) {
			analyzer.applySummary(incEdge, factAtStmt);
		}
	}
	
	@Override
	public String toString() {
		return "<"+analyzer.getAccessPath()+":"+analyzer.getMethod()+">";
	}
	
	@Override
	protected void log(String message) {
		analyzer.log(message);
	}

	public boolean hasIncomingEdges() {
		return !incomingEdges.isEmpty();
	}


}