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

import heros.ide.edgefunc.AllTop;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.edgefunc.EdgeIdentity;
import heros.ide.structs.FactEdgeFnResolverStatementTuple;
import heros.solver.Pair;

public abstract class PropagationTemplate<Fact, Stmt, Method, Value> {

	private PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer;

	public PropagationTemplate(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer) {
		this.analyzer = analyzer;
	}
	
	public void propagate(FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factAtStmt) {
		FlowFunction<Fact, Value> flowFunction = getFlowFunction();
		Set<Pair<Fact, EdgeFunction<Value>>> targets = flowFunction.computeTargets(factAtStmt.getFact());
		for (final Pair<Fact, EdgeFunction<Value>> targetPair : targets) {
			final Fact targetFact = targetPair.getO1();
			EdgeFunction<Value> edgeFunction = targetPair.getO2();
			boolean resolveRequired = edgeFunction.mayReturnTop();
			final EdgeFunction<Value> composedFunction = factAtStmt.getEdgeFunction().composeWith(edgeFunction);
			if(resolveRequired) {
				if(factAtStmt.getEdgeFunction().equals(composedFunction))
					resolveRequired = false;
				else
					resolveRequired &= composedFunction.mayReturnTop();
			}
			
			if(composedFunction instanceof AllTop)
				break;
			
			if(resolveRequired) {
				factAtStmt.getResolver().resolve(composedFunction, new InterestCallback<Fact, Stmt, Method, Value>() {
					@Override
					public void interest(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Resolver<Fact, Stmt, Method, Value> resolver, EdgeFunction<Value> edgeFunction) {
						propagate(analyzer, resolver, targetFact, edgeFunction.composeWith(composedFunction));
					}

					@Override
					public void continueBalancedTraversal(EdgeFunction<Value> balancedFunction) {
						final EdgeFunction<Value> constraint = balancedFunction.composeWith(composedFunction);
						analyzer.getCallEdgeResolver().resolve(constraint, new InterestCallback<Fact, Stmt, Method, Value>() {

							@Override
							public void interest(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer,
									Resolver<Fact, Stmt, Method, Value> resolver, EdgeFunction<Value> edgeFunction) {
								assert edgeFunction.equals(EdgeIdentity.<Value>v());
								propagate(analyzer, resolver, targetFact, constraint);
							}

							@Override
							public void continueBalancedTraversal(EdgeFunction<Value> edgeFunction) {
								throw new IllegalStateException();
							}
						});
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

	protected abstract FlowFunction<Fact, Value> getFlowFunction();
}
