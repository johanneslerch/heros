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


import heros.InterproceduralCFG;
import heros.fieldsens.FactMergeHandler;
import heros.fieldsens.Scheduler;
import heros.ide.SourceStmtAnnotatedMethodAnalyzer.Synchronizer;

import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;


public class BiDiEagerEvaluationIDESolver<Fact, Stmt, Method, Value, I extends InterproceduralCFG<Stmt, Method>> {

	private EagerEvaluationIDESolver<Fact, Stmt, Method, Value, I> forwardSolver;
	private EagerEvaluationIDESolver<Fact, Stmt, Method, Value, I> backwardSolver;
	private Scheduler scheduler;
	private SynchronizerImpl<Stmt> forwardSynchronizer;
	private SynchronizerImpl<Stmt> backwardSynchronizer;

	public BiDiEagerEvaluationIDESolver(IDETabulationProblem<Stmt, Fact, Method, Value, I> forwardProblem,
			IDETabulationProblem<Stmt, Fact, Method, Value, I> backwardProblem, 
			FactMergeHandler<Fact> factHandler, 
			Debugger<Fact, Stmt, Method, Value> debugger,
			Scheduler scheduler) {
		
		this.scheduler = scheduler;
		
		forwardSynchronizer = new SynchronizerImpl<Stmt>();
		backwardSynchronizer = new SynchronizerImpl<Stmt>();
		forwardSynchronizer.otherSynchronizer = backwardSynchronizer;
		backwardSynchronizer.otherSynchronizer = forwardSynchronizer;
		
		forwardSolver = createSolver(forwardProblem, factHandler, debugger, forwardSynchronizer);
		backwardSolver = createSolver(backwardProblem, factHandler, debugger, backwardSynchronizer);
	}

	private EagerEvaluationIDESolver<Fact, Stmt, Method, Value, I> createSolver(IDETabulationProblem<Stmt, Fact, Method, Value, I> problem, 
			FactMergeHandler<Fact> factHandler, Debugger<Fact, Stmt, Method, Value> debugger, final SynchronizerImpl<Stmt> synchronizer) {
		return new EagerEvaluationIDESolver<Fact, Stmt, Method, Value, I>(problem, factHandler, debugger, scheduler) {
			@Override
			protected MethodAnalyzer<Fact, Stmt, Method, Value> createMethodAnalyzer(Method method) {
				return new SourceStmtAnnotatedMethodAnalyzer<Fact, Stmt, Method, Value>(method, context, synchronizer);
			}
		};
	}
	
	private static class SynchronizerImpl<Stmt> implements Synchronizer<Stmt> {
		
		private SynchronizerImpl<Stmt> otherSynchronizer;
		private Set<Stmt> leakedSources = Sets.newHashSet();
		private HashMultimap<Stmt, Runnable> pausedJobs = HashMultimap.create();

		@Override
		public void synchronizeOnStmt(Stmt stmt, Runnable job) {
			leakedSources.add(stmt);
			if(otherSynchronizer.leakedSources.contains(stmt)) {
				job.run();
				for(Runnable runnable : otherSynchronizer.pausedJobs.get(stmt)) {
					runnable.run();
				}
				otherSynchronizer.pausedJobs.removeAll(stmt);
			}
			else {
				pausedJobs.put(stmt, job);
			}
		}
	}
}
