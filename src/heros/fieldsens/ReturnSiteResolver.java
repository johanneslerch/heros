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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.structs.AccessPathAndResolver;
import heros.fieldsens.structs.DeltaConstraint;
import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;

public class ReturnSiteResolver<Field, Fact, Stmt, Method> extends ResolverTemplate<Field, Fact, Stmt, Method, WrappedFact<Field, Fact, Stmt, Method>> {

	private Stmt returnSite;
	private Fact sourceFact;
	private FactMergeHandler<Fact> factMergeHandler;
	private Set<PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> propagated = Sets.newHashSet();
	private ContextLogger<Method> logger;

	public ReturnSiteResolver(FactMergeHandler<Fact> factMergeHandler, Stmt returnSite, Fact sourceFact, Debugger<Field, Fact, Stmt, Method> debugger, ContextLogger<Method> logger) {
		this(factMergeHandler, returnSite, sourceFact, AccessPath.<Field>empty(), null, debugger);
		this.logger = logger;
	}
	
	private ReturnSiteResolver(FactMergeHandler<Fact> factMergeHandler, Stmt returnSite, Fact sourceFact, AccessPath<Field> resolvedAccessPath,
			ReturnSiteResolver<Field, Fact, Stmt, Method> parent, Debugger<Field, Fact, Stmt, Method> debugger) {
		super(resolvedAccessPath, parent, debugger);
		this.factMergeHandler = factMergeHandler;
		this.returnSite = returnSite;
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
	protected AccessPathAndResolver<Field, Fact, Stmt, Method> getAccessPathAndResolver(WrappedFact<Field, Fact, Stmt, Method> inc) {
		return inc.getAccessPathAndResolver();
	}
	
	@Override
	protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer(WrappedFact<Field, Fact, Stmt, Method> inc) {
		return inc.getAccessPathAndResolver().getAnalyzer();
	}

	@Override
	protected Resolver<Field, Fact, Stmt, Method> getResolver(WrappedFact<Field, Fact, Stmt, Method> inc) {
		return inc.getAccessPathAndResolver().resolver;
	}

	@Override
	public void registerTransitiveResolverCallback(TransitiveResolverCallback<Field, Fact, Stmt, Method> callback) {
		if(resolvedAccessPath.isEmpty())
			callback.resolvedByIncomingAccessPath();
		else {
			for(Resolver<Field, Fact, Stmt, Method> transRes : Lists.newLinkedList(incomingEdges.keySet())) {
				if(transRes == null)
					callback.resolvedByIncomingAccessPath();
				else if(transRes instanceof CallEdgeResolver) 
					callback.resolvedBy(getRootParent());
				else
					callback.resolvedBy(transRes);
			}
		}
	}
	
	@Override
	protected void processIncomingPotentialPrefix(final WrappedFact<Field, Fact, Stmt, Method> inc) {
		Delta<Field> delta = inc.getAccessPathAndResolver().accessPath.getDeltaTo(resolvedAccessPath);
		inc.getAccessPathAndResolver().resolve(new DeltaConstraint<Field>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {
			@Override
			public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
					AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
				AccessPath<Field> newAccPath = resolvedAccessPath.append(accPathResolver.accessPath);
				addIncoming(new WrappedFact<Field, Fact, Stmt, Method>(sourceFact, accPathResolver.withAccessPath(newAccPath)));
			}

			@Override
			public void canBeResolvedEmpty(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer) {
				ReturnSiteResolver.this.canBeResolvedEmpty(analyzer);
			}
		});
	}

	@Override
	protected void processIncomingGuaranteedPrefix(WrappedFact<Field, Fact, Stmt, Method> fact) {
		assert fact.getFact().equals(sourceFact);
		PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer = fact.getAccessPathAndResolver().getAnalyzer();
		if(!propagated.add(analyzer) || !resolvedAccessPath.isEmpty()) {
			factMergeHandler.merge(sourceFact, fact.getFact());
		}
		else {
			analyzer.scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, 
					new WrappedFact<Field, Fact, Stmt, Method>(fact.getFact(), new AccessPathAndResolver<Field, Fact, Stmt, Method>(
							analyzer, new AccessPath<Field>(), this))));
		}
	}

	@Override
	protected ResolverTemplate<Field, Fact, Stmt, Method, WrappedFact<Field, Fact, Stmt, Method>> createNestedResolver(AccessPath<Field> newAccPath) {
		return new ReturnSiteResolver<Field, Fact, Stmt, Method>(
				factMergeHandler, returnSite, sourceFact, newAccPath, this, debugger);
	}

	@Override
	protected void log(String message) {
		logger.log("Return Site "+toString()+": "+message);
	}

	@Override
	public String toString() {
		return "<"+resolvedAccessPath+":RSR-"+returnSite+" in "+logger.getMethod()+">";
	}

}
