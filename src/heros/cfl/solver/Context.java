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
package heros.cfl.solver;

import heros.InterproceduralCFG;
import heros.cfl.IntersectionSolver;
import heros.cfl.NonTerminal;
import heros.cfl.RegularOverApproximizer;
import heros.cfl.SearchTree.SearchTreeResultChecker;
import heros.fieldsens.FactMergeHandler;
import heros.fieldsens.Scheduler;

public abstract class Context<Field, Fact, Stmt, Method> {

	public final InterproceduralCFG<Stmt, Method> icfg;
	public final Scheduler scheduler;
	public final Fact zeroValue;
	public final boolean followReturnsPastSeeds;
	public final FactMergeHandler<Fact> factHandler;
	public final FlowFunctions<Stmt, Fact, Method> flowFunctions;
	public final RegularOverApproximizer approximizer;
	public final IntersectionSolver intersectionSolver;
	public final NonTerminal zeroNonTerminal;
	
	Context(IFDSTabulationProblem<Stmt, Field, Fact, Method, ? extends InterproceduralCFG<Stmt, Method>> tabulationProblem, 
			Scheduler scheduler, FactMergeHandler<Fact> factHandler) {
		this.icfg = tabulationProblem.interproceduralCFG();
		this.flowFunctions = tabulationProblem.flowFunctions();
		this.scheduler = scheduler;
		this.zeroValue = tabulationProblem.zeroValue();
		this.followReturnsPastSeeds = tabulationProblem.followReturnsPastSeeds();
		this.factHandler = factHandler;
		this.approximizer = new RegularOverApproximizer();
		this.intersectionSolver = new IntersectionSolver();
		this.zeroNonTerminal = tabulationProblem.zeroNonTerminal();
	}
	
	public abstract MethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer(Method method);
}
