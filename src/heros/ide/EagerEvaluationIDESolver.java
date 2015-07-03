/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch, Johannes Spaeth.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch, Johannes Spaeth - initial API and implementation
 ******************************************************************************/

package heros.ide;

import heros.InterproceduralCFG;
import heros.fieldsens.Debugger;
import heros.fieldsens.FactMergeHandler;
import heros.fieldsens.Scheduler;
import heros.utilities.DefaultValueMap;

import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EagerEvaluationIDESolver<D, N, M, V, I extends InterproceduralCFG<N, M>> {

	protected static final Logger logger = LoggerFactory.getLogger(EagerEvaluationIDESolver.class);
	
	private DefaultValueMap<M, MethodAnalyzer<D, N, M, V>> methodAnalyzers = new DefaultValueMap<M, MethodAnalyzer<D,N, M,V>>() {
		@Override
		protected MethodAnalyzer<D, N, M, V> createItem(M key) {
			return createMethodAnalyzer(key);
		}
	};

	private IDETabulationProblem<N, D, M, V, I> tabulationProblem;
	protected Context<D, N, M, V> context;
	private Debugger<D, N, M, I> debugger;
	private Scheduler scheduler;

	public EagerEvaluationIDESolver(IDETabulationProblem<N,D,M,V,I> tabulationProblem, FactMergeHandler<D> factHandler, Debugger<D, N, M, I> debugger, Scheduler scheduler) {
		this.tabulationProblem = tabulationProblem;
		this.scheduler = scheduler;
		this.debugger = debugger == null ? new Debugger.NullDebugger<D, N, M, I>() : debugger;
		this.debugger.setICFG(tabulationProblem.interproceduralCFG());
		context = initContext(tabulationProblem, factHandler);
		submitInitialSeeds();
	}

	private Context<D, N, M, V> initContext(IDETabulationProblem<N, D, M, V, I> tabulationProblem, FactMergeHandler<D> factHandler) {
		 return new Context<D, N, M, V>(tabulationProblem, scheduler, factHandler) {
			@Override
			public MethodAnalyzer<D, N, M, V> getAnalyzer(M method) {
				if(method == null)
					throw new IllegalArgumentException("Method must be not null");
				return methodAnalyzers.getOrCreate(method);
			}
		};
	}
	
	protected MethodAnalyzer<D, N, M, V> createMethodAnalyzer(M method) {
		return new MethodAnalyzerImpl<D, N, M, V>(method, context);
	}

	/**
	 * Schedules the processing of initial seeds, initiating the analysis.
	 */
	private void submitInitialSeeds() {
		for(Entry<N, Set<D>> seed: tabulationProblem.initialSeeds().entrySet()) {
			N startPoint = seed.getKey();
			MethodAnalyzer<D,N,M,V> analyzer = methodAnalyzers.getOrCreate(tabulationProblem.interproceduralCFG().getMethodOf(startPoint));
			for(D val: seed.getValue()) {
				analyzer.addInitialSeed(startPoint, val, tabulationProblem.initialSeedEdgeFunction(startPoint, val));
				debugger.initialSeed(startPoint);
			}
		}
	}
}
