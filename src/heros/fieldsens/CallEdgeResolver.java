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

import java.util.Map;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.structs.WrappedFactAtStatement;
import heros.utilities.DefaultValueMap;

import com.google.common.collect.Lists;


public class CallEdgeResolver<Field, Fact, Stmt, Method> extends ResolverTemplate<Field, Fact, Stmt, Method, CallEdge<Field, Fact, Stmt, Method>>  {

	private CallEdgeResolver<Field, Fact, Stmt, Method> parent;
	private DefaultValueMap<Delta<Field>, CallEdgeResolver<Field, Fact, Stmt, Method>> repeatingResolvers = new DefaultValueMap<Delta<Field>, CallEdgeResolver<Field, Fact, Stmt, Method>>() {
		@Override
		protected CallEdgeResolver<Field, Fact, Stmt, Method> createItem(Delta<Field> key) {
			return analyzer.createWithRepeatingResolver(key).getCallEdgeResolver();
		}
	};

	public CallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Debugger<Field, Fact, Stmt, Method> debugger) {
		this(analyzer, debugger, null);
	}
	
	public CallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Debugger<Field, Fact, Stmt, Method> debugger, CallEdgeResolver<Field, Fact, Stmt, Method> parent) {
		super(analyzer, analyzer.getAccessPath(), parent, debugger);
		this.parent = parent;
	}

	@Override
	protected AccessPath<Field> getAccessPathOf(CallEdge<Field, Fact, Stmt, Method> inc) {
		return inc.getCalleeSourceFact().getAccessPathAndResolver().accessPath;
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
		for(CallEdge<Field, Fact, Stmt, Method> incEdge : Lists.newLinkedList(incomingEdges)) {
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