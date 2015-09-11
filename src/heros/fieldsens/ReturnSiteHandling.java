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

import com.google.common.collect.Lists;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.structs.AccessPathAndResolver;
import heros.fieldsens.structs.DeltaConstraint;
import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;

public class ReturnSiteHandling<Field, Fact, Stmt, Method> {

	private PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer;
	private Stmt returnSite;
	private ReturnSiteHandling<Field, Fact, Stmt, Method>.CallSiteResolver callSiteResolver;
	private ReturnSiteHandling<Field, Fact, Stmt, Method>.ReturnSiteResolver returnSiteResolver;
	private boolean isRecursionPossible = false;

	public ReturnSiteHandling(Fact fact, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Stmt returnSite, Debugger<Field, Fact, Stmt, Method> debugger) {
		this.analyzer = analyzer;
		this.returnSite = returnSite;
		
		this.callSiteResolver = new CallSiteResolver(analyzer, AccessPath.<Field>empty(), null, debugger);
		this.returnSiteResolver = new ReturnSiteResolver(analyzer, AccessPath.<Field>empty(), null, debugger);
		
		AccessPathAndResolver<Field, Fact, Stmt, Method> composition = new AccessPathAndResolver<Field, Fact, Stmt, Method>(AccessPath.<Field>empty(), returnSiteResolver).appendToLast(
				new AccessPathAndResolver<Field, Fact, Stmt, Method>(AccessPath.<Field>empty(), callSiteResolver));
		
		analyzer.scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, 
				new WrappedFact<Field, Fact, Stmt, Method>(fact, composition)));
	}

	private void setRecursionIsPossible() {
		if(!isRecursionPossible) {
			isRecursionPossible = true;
			for(AccessPathAndResolver<Field, Fact, Stmt, Method> inc : Lists.newLinkedList(callSiteResolver.incomingEdges)) {
				callSiteResolver.addIncoming(inc.appendToLast(new AccessPathAndResolver<Field, Fact, Stmt, Method>(AccessPath.<Field>empty(), callSiteResolver)));
			}
		}
	}
	
	public void addIncomingEdge(AccessPathAndResolver<Field, Fact, Stmt, Method> returnEdge, AccessPathAndResolver<Field, Fact, Stmt, Method> callEdge) {
		if(returnEdge.getLast().resolver.equals(callSiteResolver))
			setRecursionIsPossible();
		
		returnSiteResolver.addIncoming(returnEdge);
		callSiteResolver.addIncoming(callEdge);
		if(isRecursionPossible)
			callSiteResolver.addIncoming(callEdge.appendToLast(new AccessPathAndResolver<Field, Fact, Stmt, Method>(AccessPath.<Field>empty(), callSiteResolver)));
	}
	
	public class CallSiteResolver extends ResolverTemplate<Field, Fact, Stmt, Method, AccessPathAndResolver<Field, Fact, Stmt, Method>> {

		public CallSiteResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, AccessPath<Field> resolvedAccessPath,
				ResolverTemplate<Field, Fact, Stmt, Method, AccessPathAndResolver<Field, Fact, Stmt, Method>> parent,
				Debugger<Field, Fact, Stmt, Method> debugger) {
			super(analyzer, resolvedAccessPath, parent, debugger);
		}

		@Override
		protected AccessPath<Field> getAccessPathOf(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			return inc.accessPath;
		}

		@Override
		protected void processIncomingPotentialPrefix(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			Delta<Field> delta = inc.accessPath.getDeltaTo(resolvedAccessPath);
			inc.resolve(new DeltaConstraint<Field>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {
				@Override
				public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
						AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
					CallSiteResolver.this.interest(accPathResolver);
				}

				@Override
				public void canBeResolvedEmpty() {
					CallSiteResolver.this.canBeResolvedEmpty();
				}
			});			
		}
		
		@Override
		protected void interestByIncoming(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			if(resolvedAccessPath.isEmpty())
				interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(AccessPath.<Field>empty(), this));
			else {
				AccessPath<Field> delta = resolvedAccessPath.getDeltaToAsAccessPath(inc.accessPath);
				interest(inc.withAccessPath(delta));
			}
		}

		@Override
		protected void processIncomingGuaranteedPrefix(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
		}

		@Override
		protected ResolverTemplate<Field, Fact, Stmt, Method, AccessPathAndResolver<Field, Fact, Stmt, Method>> createNestedResolver(
				AccessPath<Field> newAccPath) {
			return new CallSiteResolver(analyzer, newAccPath, this, debugger);
		}

		@Override
		protected void log(String message) {
			analyzer.log("Return Site "+toString()+": "+message);			
		}
		
		@Override
		public String toString() {
			return "<"+resolvedAccessPath+":call("+returnSite+" in "+analyzer.getMethod()+")>";
		}
	}
	
	private class ReturnSiteResolver extends ResolverTemplate<Field, Fact, Stmt, Method, AccessPathAndResolver<Field, Fact, Stmt, Method>> {

		public ReturnSiteResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, AccessPath<Field> resolvedAccessPath,
				ResolverTemplate<Field, Fact, Stmt, Method, AccessPathAndResolver<Field, Fact, Stmt, Method>> parent,
				Debugger<Field, Fact, Stmt, Method> debugger) {
			super(analyzer, resolvedAccessPath, parent, debugger);
		}

		@Override
		protected AccessPath<Field> getAccessPathOf(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			return inc.accessPath;
		}

		@Override
		protected void processIncomingPotentialPrefix(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			Delta<Field> delta = inc.accessPath.getDeltaTo(resolvedAccessPath);
			inc.resolve(new DeltaConstraint<Field>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {
				@Override
				public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
						AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
					if(accPathResolver.resolver.isParentOf(ReturnSiteResolver.this))
						setRecursionIsPossible();
					ReturnSiteResolver.this.interest(accPathResolver);
				}

				@Override
				public void canBeResolvedEmpty() {
					ReturnSiteResolver.this.canBeResolvedEmpty();
				}
			});	
		}
		
		@Override
		protected void interestByIncoming(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			if(resolvedAccessPath.isEmpty())
				interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(AccessPath.<Field>empty(), this));
			else {
				AccessPath<Field> delta = resolvedAccessPath.getDeltaToAsAccessPath(inc.accessPath);
				interest(inc.withAccessPath(delta));
			}
		}

		@Override
		protected void processIncomingGuaranteedPrefix(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
		}

		@Override
		protected ResolverTemplate<Field, Fact, Stmt, Method, AccessPathAndResolver<Field, Fact, Stmt, Method>> createNestedResolver(
				AccessPath<Field> newAccPath) {
			return new ReturnSiteResolver(analyzer, newAccPath, this, debugger);
		}

		@Override
		protected void log(String message) {
			analyzer.log("Return Site "+toString()+": "+message);			
		}
		
		@Override
		public String toString() {
			return "<"+resolvedAccessPath+":return("+returnSite+" in "+analyzer.getMethod()+")>";
		}
	}
}
