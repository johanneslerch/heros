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

import fj.data.Option;
import heros.cfl.CollectNonTerminalsRuleVisitor;
import heros.cfl.ConstantRule;
import heros.cfl.NonLinearRule;
import heros.cfl.NonTerminal;
import heros.cfl.ProducingTerminal;
import heros.cfl.RegularRule;
import heros.cfl.Rule;
import heros.cfl.SearchTree;
import heros.cfl.SearchTreeResultListener;
import heros.cfl.SearchTreeViewer;
import heros.cfl.TerminalUtil;
import heros.cfl.solver.FlowFunction.ConstrainedFact;
import heros.fieldsens.ContextLogger;
import heros.utilities.DefaultValueMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> {

	private static final Logger logger = LoggerFactory.getLogger(PerAccessPathMethodAnalyzer.class);
	private Fact sourceFact;
	private Map<WrappedFactAtStatement<Field,Fact, Stmt, Method>, WrappedFactAtStatement<Field,Fact, Stmt, Method>> reachableStatements = Maps.newHashMap();
	private List<WrappedFactAtStatement<Field, Fact, Stmt, Method>> summaries = Lists.newLinkedList();
	private Context<Field, Fact, Stmt, Method> context;
	private Method method;
	private DefaultValueMap<FactAtStatement<Fact, Stmt>, NonTerminal> returnSiteResolvers = new DefaultValueMap<FactAtStatement<Fact, Stmt>, NonTerminal>() {
		@Override
		protected NonTerminal createItem(FactAtStatement<Fact, Stmt> key) {
			assert context.icfg.getMethodOf(key.stmt).equals(method);
			NonTerminal nonTerminal = new NonTerminal("{RS:"+key.stmt+":"+key.fact+"}");
			scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(key.stmt, new WrappedFact<Field, Fact, Stmt, Method>(
					key.fact, new RegularRule(nonTerminal))));
			return nonTerminal;
		}
	};
	private DefaultValueMap<FactAtStatement<Fact, Stmt>, NonTerminal> ctrFlowJoinResolvers = new DefaultValueMap<FactAtStatement<Fact, Stmt>, NonTerminal>() {
		@Override
		protected NonTerminal createItem(FactAtStatement<Fact, Stmt> key) {
			assert context.icfg.getMethodOf(key.stmt).equals(method);
			NonTerminal nonTerminal = new NonTerminal("{JS:"+key.stmt+":"+key.fact+"}");
			processFlowFromJoinStmt(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(key.stmt, new WrappedFact<Field, Fact, Stmt, Method>(
					key.fact, new RegularRule(nonTerminal))));
			return nonTerminal;
		}
	};
//	private DefaultValueMap<FactAtStatement<Fact, Stmt>, NonTerminal> callSiteResolvers = new DefaultValueMap<FactAtStatement<Fact, Stmt>, NonTerminal>() {
//		@Override
//		protected NonTerminal createItem(FactAtStatement<Fact, Stmt> key) {
//			assert context.icfg.getMethodOf(key.stmt).equals(method);
//			return new NonTerminal(key);
//		}
//	};
	private NonTerminal callEdgeResolver;
	private Set<CallEdge<Field, Fact, Stmt, Method>> incomingEdges = Sets.newHashSet();

	public PerAccessPathMethodAnalyzer(Method method, Fact sourceFact, Context<Field, Fact, Stmt, Method> context) {
		if(method == null)
			throw new IllegalArgumentException("Method must be not null");
		this.method = method;
		this.sourceFact = sourceFact;
		this.context = context;
		if(isZeroSource()) {
			callEdgeResolver = new NonTerminal("{ZERO:"+method+"}");
			callEdgeResolver.addRule(new ConstantRule());
		}
		else
			this.callEdgeResolver = new NonTerminal("{SP:"+method+": "+sourceFact+"}");
	}
	
	Context<Field, Fact, Stmt, Method> getContext() {
		return context;
	}
	
	WrappedFact<Field, Fact, Stmt, Method> wrappedSource() {
		return new WrappedFact<Field, Fact, Stmt, Method>(sourceFact, new ConstantRule());
	}
	
	private boolean isBootStrapped() {
		return !incomingEdges.isEmpty();
	}

	private void bootstrapAtMethodStartPoints() {
		for(Stmt startPoint : context.icfg.getStartPointsOf(method)) {
			WrappedFactAtStatement<Field, Fact, Stmt, Method> target = new WrappedFactAtStatement<Field, Fact, Stmt, Method>(startPoint, wrappedSource());
			if(!reachableStatements.containsKey(target))
				scheduleEdgeTo(target);
		}
	}
	
	public void addInitialSeed(Stmt stmt) {
		scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(stmt, wrappedSource()));
	}
	
	private void scheduleEdgeTo(Collection<Stmt> successors, WrappedFact<Field, Fact, Stmt, Method> fact) {
		for (Stmt stmt : successors) {
			scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(stmt, fact));
		}
	}

	void scheduleEdgeTo(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		assert context.icfg.getMethodOf(factAtStmt.getStatement()).equals(method);
		
		if (reachableStatements.containsKey(factAtStmt)) {
			log("Merging "+factAtStmt);
			context.factHandler.merge(reachableStatements.get(factAtStmt).getWrappedFact().getFact(), factAtStmt.getWrappedFact().getFact());
		} else {
			log("Edge to "+factAtStmt);
			reachableStatements.put(factAtStmt, factAtStmt);
			context.scheduler.schedule(new Job(factAtStmt));
		}
	}

	void log(String message) {
		logger.trace(toString()+": "+message);
	}

	@Override
	public String toString() {
		return method+sourceFact.toString();
	}

	void processCall(WrappedFactAtStatement<Field,Fact, Stmt, Method> factAtStmt) {
//		if(context.icfg.getCalleesOfCallAt(factAtStmt.getStatement()).size() > 1) {
//			CallSiteResolver<Field,Fact,Stmt,Method> resolver = callSiteResolvers.getOrCreate(factAtStmt.getAsFactAtStatement());
//			resolver.addIncoming(factAtStmt.getWrappedFact());
//		}
//		else
			processCallWithoutAbstractionPoint(factAtStmt);
		
		processCallToReturnEdge(factAtStmt);
	}
	
	void processCallWithoutAbstractionPoint(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		Collection<Method> calledMethods = context.icfg.getCalleesOfCallAt(factAtStmt.getStatement());
		for (Method calledMethod : calledMethods) {
			FlowFunction<Fact> flowFunction = context.flowFunctions.getCallFlowFunction(factAtStmt.getStatement(), calledMethod);
			Set<ConstrainedFact<Fact>> targetFacts =  flowFunction.computeTargets(factAtStmt.getFact());
			for (ConstrainedFact<Fact> targetFact : targetFacts) {
				//TODO handle constraint
				Rule concatenatedRule = factAtStmt.getRule().append(targetFact.terminals);
				MethodAnalyzer<Field, Fact, Stmt, Method> analyzer = context.getAnalyzer(calledMethod);
				analyzer.addIncomingEdge(new CallEdge<Field, Fact, Stmt, Method>(this,
						factAtStmt, new WrappedFact<Field, Fact, Stmt, Method>(targetFact.fact, concatenatedRule)));
			}
		}
	}

	void processExit(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		log("New Summary: "+factAtStmt);
		if(!summaries.add(factAtStmt))
			throw new AssertionError();
		
		for(CallEdge<Field, Fact, Stmt, Method> incEdge : incomingEdges) {
			applySummary(incEdge, factAtStmt);
		}

		if(context.followReturnsPastSeeds && isZeroSource()) {
			Collection<Stmt> callSites = context.icfg.getCallersOf(method);
			for(Stmt callSite : callSites) {
				Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(callSite);
				for(Stmt returnSite : returnSites) {
					FlowFunction<Fact> flowFunction = context.flowFunctions.getReturnFlowFunction(callSite, method, factAtStmt.getStatement(), returnSite);
					Collection<ConstrainedFact<Fact>> targetFacts = flowFunction.computeTargets(factAtStmt.getFact());
					for (ConstrainedFact<Fact> targetFact : targetFacts) {
						//TODO handle constraint
						Rule concatenatedRule = factAtStmt.getRule().append(targetFact.terminals);
						context.getAnalyzer(context.icfg.getMethodOf(callSite)).addUnbalancedReturnFlow(
								new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite,
										new WrappedFact<Field, Fact, Stmt, Method>(targetFact.fact, concatenatedRule)), callSite);
					}
				}
			}
			//in cases where there are no callers, the return statement would normally not be processed at all;
			//this might be undesirable if the flow function has a side effect such as registering a taint;
			//instead we thus call the return flow function will a null caller
			if(callSites.isEmpty()) {
				FlowFunction<Fact> flowFunction = context.flowFunctions.getReturnFlowFunction(null, method, factAtStmt.getStatement(), null);
				flowFunction.computeTargets(factAtStmt.getFact());
			}
		}
	}

	private void processCallToReturnEdge(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		if(isLoopStart(factAtStmt.getStatement())) {
			ctrFlowJoinResolvers.getOrCreate(factAtStmt.getAsFactAtStatement()).addRule(factAtStmt.getRule());
		}
		else {
			processNonJoiningCallToReturnFlow(factAtStmt);
		}
	}

	private void processNonJoiningCallToReturnFlow(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(factAtStmt.getStatement());
		for(Stmt returnSite : returnSites) {
			FlowFunction<Fact> flowFunction = context.flowFunctions.getCallToReturnFlowFunction(factAtStmt.getStatement(), returnSite);
			Collection<ConstrainedFact<Fact>> targetFacts = flowFunction.computeTargets(factAtStmt.getFact());
			for (ConstrainedFact<Fact> targetFact : targetFacts) {
				final Rule concatenatedRule = factAtStmt.getRule().append(targetFact.terminals);
				//TODO handle constraint
				scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, new WrappedFact<Field, Fact, Stmt, Method>(targetFact.fact, concatenatedRule)));
			}
		}
	}

	private void processNormalFlow(WrappedFactAtStatement<Field,Fact, Stmt, Method> factAtStmt) {
		if(isLoopStart(factAtStmt.getStatement())) {
			ctrFlowJoinResolvers.getOrCreate(factAtStmt.getAsFactAtStatement()).addRule(factAtStmt.getRule());
		}
		else {
			processNormalNonJoiningFlow(factAtStmt);
		}
	}
	
	private boolean isLoopStart(Stmt stmt) {
		int numberOfPredecessors = context.icfg.getPredsOf(stmt).size();
		if((numberOfPredecessors > 1 && !context.icfg.isExitStmt(stmt)) || (context.icfg.isStartPoint(stmt) && numberOfPredecessors > 0)) {
			Set<Stmt> visited = Sets.newHashSet();
			List<Stmt> worklist = Lists.newLinkedList();
			worklist.addAll(context.icfg.getPredsOf(stmt));
			while(!worklist.isEmpty()) {
				Stmt current = worklist.remove(0);
				if(current.equals(stmt))
					return true;
				if(!visited.add(current))
					continue;
				worklist.addAll(context.icfg.getPredsOf(current));
			}
		}
		return false;
	}

	void processFlowFromJoinStmt(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		if(context.icfg.isCallStmt(factAtStmt.getStatement()))
			processNonJoiningCallToReturnFlow(factAtStmt);
		else
			processNormalNonJoiningFlow(factAtStmt);
	}

	private void processNormalNonJoiningFlow(final WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		final List<Stmt> successors = context.icfg.getSuccsOf(factAtStmt.getStatement());
		FlowFunction<Fact> flowFunction = context.flowFunctions.getNormalFlowFunction(factAtStmt.getStatement());
		Collection<ConstrainedFact<Fact>> targetFacts = flowFunction.computeTargets(factAtStmt.getFact());
		for (final ConstrainedFact<Fact> targetFact : targetFacts) {
			final Rule concatenatedRule = factAtStmt.getRule().append(targetFact.terminals);
			if(TerminalUtil.containsConstraints(targetFact.terminals) && TerminalUtil.containsConstraints(targetFact.terminals)) {
				final Rule candidateRule = new RegularRule(callEdgeResolver).append(concatenatedRule);
				log("Checking for solutions: "+candidateRule);
				context.approximizer.approximate(candidateRule);
				SearchTree searchTree = new SearchTree(candidateRule, 
						Option.<SearchTreeViewer> none(), context.resultChecker);
				searchTree.addListener(new SearchTreeResultListener() {
					@Override
					public void solved() {
						log("Solution found for: "+candidateRule);
						scheduleEdgeTo(successors, new WrappedFact<Field, Fact, Stmt, Method>(targetFact.fact, concatenatedRule));
					}
				});
				searchTree.search();
			}
			else {
				scheduleEdgeTo(successors, new WrappedFact<Field, Fact, Stmt, Method>(targetFact.fact, concatenatedRule));
			}
		}
	}
	
	public void addIncomingEdge(CallEdge<Field, Fact, Stmt, Method> incEdge) {
		if(isBootStrapped()) {
			context.factHandler.merge(sourceFact, incEdge.getCalleeSourceFact().getFact());
		} else 
			bootstrapAtMethodStartPoints();
		
		log("Incoming Edge: "+incEdge);
		incomingEdges.add(incEdge);
		callEdgeResolver.addRule(new RegularRule(incEdge.getCallerAnalyzer().callEdgeResolver).append(incEdge.getCalleeSourceFact().getRule()));
		applySummaries(incEdge);
	}

	void applySummary(final CallEdge<Field, Fact, Stmt, Method> incEdge, final WrappedFactAtStatement<Field, Fact, Stmt, Method> exitFact) {
		final NonTerminal callingCtx = new NonTerminal("{Calling Context}");
		callingCtx.addRule(new RegularRule(incEdge.getCallerAnalyzer().callEdgeResolver).append(incEdge.getCalleeSourceFact().getRule()));
		final Rule candidateRule = new RegularRule(callingCtx).append(exitFact.getRule());
		log("Checking if summary can be applied for incoming edge: "+incEdge+" and constraint: "+candidateRule);
		context.approximizer.approximate(candidateRule);
		SearchTree searchTree = new SearchTree(candidateRule, Option.<SearchTreeViewer>none(), new SearchTree.SearchTreeResultChecker() {
			@Override
			public boolean isSolution(Rule rule) {
				if(rule instanceof NonLinearRule && ((NonLinearRule) rule).getLeft().accept(new CollectNonTerminalsRuleVisitor()).contains(callingCtx))
					return false;
				else
					return context.resultChecker.isSolution(rule);
			}
		});
		searchTree.addListener(new SearchTreeResultListener() {
			@Override
			public void solved() {
				log("Solution found, summary will be applied for "+incEdge+". Checked: "+candidateRule);
				Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(incEdge.getCallSite());
				for(Stmt returnSite : returnSites) {
					FlowFunction<Fact> flowFunction = context.flowFunctions.getReturnFlowFunction(incEdge.getCallSite(), method, exitFact.getStatement(), returnSite);
					Set<ConstrainedFact<Fact>> targets = flowFunction.computeTargets(exitFact.getFact());
					for (ConstrainedFact<Fact> targetFact : targets) {
						context.factHandler.restoreCallingContext(targetFact.fact, incEdge.getCallerCallSiteFact().getFact());
						//TODO handle constraint
						Rule concatenatedRule = exitFact.getRule().append(targetFact.terminals);
						scheduleReturnEdge(incEdge, new WrappedFact<Field, Fact, Stmt, Method>(targetFact.fact, concatenatedRule), returnSite);
					}
				}
			}
		});
		searchTree.search();
	}

	public void scheduleUnbalancedReturnEdgeTo(WrappedFactAtStatement<Field, Fact, Stmt, Method> fact) {
		NonTerminal resolver = returnSiteResolvers.getOrCreate(fact.getAsFactAtStatement());
		resolver.addRule(fact.getRule());
	}
	
	private void scheduleReturnEdge(CallEdge<Field, Fact, Stmt, Method> callEdge, WrappedFact<Field, Fact, Stmt, Method> fact, Stmt returnSite) {
		NonTerminal resolver = callEdge.getCallerAnalyzer().returnSiteResolvers.getOrCreate(new FactAtStatement<Fact, Stmt>(fact.getFact(), returnSite));
		resolver.addRule(callEdge.getCalleeSourceFact().getRule().append(fact.getRule()));
	}

	void applySummaries(CallEdge<Field, Fact, Stmt, Method> incEdge) {
		for(WrappedFactAtStatement<Field, Fact, Stmt, Method> summary : summaries) {
			applySummary(incEdge, summary);
		}
	}
	
	public boolean isZeroSource() {
		return sourceFact.equals(context.zeroValue);
	}

	private class Job implements Runnable {

		private WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt;

		public Job(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
			this.factAtStmt = factAtStmt;
		}

		@Override
		public void run() {
			if (context.icfg.isCallStmt(factAtStmt.getStatement())) {
				processCall(factAtStmt);
			} else {
				if (context.icfg.isExitStmt(factAtStmt.getStatement())) {
					processExit(factAtStmt);
				}
				if (!context.icfg.getSuccsOf(factAtStmt.getStatement()).isEmpty()) {
					processNormalFlow(factAtStmt);
				}
			}
		}
		
		@Override
		public String toString() {
			return "Job: "+factAtStmt;
		}
	}

	public Method getMethod() {
		return method;
	}
	
	private ContextLogger<Method> getLogger() {
		return new ContextLogger<Method>() {

			@Override
			public void log(String message) {
				PerAccessPathMethodAnalyzer.this.log(message);
			}

			@Override
			public Method getMethod() {
				return method;
			}
		};
	}

	public Fact getSourceFact() {
		return sourceFact;
	}
}