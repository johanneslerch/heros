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

import java.util.Set;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.structs.AccessPathAndResolver;
import heros.fieldsens.structs.DeltaConstraint;
import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;

public class ReturnSiteHandling<Field, Fact, Stmt, Method> {

	private Stmt returnSite;
	private ReturnSiteHandling<Field, Fact, Stmt, Method>.CallSiteResolver callSiteResolver;
	private ReturnSiteHandling<Field, Fact, Stmt, Method>.ReturnSiteResolver returnSiteResolver;
	private boolean isRecursionPossible = false;
	private Set<PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> propagated = Sets.newHashSet();
	private ContextLogger<Method> logger;
	private Fact fact;

	public ReturnSiteHandling(Fact fact, Stmt returnSite, Debugger<Field, Fact, Stmt, Method> debugger, ContextLogger<Method> logger) {
		this.fact = fact;
		this.logger = logger;
		this.returnSite = returnSite;
		
		this.callSiteResolver = new CallSiteResolver(AccessPath.<Field>empty(), null, debugger);
		this.returnSiteResolver = new ReturnSiteResolver(AccessPath.<Field>empty(), null, debugger);
	}

	private void setRecursionIsPossible() {
		if(!isRecursionPossible) {
			isRecursionPossible = true;
			for(Entry<Resolver<Field, Fact, Stmt, Method>, AccessPathAndResolver<Field, Fact, Stmt, Method>> inc : Lists.newLinkedList(callSiteResolver.incomingEdges.entries())) {
				callSiteResolver.addIncoming(inc.getValue().appendToLast(new AccessPathAndResolver<Field, Fact, Stmt, Method>(
						inc.getValue().getAnalyzer(), AccessPath.<Field>empty(), callSiteResolver)),
						inc.getKey());
			}
		}
	}
	
	public void addIncomingEdge(AccessPathAndResolver<Field, Fact, Stmt, Method> returnEdge, AccessPathAndResolver<Field, Fact, Stmt, Method> callEdge,
			Resolver<Field, Fact, Stmt, Method> transitiveCallResolver) {
		if(propagated.add(callEdge.getAnalyzer())) {
			AccessPathAndResolver<Field, Fact, Stmt, Method> composition = new AccessPathAndResolver<Field, Fact, Stmt, Method>(callEdge.getAnalyzer(), AccessPath.<Field>empty(), returnSiteResolver).appendToLast(
					new AccessPathAndResolver<Field, Fact, Stmt, Method>(callEdge.getAnalyzer(), AccessPath.<Field>empty(), callSiteResolver));
			
			callEdge.getAnalyzer().scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, 
					new WrappedFact<Field, Fact, Stmt, Method>(fact, composition)));
		}
		
		if(returnEdge.getLast().resolver.equals(callSiteResolver))
			setRecursionIsPossible();
		
		returnSiteResolver.addIncoming(returnEdge, null);
		callSiteResolver.addIncoming(callEdge, transitiveCallResolver);
		if(isRecursionPossible)
			callSiteResolver.addIncoming(callEdge.appendToLast(
					new AccessPathAndResolver<Field, Fact, Stmt, Method>(callEdge.getAnalyzer(), AccessPath.<Field>empty(), callSiteResolver)), transitiveCallResolver);
	}
	
	public class CallSiteResolver extends ResolverTemplate<Field, Fact, Stmt, Method, AccessPathAndResolver<Field, Fact, Stmt, Method>> {

		public CallSiteResolver(AccessPath<Field> resolvedAccessPath,
				ResolverTemplate<Field, Fact, Stmt, Method, AccessPathAndResolver<Field, Fact, Stmt, Method>> parent,
				Debugger<Field, Fact, Stmt, Method> debugger) {
			super(resolvedAccessPath, parent, debugger);
		}

		@Override
		protected AccessPath<Field> getAccessPathOf(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			return inc.accessPath;
		}
		
		@Override
		protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			return inc.getAnalyzer();
		}

		@Override
		protected void processIncomingPotentialPrefix(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			Delta<Field> delta = inc.accessPath.getDeltaTo(resolvedAccessPath);
			inc.resolve(new DeltaConstraint<Field>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {
				@Override
				public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
						AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver, Resolver<Field, Fact, Stmt, Method> transitiveResolver) {
					CallSiteResolver.this.interest(accPathResolver, transitiveResolver);
				}

				@Override
				public void canBeResolvedEmpty() {
					CallSiteResolver.this.canBeResolvedEmpty();
				}
			});			
		}
		
		@Override
		protected void interestByIncoming(AccessPathAndResolver<Field, Fact, Stmt, Method> inc, Resolver<Field, Fact, Stmt, Method> transitiveResolver) {
			if(resolvedAccessPath.isEmpty())
				interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(inc.getAnalyzer(), AccessPath.<Field>empty(), this), null);
			else {
				AccessPath<Field> delta = resolvedAccessPath.getDeltaToAsAccessPath(inc.accessPath);
				interest(inc.withAccessPath(delta), transitiveResolver);
			}
		}

		@Override
		protected void processIncomingGuaranteedPrefix(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
		}

		@Override
		protected ResolverTemplate<Field, Fact, Stmt, Method, AccessPathAndResolver<Field, Fact, Stmt, Method>> createNestedResolver(
				AccessPath<Field> newAccPath) {
			return new CallSiteResolver(newAccPath, this, debugger);
		}

		@Override
		protected void log(String message) {
			logger.log("Return Site "+toString()+": "+message);			
		}
		
		@Override
		public String toString() {
			return "<"+resolvedAccessPath+":call("+returnSite+" in "+logger.getMethod()+")>";
		}
	}
	
	private class ReturnSiteResolver extends ResolverTemplate<Field, Fact, Stmt, Method, AccessPathAndResolver<Field, Fact, Stmt, Method>> {

		public ReturnSiteResolver(AccessPath<Field> resolvedAccessPath,
				ResolverTemplate<Field, Fact, Stmt, Method, AccessPathAndResolver<Field, Fact, Stmt, Method>> parent,
				Debugger<Field, Fact, Stmt, Method> debugger) {
			super( resolvedAccessPath, parent, debugger);
		}

		@Override
		protected AccessPath<Field> getAccessPathOf(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			return inc.accessPath;
		}
		
		@Override
		protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			return inc.getAnalyzer();
		}

		@Override
		protected void processIncomingPotentialPrefix(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			Delta<Field> delta = inc.accessPath.getDeltaTo(resolvedAccessPath);
			inc.resolve(new DeltaConstraint<Field>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {
				@Override
				public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
						AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver, Resolver<Field, Fact, Stmt, Method> transitiveResolver) {
					if(accPathResolver.resolver.isParentOf(ReturnSiteResolver.this))
						setRecursionIsPossible();
					ReturnSiteResolver.this.interest(accPathResolver, transitiveResolver);
				}

				@Override
				public void canBeResolvedEmpty() {
					ReturnSiteResolver.this.canBeResolvedEmpty();
				}
			});	
		}
		
		@Override
		protected void interestByIncoming(AccessPathAndResolver<Field, Fact, Stmt, Method> inc, Resolver<Field, Fact, Stmt, Method> transitiveResolver) {
			if(resolvedAccessPath.isEmpty())
				interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(inc.getAnalyzer(), AccessPath.<Field>empty(), this), null);
			else {
				AccessPath<Field> delta = resolvedAccessPath.getDeltaToAsAccessPath(inc.accessPath);
				interest(inc.withAccessPath(delta), transitiveResolver);
			}
		}

		@Override
		protected void processIncomingGuaranteedPrefix(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
		}

		@Override
		protected ResolverTemplate<Field, Fact, Stmt, Method, AccessPathAndResolver<Field, Fact, Stmt, Method>> createNestedResolver(
				AccessPath<Field> newAccPath) {
			return new ReturnSiteResolver(newAccPath, this, debugger);
		}

		@Override
		protected void log(String message) {
			logger.log("Return Site "+toString()+": "+message);			
		}
		
		@Override
		public String toString() {
			return "<"+resolvedAccessPath+":return("+returnSite+" in "+logger.getMethod()+")>";
		}
	}
}
