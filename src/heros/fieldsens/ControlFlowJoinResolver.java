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

import com.google.common.collect.Sets;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.structs.AccessPathAndResolver;
import heros.fieldsens.structs.DeltaConstraint;
import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;

public class ControlFlowJoinResolver<Field, Fact, Stmt, Method> extends ResolverTemplate<Field, Fact, Stmt, Method, WrappedFact<Field, Fact, Stmt, Method>> {

	private Stmt joinStmt;
	private Set<PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> propagated = Sets.newHashSet();
	private Fact sourceFact;
	private FactMergeHandler<Fact> factMergeHandler;
	private ContextLogger<Method> logger;

	public ControlFlowJoinResolver(FactMergeHandler<Fact> factMergeHandler, Stmt joinStmt, Debugger<Field, Fact, Stmt, Method> debugger, ContextLogger<Method> logger) {
		this(factMergeHandler, joinStmt, null, new AccessPath<Field>(), debugger, null);
		this.factMergeHandler = factMergeHandler;
		this.logger = logger;
	}
	
	private ControlFlowJoinResolver(FactMergeHandler<Fact> factMergeHandler,
			Stmt joinStmt, Fact sourceFact, AccessPath<Field> resolvedAccPath, Debugger<Field, Fact, Stmt, Method> debugger, ControlFlowJoinResolver<Field, Fact, Stmt, Method> parent) {
		super(resolvedAccPath, parent, debugger);
		this.factMergeHandler = factMergeHandler;
		this.joinStmt = joinStmt;
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
	protected Resolver<Field, Fact, Stmt, Method> getResolver(WrappedFact<Field, Fact, Stmt, Method> inc) {
		return inc.getAccessPathAndResolver().resolver;
	}
	
	@Override
	protected void registerTransitiveResolverCallback(WrappedFact<Field, Fact, Stmt, Method> inc,
			TransitiveResolverCallback<Field, Fact, Stmt, Method> callback) {
		inc.getAccessPathAndResolver().resolver.registerTransitiveResolverCallback(callback);
	}
	
	protected void processIncomingGuaranteedPrefix(heros.fieldsens.structs.WrappedFact<Field,Fact,Stmt,Method> fact) {
		PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer = fact.getAccessPathAndResolver().getAnalyzer();
		if(!propagated.add(analyzer)) {
			factMergeHandler.merge(sourceFact, fact.getFact());
		}
		else {
			sourceFact = fact.getFact();
			analyzer.processFlowFromJoinStmt(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(
					joinStmt, new WrappedFact<Field, Fact, Stmt, Method>(
					fact.getFact(), new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, new AccessPath<Field>(), this))));
		}
	};
	
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
	protected void processIncomingPotentialPrefix(final WrappedFact<Field, Fact, Stmt, Method> fact) {
		Delta<Field> delta = fact.getAccessPathAndResolver().accessPath.getDeltaTo(resolvedAccessPath);
		fact.getAccessPathAndResolver().resolve(new DeltaConstraint<Field>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {
			@Override
			public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
					AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
				ControlFlowJoinResolver.this.interest(accPathResolver);
			}

			@Override
			public void canBeResolvedEmpty() {
				ControlFlowJoinResolver.this.canBeResolvedEmpty();
			}
		});
	}
	
	@Override
	protected ResolverTemplate<Field, Fact, Stmt, Method, WrappedFact<Field, Fact, Stmt, Method>> createNestedResolver(AccessPath<Field> newAccPath) {
		return new ControlFlowJoinResolver<Field, Fact, Stmt, Method>(factMergeHandler, joinStmt, sourceFact, newAccPath, debugger, this);
	}

	@Override
	protected void log(String message) {
		logger.log("Join Stmt "+toString()+": "+message);
	}

	@Override
	public String toString() {
		return "<"+resolvedAccessPath+":"+joinStmt+" in "+logger.getMethod()+">";
	}

	public Stmt getJoinStmt() {
		return joinStmt;
	}
}
