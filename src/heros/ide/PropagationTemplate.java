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
package heros.ide;

import java.util.Set;

import heros.FlowFunction;
import heros.ide.edgefunc.AllTop;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.structs.FactEdgeFnResolverStatementTuple;

public abstract class PropagationTemplate<Fact, Stmt, Method, Value> {

	private PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer;

	public PropagationTemplate(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer) {
		this.analyzer = analyzer;
	}
	
	public void propagate(FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factAtStmt) {
		FlowFunction<Fact> flowFunction = getFlowFunction();
		Set<Fact> targets = flowFunction.computeTargets(factAtStmt.getFact());
		for (final Fact targetFact : targets) {
			EdgeFunction<Value> edgeFunction = getEdgeFunction(targetFact);
			boolean resolveRequired = edgeFunction.mayReturnTop();
			final EdgeFunction<Value> composedFunction = factAtStmt.getEdgeFunction().composeWith(edgeFunction);
			resolveRequired &= composedFunction.mayReturnTop();
			if(composedFunction instanceof AllTop)
				break;
			
			if(resolveRequired) {
				factAtStmt.getResolver().resolve(composedFunction, new InterestCallback<Fact, Stmt, Method, Value>() {
					@Override
					public void interest(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Resolver<Fact, Stmt, Method, Value> resolver, EdgeFunction<Value> edgeFunction) {
//						propagate(analyzer, resolver, targetFact, edgeFunction.composeWith(composedFunction));
						propagate(analyzer, resolver, targetFact, composedFunction);
					}

					@Override
					public void continueBalancedTraversal(EdgeFunction<Value> balancedFunction) {
						analyzer.getCallEdgeResolver().resolve(balancedFunction.composeWith(composedFunction), this);
					}
				});
			}
			else {
				propagate(analyzer, factAtStmt.getResolver(), targetFact, composedFunction);
			}
		}
	}
	
	protected abstract void propagate(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Resolver<Fact, Stmt, Method, Value> resolver,
			Fact targetFact, EdgeFunction<Value> edgeFunction);

	protected abstract EdgeFunction<Value> getEdgeFunction(Fact targetFact);

	protected abstract FlowFunction<Fact> getFlowFunction();
}
