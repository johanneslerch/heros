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
import heros.fieldsens.structs.AccessPathAndResolver;
import heros.fieldsens.structs.DeltaConstraint;
import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;

import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class ReturnSiteHandling<Field, Fact, Stmt, Method> {

	private Stmt returnSite;
	private ReturnSiteHandling<Field, Fact, Stmt, Method>.CallSiteResolver callSiteResolver;
	private ReturnSiteHandling<Field, Fact, Stmt, Method>.ReturnSiteResolver returnSiteResolver;
	private boolean isRecursionPossible = false;
	private ContextLogger<Method> logger;
	private PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer;

	public ReturnSiteHandling(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Fact fact, Stmt returnSite, Debugger<Field, Fact, Stmt, Method> debugger, ContextLogger<Method> logger) {
		this.analyzer = analyzer;
		this.logger = logger;
		this.returnSite = returnSite;
		
		this.callSiteResolver = new CallSiteResolver(AccessPath.<Field>empty(), null, debugger);
		this.returnSiteResolver = new ReturnSiteResolver(AccessPath.<Field>empty(), null, debugger);
		
		AccessPathAndResolver<Field, Fact, Stmt, Method> composition = new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, AccessPath.<Field>empty(), returnSiteResolver).appendToLast(
				new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, AccessPath.<Field>empty(), callSiteResolver));
		
		analyzer.scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, 
				new WrappedFact<Field, Fact, Stmt, Method>(fact, composition)));
	}

	private void setRecursionIsPossible() {
		if(!isRecursionPossible) {
			isRecursionPossible = true;
			for(Entry<Resolver<Field, Fact, Stmt, Method>, AccessPathAndResolver<Field, Fact, Stmt, Method>> inc : Lists.newLinkedList(callSiteResolver.incomingEdges.entries())) {
				callSiteResolver.addIncoming(inc.getValue().appendToLast(new AccessPathAndResolver<Field, Fact, Stmt, Method>(
						analyzer, AccessPath.<Field>empty(), callSiteResolver)));
			}
		}
	}
	
	public void addIncomingEdge(AccessPathAndResolver<Field, Fact, Stmt, Method> returnEdge, AccessPathAndResolver<Field, Fact, Stmt, Method> callEdge,
			Resolver<Field, Fact, Stmt, Method> transitiveCallResolver) {
		if (returnEdge.exists(new Predicate<AccessPathAndResolver<Field, Fact, Stmt, Method>>() {
			@Override
			public boolean apply(AccessPathAndResolver<Field, Fact, Stmt, Method> input) {
				return input.resolver.equals(callSiteResolver);
			}
		})) {
			setRecursionIsPossible();
			AccessPathAndResolver<Field, Fact, Stmt, Method> strippedReturnEdge = returnEdge
					.removeStartingWith(new Predicate<AccessPathAndResolver<Field, Fact, Stmt, Method>>() {
						@Override
						public boolean apply(AccessPathAndResolver<Field, Fact, Stmt, Method> input) {
							return input.resolver.equals(callSiteResolver);
						}
					});
			returnSiteResolver.addIncoming(strippedReturnEdge);
		} else
			returnSiteResolver.addIncoming(returnEdge);
		callSiteResolver.addIncoming(callEdge);
		if(isRecursionPossible)
			callSiteResolver.addIncoming(callEdge.appendToLast(
					new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, AccessPath.<Field>empty(), callSiteResolver)));
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
		protected Resolver<Field, Fact, Stmt, Method> getResolver(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			return inc.resolver;
		}
		
		@Override
		protected boolean addSameTransitiveResolver() {
			return false;
		}
		
		@Override
		protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			return analyzer;
		}
		
		@Override
		protected void registerTransitiveResolverCallback(AccessPathAndResolver<Field, Fact, Stmt, Method> inc,
				TransitiveResolverCallback<Field, Fact, Stmt, Method> callback) {
//			inc.resolver.registerTransitiveResolverCallback(callback);
			callback.resolvedByIncomingAccessPath();
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
				interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, AccessPath.<Field>empty(), this));
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
		protected boolean addSameTransitiveResolver() {
			return false;
		}
		
		@Override
		protected Resolver<Field, Fact, Stmt, Method> getResolver(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			return inc.resolver;
		}
		
		@Override
		protected AccessPath<Field> getAccessPathOf(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			return inc.accessPath;
		}
		
		@Override
		protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer(AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			return analyzer;
		}
		
		@Override
		protected void registerTransitiveResolverCallback(AccessPathAndResolver<Field, Fact, Stmt, Method> inc,
				TransitiveResolverCallback<Field, Fact, Stmt, Method> callback) {
//			inc.resolver.registerTransitiveResolverCallback(callback);
			callback.resolvedByIncomingAccessPath();
		}

		@Override
		protected void processIncomingPotentialPrefix(final AccessPathAndResolver<Field, Fact, Stmt, Method> inc) {
			Delta<Field> delta = inc.accessPath.getDeltaTo(resolvedAccessPath);
			inc.resolve(new DeltaConstraint<Field>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {
				@Override
				public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
						AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
					if(accPathResolver.resolver.isParentOf(ReturnSiteResolver.this))
						setRecursionIsPossible();
					
					if(accPathResolver != accPathResolver.getLast()) {
						if(accPathResolver.getLast().resolver.equals(callSiteResolver))
							ReturnSiteResolver.this.interest(accPathResolver.removeLast());
						else if(accPathResolver.getLast().resolver instanceof ReturnSiteHandling.CallSiteResolver) {
							ReturnSiteResolver.this.interest(accPathResolver.removeLast());
							callSiteResolver.addIncoming(accPathResolver.getLast().appendToLast(
									new AccessPathAndResolver<Field, Fact, Stmt, Method>(
											ReturnSiteHandling.this.analyzer, AccessPath.<Field>empty(), callSiteResolver)));
						}
						else
							ReturnSiteResolver.this.interest(accPathResolver);
					}
					else
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
				interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, AccessPath.<Field>empty(), this));
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
