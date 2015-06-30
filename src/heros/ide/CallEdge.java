/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.ide;

import heros.ide.structs.WrappedFact;
import heros.ide.structs.WrappedFactAtStatement;

public class CallEdge<Fact, Stmt, Method, Value> {

	private Fact calleeSourceFact;
	private PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> callerAnalyzer;
	private WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtCallSite;
	private EdgeFunction<Value> edgeFunctionAtCallee;
	
	public CallEdge(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> callerAnalyzer, 
			WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtCallSite,
			Fact calleeSourceFact, EdgeFunction<Value> edgeFunctionAtCallee) {
		this.callerAnalyzer = callerAnalyzer;
		this.factAtCallSite = factAtCallSite;
		this.calleeSourceFact = calleeSourceFact;
		this.edgeFunctionAtCallee = edgeFunctionAtCallee;
	}
	
	public Fact getCalleeSourceFact() {
		return calleeSourceFact;
	}
	
	public WrappedFact<Fact, Stmt, Method, Value> getCallerCallSiteFact() {
		return factAtCallSite.getWrappedFact();
	}
	
	public WrappedFact<Fact, Stmt, Method, Value> getCallerSourceFact() {
		return callerAnalyzer.wrappedSource();
	}
	
	public Stmt getCallSite() {
		return factAtCallSite.getStatement();
	}
	
	public PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> getCallerAnalyzer() {
		return callerAnalyzer;
	}
	
	public EdgeFunction<Value> getEdgeFunctionAtCallee() {
		return edgeFunctionAtCallee;
	}
	
	public void registerInterestCallback(final PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> interestedAnalyzer) {
		final Delta<Field> delta = calleeSourceFact.getAccessPath().getDeltaTo(interestedAnalyzer.getAccessPath());
		
		if(!factAtCallSite.canDeltaBeApplied(delta))
			return;
		
		factAtCallSite.getWrappedFact().getResolver().resolve(new DeltaConstraint<Field>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {
			
			@Override
			public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver) {
				WrappedFact<Field, Fact, Stmt, Method> calleeSourceFactWithDelta = new WrappedFact<Field, Fact, Stmt, Method>(calleeSourceFact.getFact(), delta.applyTo(calleeSourceFact.getAccessPath()), resolver);
				if(interestedAnalyzer.getAccessPath().isPrefixOf(calleeSourceFactWithDelta.getAccessPath()) != PrefixTestResult.GUARANTEED_PREFIX)
					throw new AssertionError();
				interestedAnalyzer.addIncomingEdge(new CallEdge<Field, Fact, Stmt, Method>(analyzer, 
						new WrappedFactAtStatement<Field, Fact, Stmt, Method>(factAtCallSite.getStatement(), 
											new WrappedFact<Field, Fact, Stmt, Method>(factAtCallSite.getWrappedFact().getFact(), 
													delta.applyTo(factAtCallSite.getWrappedFact().getAccessPath()), 
													resolver)), 
						calleeSourceFactWithDelta));
			}
			
			@Override
			public void canBeResolvedEmpty() {
				callerAnalyzer.getCallEdgeResolver().resolve(new DeltaConstraint<Field>(delta), this);
			}
		});
	}
	
	@Override
	public String toString() {
		return "[IncEdge CSite:"+getCallSite()+", Caller-Edge: "+getCallerSourceFact()+"->"+getCallerCallSiteFact()+",  CalleeFact: "+calleeSourceFact+"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((calleeSourceFact == null) ? 0 : calleeSourceFact.hashCode());
		result = prime * result + ((callerAnalyzer == null) ? 0 : callerAnalyzer.hashCode());
		result = prime * result + ((edgeFunctionAtCallee == null) ? 0 : edgeFunctionAtCallee.hashCode());
		result = prime * result + ((factAtCallSite == null) ? 0 : factAtCallSite.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CallEdge other = (CallEdge) obj;
		if (calleeSourceFact == null) {
			if (other.calleeSourceFact != null)
				return false;
		} else if (!calleeSourceFact.equals(other.calleeSourceFact))
			return false;
		if (callerAnalyzer == null) {
			if (other.callerAnalyzer != null)
				return false;
		} else if (!callerAnalyzer.equals(other.callerAnalyzer))
			return false;
		if (edgeFunctionAtCallee == null) {
			if (other.edgeFunctionAtCallee != null)
				return false;
		} else if (!edgeFunctionAtCallee.equals(other.edgeFunctionAtCallee))
			return false;
		if (factAtCallSite == null) {
			if (other.factAtCallSite != null)
				return false;
		} else if (!factAtCallSite.equals(other.factAtCallSite))
			return false;
		return true;
	}

	
	
}
