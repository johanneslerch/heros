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

import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.JoinLattice;
import heros.fieldsens.FactMergeHandler;
import heros.fieldsens.Scheduler;

public abstract class Context<Fact, Stmt, Method, Value> {

	public final InterproceduralCFG<Stmt, Method> icfg;
	public final Scheduler scheduler;
	public final Fact zeroValue;
	public final boolean followReturnsPastSeeds;
	public final FactMergeHandler<Fact> factHandler;
	public final FlowFunctions<Stmt, Fact, Method> flowFunctions;
	public final EdgeFunctions<Stmt, Fact, Method, Value> edgeFunctions;
	public final JoinLattice<Value> joinLattice;
	
	Context(IDETabulationProblem<Stmt, Fact, Method, Value, ? extends InterproceduralCFG<Stmt, Method>> tabulationProblem, 
			Scheduler scheduler, FactMergeHandler<Fact> factHandler) {
		this.icfg = tabulationProblem.interproceduralCFG();
		this.flowFunctions = tabulationProblem.flowFunctions();
		this.edgeFunctions = tabulationProblem.edgeFunctions();
		this.joinLattice = tabulationProblem.joinLattice();
		this.scheduler = scheduler;
		this.zeroValue = tabulationProblem.zeroValue();
		this.followReturnsPastSeeds = tabulationProblem.followReturnsPastSeeds();
		this.factHandler = factHandler;
	}
	
	public abstract MethodAnalyzer<Fact, Stmt, Method, Value> getAnalyzer(Method method);
}
