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

import heros.FlowFunction;
import heros.ide.structs.FactAtStatement;
import heros.ide.structs.WrappedFact;
import heros.ide.structs.WrappedFactAtStatement;
import heros.solver.Pair;
import heros.utilities.DefaultValueMap;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

class PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> {

	private static final Logger logger = LoggerFactory.getLogger(PerAccessPathMethodAnalyzer.class);
	private Fact sourceFact;
	private Map<WrappedFactAtStatement<Fact, Stmt, Method, Value>, Pair<WrappedFactAtStatement<Fact, Stmt, Method, Value>, EdgeFunction<Value>>> reachableStatements = Maps.newHashMap();
	private List<WrappedFactAtStatement<Fact, Stmt, Method, Value>> summaries = Lists.newLinkedList();
	private EdgeFunction<Value> constraint;
	private Context<Fact, Stmt, Method, Value> context;
	private Method method;
	private DefaultValueMap<FactAtStatement<Fact, Stmt>, ReturnSiteResolver<Fact, Stmt, Method, Value>> returnSiteResolvers = new DefaultValueMap<FactAtStatement<Fact, Stmt>, ReturnSiteResolver<Fact,Stmt,Method, Value>>() {
		@Override
		protected ReturnSiteResolver<Fact, Stmt, Method, Value> createItem(FactAtStatement<Fact, Stmt> key) {
			return new ReturnSiteResolver<Fact, Stmt, Method, Value>(context.factHandler, PerAccessPathMethodAnalyzer.this, key.stmt);
		}
	};
	private DefaultValueMap<FactAtStatement<Fact, Stmt>, ControlFlowJoinResolver<Fact, Stmt, Method, Value>> ctrFlowJoinResolvers = new DefaultValueMap<FactAtStatement<Fact, Stmt>, ControlFlowJoinResolver<Fact,Stmt,Method, Value>>() {
		@Override
		protected ControlFlowJoinResolver<Fact, Stmt, Method, Value> createItem(FactAtStatement<Fact, Stmt> key) {
			return new ControlFlowJoinResolver<Fact, Stmt, Method, Value>(context.factHandler, PerAccessPathMethodAnalyzer.this, key.stmt);
		}
	};
	private CallEdgeResolver<Fact, Stmt, Method, Value> callEdgeResolver;
	private PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> parent;

	public PerAccessPathMethodAnalyzer(Method method, Fact sourceFact, Context<Fact, Stmt, Method, Value> context) {
		this(method, sourceFact, context, EdgeIdentity.<Value>v(), null);
	}
	
	private PerAccessPathMethodAnalyzer(Method method, Fact sourceFact, Context<Fact, Stmt, Method, Value> context, EdgeFunction<Value> constraint, PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> parent) {
		if(method == null)
			throw new IllegalArgumentException("Method must be not null");
		this.constraint = constraint;
		this.parent = parent;
		this.method = method;
		this.sourceFact = sourceFact;
		this.context = context;
		if(parent == null) {
			this.callEdgeResolver = isZeroSource() ? new ZeroCallEdgeResolver<Fact, Stmt, Method, Value>(this, context.zeroHandler) : new CallEdgeResolver<Fact, Stmt, Method, Value>(this);
		}
		else {
			this.callEdgeResolver = isZeroSource() ? parent.callEdgeResolver : new CallEdgeResolver<Fact, Stmt, Method, Value>(this, parent.callEdgeResolver);
		}
		log("initialized");
	}
	
	public PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> createWithConstraint(EdgeFunction<Value> constraint) {
		return new PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value>(method, sourceFact, context, constraint, this);
	}
	
	private boolean isBootStrapped() {
		return callEdgeResolver.hasIncomingEdges() || !(constraint instanceof EdgeIdentity);
	}

	private void bootstrapAtMethodStartPoints() {
		callEdgeResolver.interest();
		for(Stmt startPoint : context.icfg.getStartPointsOf(method)) {
			WrappedFactAtStatement<Fact, Stmt, Method, Value> target = new WrappedFactAtStatement<Fact, Stmt, Method, Value>(startPoint, wrappedSource());
			if(!reachableStatements.containsKey(target))
				scheduleEdgeTo(target, EdgeIdentity.<Value>v());
		}
	}
	
	private WrappedFact<Fact, Stmt, Method, Value> wrappedSource() {
		return new WrappedFact<Fact, Stmt, Method, Value>(sourceFact, callEdgeResolver);
	}

	public void addInitialSeed(Stmt stmt) {
		scheduleEdgeTo(new WrappedFactAtStatement<Fact, Stmt, Method, Value>(stmt, wrappedSource()), EdgeIdentity.<Value>v());
	}
	
	private void scheduleEdgeTo(Collection<Stmt> successors, WrappedFact<Fact, Stmt, Method, Value> fact, EdgeFunction<Value> edgeFunction) {
		for (Stmt stmt : successors) {
			scheduleEdgeTo(new WrappedFactAtStatement<Fact, Stmt, Method, Value>(stmt, fact), edgeFunction);
		}
	}

	void scheduleEdgeTo(WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt, EdgeFunction<Value> edgeFunction) {
		assert context.icfg.getMethodOf(factAtStmt.getStatement()).equals(method);
		if (reachableStatements.containsKey(factAtStmt)) {
			log("Merging "+factAtStmt);
			Pair<WrappedFactAtStatement<Fact, Stmt, Method, Value>, EdgeFunction<Value>> prevFact = reachableStatements.get(factAtStmt);
			context.factHandler.merge(prevFact.getO1().getWrappedFact().getFact(), factAtStmt.getWrappedFact().getFact());
			EdgeFunction<Value> joinedFunction = prevFact.getO2().joinWith(edgeFunction);
			if(!joinedFunction.equalTo(prevFact.getO2())) {
				log("New EdgeFunction at "+factAtStmt+": "+joinedFunction);
				reachableStatements.put(factAtStmt, new Pair<WrappedFactAtStatement<Fact,Stmt,Method,Value>, EdgeFunction<Value>>(prevFact.getO1(), joinedFunction));
				context.scheduler.schedule(new Job(factAtStmt, joinedFunction));
			}
		} else {
			log("Edge to "+factAtStmt+" with EdgeFunction: "+edgeFunction);
			reachableStatements.put(factAtStmt, new Pair<WrappedFactAtStatement<Fact,Stmt,Method,Value>, EdgeFunction<Value>>(factAtStmt, edgeFunction));
			context.scheduler.schedule(new Job(factAtStmt, edgeFunction));
		}
	}

	void log(String message) {
		logger.trace("[{}; {}{}: "+message+"]", method, sourceFact, constraint);
	}

	@Override
	public String toString() {
		return method+"; "+sourceFact+constraint;
	}

	void processCall(WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt, EdgeFunction<Value> edgeFunction) {
		Collection<Method> calledMethods = context.icfg.getCalleesOfCallAt(factAtStmt.getStatement());
		for (Method calledMethod : calledMethods) {
			FlowFunction<Fact> flowFunction = context.flowFunctions.getCallFlowFunction(factAtStmt.getStatement(), calledMethod);
			Set<Fact> targetFacts = flowFunction.computeTargets(factAtStmt.getFact());
			for (Fact targetFact : targetFacts) {
				EdgeFunction<Value> callEdgeFunction = context.edgeFunctions.getCallEdgeFunction(factAtStmt.getStatement(), factAtStmt.getFact(), calledMethod, targetFact);
				boolean resolveRequired = callEdgeFunction.mayReturnTop();
				EdgeFunction<Value> composedFunction = edgeFunction.composeWith(callEdgeFunction);
				resolveRequired &= composedFunction.mayReturnTop();
				if(composedFunction instanceof AllTop)
					break;
				
				if(resolveRequired) {
					//TODO
				}
				else {
					MethodAnalyzer<Fact, Stmt, Method, Value> analyzer = context.getAnalyzer(calledMethod);
					analyzer.addIncomingEdge(new CallEdge<Fact, Stmt, Method, Value>(this,
							factAtStmt, targetFact));
				}
			}
		}
		
		processCallToReturnEdge(factAtStmt, edgeFunction);
	}

	void processExit(WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt, EdgeFunction<Value> edgeFunction) {
		log("New Summary: "+factAtStmt);
		if(!summaries.add(factAtStmt))
			throw new AssertionError();

		callEdgeResolver.applySummaries(factAtStmt, edgeFunction);

		if(context.followReturnsPastSeeds && isZeroSource()) {
			Collection<Stmt> callSites = context.icfg.getCallersOf(method);
			for(Stmt callSite : callSites) {
				Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(callSite);
				for(Stmt returnSite : returnSites) {
					FlowFunction<Fact> flowFunction = context.flowFunctions.getReturnFlowFunction(callSite, method, factAtStmt.getStatement(), returnSite);
					Set<Fact> targetFacts = flowFunction.computeTargets(factAtStmt.getFact());
					for (Fact targetFact : targetFacts) {
						EdgeFunction<Value> returnEdgeFunction = context.edgeFunctions.getReturnEdgeFunction(callSite, method, factAtStmt.getStatement(), factAtStmt.getFact(), returnSite, targetFact);
						boolean resolveRequired = returnEdgeFunction.mayReturnTop();
						EdgeFunction<Value> composedFunction = edgeFunction.composeWith(returnEdgeFunction);
						resolveRequired &= composedFunction.mayReturnTop();
						if(composedFunction instanceof AllTop)
							break;
						
						if(resolveRequired) {
							//TODO
						}
						else {
							context.getAnalyzer(context.icfg.getMethodOf(callSite)).addUnbalancedReturnFlow(new WrappedFactAtStatement<Fact, Stmt, Method, Value>(returnSite, targetFact), callSite);
						}
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
		Stmt stmt = factAtStmt.getStatement();
		int numberOfPredecessors = context.icfg.getPredsOf(stmt).size();		
		if(numberOfPredecessors > 1 || (context.icfg.isStartPoint(stmt) && numberOfPredecessors > 0)) {
			ctrFlowJoinResolvers.getOrCreate(factAtStmt.getAsFactAtStatement()).addIncoming(factAtStmt.getWrappedFact());
		}
		else {
			processNonJoiningCallToReturnFlow(factAtStmt);
		}
	}

	private void processNonJoiningCallToReturnFlow(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(factAtStmt.getStatement());
		for(Stmt returnSite : returnSites) {
			FlowFunction<Field, Fact, Stmt, Method> flowFunction = context.flowFunctions.getCallToReturnFlowFunction(factAtStmt.getStatement(), returnSite);
			Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts = flowFunction.computeTargets(factAtStmt.getFact(), new AccessPathHandler<Field, Fact, Stmt, Method>(factAtStmt.getAccessPath(), factAtStmt.getResolver()));
			for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
				//TODO handle constraint
				scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, targetFact.getFact()));
			}
		}
	}

	private void processNormalFlow(WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt, EdgeFunction<Value> edgeFunction) {
		Stmt stmt = factAtStmt.getStatement();
		int numberOfPredecessors = context.icfg.getPredsOf(stmt).size();
		if((numberOfPredecessors > 1 && !context.icfg.isExitStmt(stmt)) || (context.icfg.isStartPoint(stmt) && numberOfPredecessors > 0)) {
			ctrFlowJoinResolvers.getOrCreate(factAtStmt.getAsFactAtStatement()).addIncoming(factAtStmt.getWrappedFact());
		}
		else {
			processNormalNonJoiningFlow(factAtStmt, edgeFunction);
		}
	}

	void processFlowFromJoinStmt(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		if(context.icfg.isCallStmt(factAtStmt.getStatement()))
			processNonJoiningCallToReturnFlow(factAtStmt);
		else
			processNormalNonJoiningFlow(factAtStmt);
	}

	private void processNormalNonJoiningFlow(WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt, EdgeFunction<Value> edgeFunction) {
		final List<Stmt> successors = context.icfg.getSuccsOf(factAtStmt.getStatement());
		for(Stmt successor : successors) {
			FlowFunction<Fact> flowFunction = context.flowFunctions.getNormalFlowFunction(factAtStmt.getStatement(), successor);
			Set<Fact> targetFacts = flowFunction.computeTargets(factAtStmt.getFact());
			for (final Fact targetFact : targetFacts) {
				EdgeFunction<Value> normalEdgeFunction = context.edgeFunctions.getNormalEdgeFunction(factAtStmt.getStatement(), factAtStmt.getFact(), successor, targetFact);
				boolean resolveRequired = normalEdgeFunction.mayReturnTop();
				EdgeFunction<Value> composedFunction = edgeFunction.composeWith(normalEdgeFunction);
				resolveRequired &= composedFunction.mayReturnTop();
				if(composedFunction instanceof AllTop)
					break;
				
				if(resolveRequired) {
					//TODO
				}
				else {
					scheduleEdgeTo(successors, targetFact, composedFunction);
				}
				
				if(targetFact.getConstraint() == null)
				else {
					targetFact.getFact().getResolver().resolve(targetFact.getConstraint(), new InterestCallback<Field, Fact, Stmt, Method>() {
						@Override
						public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
								Resolver<Field, Fact, Stmt, Method> resolver) {
							analyzer.scheduleEdgeTo(successors, new WrappedFact<Field, Fact, Stmt, Method>(targetFact.getFact().getFact(), targetFact.getFact().getAccessPath(), resolver));
						}
						
						@Override
						public void canBeResolvedEmpty() {
							callEdgeResolver.resolve(targetFact.getConstraint(), this);
						}
					});
				}
			}
			
		}
	}
	
	public void addIncomingEdge(CallEdge<Field, Fact, Stmt, Method> incEdge) {
		if(isBootStrapped()) {
			context.factHandler.merge(sourceFact, incEdge.getCalleeSourceFact().getFact());
		} else 
			bootstrapAtMethodStartPoints();
		callEdgeResolver.addIncoming(incEdge);
	}

	void applySummary(CallEdge<Field, Fact, Stmt, Method> incEdge, WrappedFactAtStatement<Field, Fact, Stmt, Method> exitFact) {
		Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(incEdge.getCallSite());
		for(Stmt returnSite : returnSites) {
			FlowFunction<Field, Fact, Stmt, Method> flowFunction = context.flowFunctions.getReturnFlowFunction(incEdge.getCallSite(), method, exitFact.getStatement(), returnSite);
			Set<ConstrainedFact<Field, Fact, Stmt, Method>> targets = flowFunction.computeTargets(exitFact.getFact(), new AccessPathHandler<Field, Fact, Stmt, Method>(exitFact.getAccessPath(), exitFact.getResolver()));
			for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targets) {
				context.factHandler.restoreCallingContext(targetFact.getFact().getFact(), incEdge.getCallerCallSiteFact().getFact());
				//TODO handle constraint
				scheduleReturnEdge(incEdge, targetFact.getFact(), returnSite);
			}
		}
	}

	public void scheduleUnbalancedReturnEdgeTo(WrappedFactAtStatement<Field, Fact, Stmt, Method> fact) {
		ReturnSiteResolver<Field,Fact,Stmt,Method> resolver = returnSiteResolvers.getOrCreate(fact.getAsFactAtStatement());
		resolver.addIncoming(new WrappedFact<Field, Fact, Stmt, Method>(fact.getWrappedFact().getFact(), fact.getWrappedFact().getAccessPath(), 
				fact.getWrappedFact().getResolver()), null, Delta.<Field>empty());
	}
	
	private void scheduleReturnEdge(CallEdge<Field, Fact, Stmt, Method> incEdge, WrappedFact<Field, Fact, Stmt, Method> fact, Stmt returnSite) {
		Delta<Field> delta = accessPath.getDeltaTo(incEdge.getCalleeSourceFact().getAccessPath());
		ReturnSiteResolver<Field, Fact, Stmt, Method> returnSiteResolver = incEdge.getCallerAnalyzer().returnSiteResolvers.getOrCreate(
				new FactAtStatement<Fact, Stmt>(fact.getFact(), returnSite));
		returnSiteResolver.addIncoming(fact, incEdge.getCalleeSourceFact().getResolver(), delta);
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

		private WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt;
		private EdgeFunction<Value> edgeFunction;

		public Job(WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt, EdgeFunction<Value> edgeFunction) {
			this.factAtStmt = factAtStmt;
			this.edgeFunction = edgeFunction;
		}

		@Override
		public void run() {
			if (context.icfg.isCallStmt(factAtStmt.getStatement())) {
				processCall(factAtStmt, edgeFunction);
			} else {
				if (context.icfg.isExitStmt(factAtStmt.getStatement())) {
					processExit(factAtStmt, edgeFunction);
				}
				if (!context.icfg.getSuccsOf(factAtStmt.getStatement()).isEmpty()) {
					processNormalFlow(factAtStmt, edgeFunction);
				}
			}
		}
	}

	public CallEdgeResolver<Fact, Stmt, Method, Value> getCallEdgeResolver() {
		return callEdgeResolver;
	}
	
}
