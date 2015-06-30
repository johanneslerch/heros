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

import heros.ide.structs.WrappedFactAtStatement;
import heros.utilities.DefaultValueMap;

public class MethodAnalyzerImpl<Fact, Stmt, Method, Value> 
		implements MethodAnalyzer<Fact, Stmt, Method, Value> {

	private Method method;
	private DefaultValueMap<Fact, PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value>> perSourceAnalyzer = 
			new DefaultValueMap<Fact, PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value>>() {
		@Override
		protected PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> createItem(Fact key) {
			return new PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value>(method, key, context);
		}
	};
	private Context<Fact, Stmt, Method, Value> context;

	MethodAnalyzerImpl(Method method, Context<Fact, Stmt, Method, Value> context) {
		this.method = method;
		this.context = context;
	}
	
	@Override
	public void addIncomingEdge(CallEdge<Fact, Stmt, Method, Value> incEdge) {
		Fact calleeSourceFact = incEdge.getCalleeSourceFact();
		PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer = perSourceAnalyzer.getOrCreate(calleeSourceFact);
		analyzer.addIncomingEdge(incEdge);
	}

	@Override
	public void addInitialSeed(Stmt startPoint, Fact val) {
		perSourceAnalyzer.getOrCreate(val).addInitialSeed(startPoint);
	}
	
	@Override
	public void addUnbalancedReturnFlow(WrappedFactAtStatement<Fact, Stmt, Method, Value> target, Stmt callSite) {
		perSourceAnalyzer.getOrCreate(context.zeroValue).scheduleUnbalancedReturnEdgeTo(target);
	}
}
