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

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.FlowFunction.ConstrainedFact;
import heros.fieldsens.structs.AccessPathAndResolver;
import heros.fieldsens.structs.DeltaConstraint;
import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;

public class CallSiteResolver<Field, Fact, Stmt, Method> extends ResolverTemplate<Field, Fact, Stmt, Method, WrappedFact<Field, Fact, Stmt, Method>> {

	private Stmt callSite;
	private Set<PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> propagated = Sets.newHashSet();
	private Fact sourceFact;
	private FactMergeHandler<Fact> factMergeHandler;
	private ContextLogger<Method> logger;
	
	public CallSiteResolver(FactMergeHandler<Fact> factMergeHandler, Stmt callSite, Debugger<Field, Fact, Stmt, Method> debugger, ContextLogger<Method> logger) {
		this(factMergeHandler, callSite, null, new AccessPath<Field>(), debugger, null);
		this.factMergeHandler = factMergeHandler;
		this.logger = logger;
	}
	
	private CallSiteResolver(FactMergeHandler<Fact> factMergeHandler,
			Stmt callSite, Fact sourceFact, AccessPath<Field> resolvedAccPath, Debugger<Field, Fact, Stmt, Method> debugger, CallSiteResolver<Field, Fact, Stmt, Method> parent) {
		super(resolvedAccPath, parent, debugger);
		this.factMergeHandler = factMergeHandler;
		this.callSite = callSite;
		this.sourceFact = sourceFact;
		if(parent != null) {
			this.logger = parent.logger;
			this.propagated=parent.propagated;
		}
	}

	@Override
	protected AccessPath<Field> getAccessPathOf(WrappedFact<Field, Fact, Stmt, Method> inc) {
		return inc.getAccessPathAndResolver().accessPath;
	}

	@Override
	protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer(WrappedFact<Field, Fact, Stmt, Method> inc) {
		return inc.getAccessPathAndResolver().getAnalyzer();
	}

	@Override
	protected void registerTransitiveResolverCallback(WrappedFact<Field, Fact, Stmt, Method> inc,
			TransitiveResolverCallback<Field, Fact, Stmt, Method> callback) {
		inc.getAccessPathAndResolver().resolver.registerTransitiveResolverCallback(callback);
	}

	@Override
	protected Resolver<Field, Fact, Stmt, Method> getResolver(WrappedFact<Field, Fact, Stmt, Method> inc) {
		return inc.getAccessPathAndResolver().resolver;
	}

	@Override
	protected void interestByIncoming(WrappedFact<Field, Fact, Stmt, Method> inc) {
		if(resolvedAccessPath.isEmpty())
			interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(inc.getAccessPathAndResolver().getAnalyzer(), AccessPath.<Field>empty(), this));
		else {
			AccessPath<Field> delta = resolvedAccessPath.getDeltaToAsAccessPath(inc.getAccessPathAndResolver().accessPath);
			interest(inc.getAccessPathAndResolver().withAccessPath(delta));
		}
	}
	
	@Override
	protected void processIncomingPotentialPrefix(WrappedFact<Field, Fact, Stmt, Method> fact) {
		Delta<Field> delta = fact.getAccessPathAndResolver().accessPath.getDeltaTo(resolvedAccessPath);
		fact.getAccessPathAndResolver().resolve(new DeltaConstraint<Field>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {
			@Override
			public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
					AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
				CallSiteResolver.this.interest(accPathResolver);
			}

			@Override
			public void canBeResolvedEmpty(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer) {
				CallSiteResolver.this.canBeResolvedEmpty(analyzer);
			}
		});
	}

	@Override
	protected void processIncomingGuaranteedPrefix(WrappedFact<Field, Fact, Stmt, Method> fact) {
		PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer = fact.getAccessPathAndResolver().getAnalyzer();
		if(!propagated.add(analyzer)) {
			factMergeHandler.merge(sourceFact, fact.getFact());
		}
		else {
			sourceFact = fact.getFact();
			analyzer.processCallWithoutAbstractionPoint(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(callSite, 
					new WrappedFact<Field, Fact, Stmt, Method>(fact.getFact(), new AccessPathAndResolver<Field, Fact, Stmt, Method>(
							analyzer, AccessPath.<Field>empty(), this))));
		}
	}

	@Override
	protected ResolverTemplate<Field, Fact, Stmt, Method, WrappedFact<Field, Fact, Stmt, Method>> createNestedResolver(AccessPath<Field> newAccPath) {
		return new CallSiteResolver<Field, Fact, Stmt, Method>(factMergeHandler, callSite, sourceFact, newAccPath, debugger, this);
	}

	@Override
	protected void log(String message) {
		logger.log("CallSite "+toString()+": "+message);
	}

	@Override
	public String toString() {
		return "<"+resolvedAccessPath+":"+callSite+" in "+logger.getMethod()+">";
	}
}
