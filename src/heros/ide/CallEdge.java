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

import heros.ide.edgefunc.EdgeFunction;
import heros.ide.structs.WrappedFact;

public class CallEdge<Fact, Stmt, Method, Value> {

	private PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> callerAnalyzer;
	private Fact factIntoCallee;
	private EdgeFunction<Value> edgeFunctionIntoCallee;
	private Resolver<Fact, Stmt, Method, Value> resolverIntoCallee;
	private Stmt callSite;
	private Fact factAtCallSite;
	
	public CallEdge(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> callerAnalyzer, Fact factAtCallSite, Fact factIntoCallee, EdgeFunction<Value> edgeFunctionIntoCallee,
			Resolver<Fact, Stmt, Method, Value> resolverIntoCallee, Stmt callSite) {
				this.callerAnalyzer = callerAnalyzer;
				this.factAtCallSite = factAtCallSite;
				this.factIntoCallee = factIntoCallee;
				this.edgeFunctionIntoCallee = edgeFunctionIntoCallee;
				this.resolverIntoCallee = resolverIntoCallee;
				this.callSite = callSite;
	}

	public Fact getCalleeSourceFact() {
		return factIntoCallee;
	}
	
	public Resolver<Fact, Stmt, Method, Value> getResolverIntoCallee() {
		return resolverIntoCallee;
	}
	
	public WrappedFact<Fact, Stmt, Method, Value> getCallerSourceFact() {
		return callerAnalyzer.wrappedSource();
	}
	
	public Stmt getCallSite() {
		return callSite;
	}
	
	public PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> getCallerAnalyzer() {
		return callerAnalyzer;
	}
	
	public EdgeFunction<Value> getEdgeFunctionAtCallee() {
		return edgeFunctionIntoCallee;
	}
	
	public void registerInterestCallback(final PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> interestedAnalyzer) {
		final EdgeFunction<Value> composedEdgeFunction = edgeFunctionIntoCallee.composeWith(interestedAnalyzer.getConstraint());
		resolverIntoCallee.resolve(composedEdgeFunction, new InterestCallback<Fact, Stmt, Method, Value>() {
			
			@Override
			public void interest(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Resolver<Fact, Stmt, Method, Value> resolver, EdgeFunction<Value> edgeFunction) {
//				interestedAnalyzer.addIncomingEdge(new CallEdge<Fact, Stmt, Method, Value>(analyzer, factAtCallSite, factIntoCallee, edgeFunctionIntoCallee, resolver, callSite));
				interestedAnalyzer.addIncomingEdge(new CallEdge<Fact, Stmt, Method, Value>(analyzer, factAtCallSite, factIntoCallee, edgeFunction.composeWith(edgeFunctionIntoCallee), resolver, callSite));
			}
			
			@Override
			public void continueBalancedTraversal(EdgeFunction<Value> edgeFunction) {
				callerAnalyzer.getCallEdgeResolver().resolve(edgeFunction.composeWith(composedEdgeFunction), this);
			}
		});
	}

	public Fact getCallerCallSiteFact() {
		return factAtCallSite;
	}
	
	@Override
	public String toString() {
		return "[IncEdge CSite:"+getCallSite()+", FactIntoCaller: "+factIntoCallee+" , EdgeFunction: "+edgeFunctionIntoCallee+"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
		result = prime * result + ((callerAnalyzer == null) ? 0 : callerAnalyzer.hashCode());
		result = prime * result + ((edgeFunctionIntoCallee == null) ? 0 : edgeFunctionIntoCallee.hashCode());
		result = prime * result + ((factAtCallSite == null) ? 0 : factAtCallSite.hashCode());
		result = prime * result + ((factIntoCallee == null) ? 0 : factIntoCallee.hashCode());
		result = prime * result + ((resolverIntoCallee == null) ? 0 : resolverIntoCallee.hashCode());
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
		if (callSite == null) {
			if (other.callSite != null)
				return false;
		} else if (!callSite.equals(other.callSite))
			return false;
		if (callerAnalyzer == null) {
			if (other.callerAnalyzer != null)
				return false;
		} else if (!callerAnalyzer.equals(other.callerAnalyzer))
			return false;
		if (edgeFunctionIntoCallee == null) {
			if (other.edgeFunctionIntoCallee != null)
				return false;
		} else if (!edgeFunctionIntoCallee.equals(other.edgeFunctionIntoCallee))
			return false;
		if (factAtCallSite == null) {
			if (other.factAtCallSite != null)
				return false;
		} else if (!factAtCallSite.equals(other.factAtCallSite))
			return false;
		if (factIntoCallee == null) {
			if (other.factIntoCallee != null)
				return false;
		} else if (!factIntoCallee.equals(other.factIntoCallee))
			return false;
		if (resolverIntoCallee == null) {
			if (other.resolverIntoCallee != null)
				return false;
		} else if (!resolverIntoCallee.equals(other.resolverIntoCallee))
			return false;
		return true;
	}
}
