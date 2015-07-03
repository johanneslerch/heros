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

import heros.ide.edgefunc.EdgeFunction;
import heros.ide.structs.FactEdgeFnResolverStatementTuple;
import heros.ide.structs.WrappedFactAtStatement;


public interface MethodAnalyzer<Fact,Stmt,Method, Value>  {

	public void addIncomingEdge(CallEdge<Fact, Stmt, Method, Value> incEdge);
	
	public void addInitialSeed(Stmt startPoint, Fact val, EdgeFunction<Value> edgeFunction);
	
	public void addUnbalancedReturnFlow(FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factEdgeResolverStatementTuple, Stmt callSite);
}
