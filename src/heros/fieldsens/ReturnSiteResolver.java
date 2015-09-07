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

public class ReturnSiteResolver<Field, Fact, Stmt, Method> extends ResolverTemplate<Field, Fact, Stmt, Method, WrappedFact<Field, Fact, Stmt, Method>> {

	private Stmt returnSite;
	private boolean propagated = false;
	private Fact sourceFact;
	private FactMergeHandler<Fact> factMergeHandler;

	public ReturnSiteResolver(FactMergeHandler<Fact> factMergeHandler, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Stmt returnSite, Debugger<Field, Fact, Stmt, Method> debugger) {
		this(factMergeHandler, analyzer, returnSite, null, debugger, new AccessPath<Field>(), null);
		this.factMergeHandler = factMergeHandler;
		propagated = false;
	}

	private ReturnSiteResolver(FactMergeHandler<Fact> factMergeHandler, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Stmt returnSite, 
			Fact sourceFact, Debugger<Field, Fact, Stmt, Method> debugger, AccessPath<Field> resolvedAccPath, ReturnSiteResolver<Field, Fact, Stmt, Method> parent) {
		super(analyzer, resolvedAccPath, parent, debugger);
		this.factMergeHandler = factMergeHandler;
		this.returnSite = returnSite;
		this.sourceFact = sourceFact;
		propagated=true;
	}
	
	@Override
	protected void interestByIncoming(WrappedFact<Field, Fact, Stmt, Method> inc) {
		if(resolvedAccessPath.isEmpty())
			interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(AccessPath.<Field>empty(), this));
		else {
			AccessPath<Field> delta = resolvedAccessPath.getDeltaToAsAccessPath(inc.getAccessPathAndResolver().accessPath);
			interest(inc.getAccessPathAndResolver().withAccessPath(delta));
		}
	}
	
	@Override
	public String toString() {
		return "<"+resolvedAccessPath+":"+returnSite+" in "+analyzer.getMethod()+">";
	}
	
	@Override
	protected AccessPath<Field> getAccessPathOf(WrappedFact<Field, Fact, Stmt, Method> inc) {
		return inc.getAccessPathAndResolver().accessPath;
	}
	
	protected void processIncomingGuaranteedPrefix(WrappedFact<Field, Fact, Stmt, Method> inc) {
		if(propagated) {
			factMergeHandler.merge(sourceFact, inc.getFact());
		} 
		else {
			propagated=true;
			sourceFact = inc.getFact();
			analyzer.scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, 
					new WrappedFact<Field, Fact, Stmt, Method>(inc.getFact(), new AccessPathAndResolver<Field, Fact, Stmt, Method>(new AccessPath<Field>(), this))));
		}
	};
	
	@Override
	protected void processIncomingPotentialPrefix(final WrappedFact<Field, Fact, Stmt, Method> fact) {
		Delta<Field> delta = fact.getAccessPathAndResolver().accessPath.getDeltaTo(resolvedAccessPath);
		fact.getAccessPathAndResolver().resolve(new DeltaConstraint<Field>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {
			@Override
			public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
					AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
				ReturnSiteResolver.this.interest(accPathResolver);
			}

			@Override
			public void canBeResolvedEmpty() {
				ReturnSiteResolver.this.canBeResolvedEmpty();
			}
		});
	}
	protected void log(String message) {
		analyzer.log("Return Site "+toString()+": "+message);
	}

	@Override
	protected ReturnSiteResolver<Field, Fact, Stmt, Method> createNestedResolver(AccessPath<Field> newAccPath) {
		return new ReturnSiteResolver<Field, Fact, Stmt, Method>(factMergeHandler, analyzer, returnSite, sourceFact, debugger, newAccPath, this);
	}
	
	public Stmt getReturnSite() {
		return returnSite;
	}
	
}
