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

import heros.ide.edgefunc.ChainableEdgeFunction;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.edgefunc.EdgeIdentity;
import heros.ide.structs.FactAtStatement;
import heros.ide.structs.FactEdgeFnResolverStatementTuple;
import heros.ide.structs.WrappedFact;
import heros.ide.structs.WrappedFactAtStatement;
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

class PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> {

	private static final Logger logger = LoggerFactory.getLogger(PerAccessPathMethodAnalyzer.class);
	private Fact sourceFact;
	private Map<WrappedFactAtStatement<Fact, Stmt, Method, Value>, FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value>> reachableStatements = Maps.newHashMap();
	private List<FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value>> summaries = Lists.newLinkedList();
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
			this.callEdgeResolver = isZeroSource() ? new ZeroCallEdgeResolver<Fact, Stmt, Method, Value>(this) : new CallEdgeResolver<Fact, Stmt, Method, Value>(this);
		}
		else {
			this.callEdgeResolver = isZeroSource() ? parent.callEdgeResolver : new CallEdgeResolver<Fact, Stmt, Method, Value>(this, parent.callEdgeResolver);
		}
		log("initialized");
	}
	
	public EdgeFunction<Value> getConstraint() {
		return constraint;
	}
	
	public PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> createWithConstraint(EdgeFunction<Value> constraint) {
		if(this.constraint instanceof ChainableEdgeFunction && constraint instanceof ChainableEdgeFunction) {
			assert ((ChainableEdgeFunction) this.constraint).depth()+1 >= ((ChainableEdgeFunction) constraint).depth();
		}
		return new PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value>(method, sourceFact, context, constraint, this);
	}
	
	private boolean isBootStrapped() {
		return callEdgeResolver.hasIncomingEdges() || !(constraint instanceof EdgeIdentity);
	}

	private void bootstrapAtMethodStartPoints(EdgeFunction<Value> edgeFunction) {
		callEdgeResolver.resolvedUnbalanced(EdgeIdentity.<Value>v(), callEdgeResolver);
		for(Stmt startPoint : context.icfg.getStartPointsOf(method)) {
			WrappedFactAtStatement<Fact, Stmt, Method, Value> target = new WrappedFactAtStatement<Fact, Stmt, Method, Value>(startPoint, wrappedSource());
			if(!reachableStatements.containsKey(target))
				scheduleEdgeTo(target.withEdgeFunction(EdgeIdentity.<Value>v()));
		}
	}
	
	WrappedFact<Fact, Stmt, Method, Value> wrappedSource() {
		return new WrappedFact<Fact, Stmt, Method, Value>(sourceFact, callEdgeResolver);
	}

	public void addInitialSeed(Stmt stmt, EdgeFunction<Value> edgeFunction) {
		scheduleEdgeTo(new FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value>(sourceFact, edgeFunction, callEdgeResolver, stmt));
	}

	void scheduleEdgeTo(FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factAtStmt) {
		assert context.icfg.getMethodOf(factAtStmt.getStatement()).equals(method);
		WrappedFactAtStatement<Fact, Stmt, Method, Value> withoutEdgeFunction = factAtStmt.getWithoutEdgeFunction();
		if (reachableStatements.containsKey(withoutEdgeFunction)) {
			log("Merging "+factAtStmt);
			FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> prevFact = reachableStatements.get(withoutEdgeFunction);
			context.factHandler.merge(prevFact.getFact(), factAtStmt.getFact());
			EdgeFunction<Value> joinedFunction = prevFact.getEdgeFunction().joinWith(factAtStmt.getEdgeFunction());
			if(!joinedFunction.equals(prevFact.getEdgeFunction())) {
				log("Updated EdgeFunction at "+prevFact+": "+joinedFunction);
				FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> newFact = prevFact.copyWithEdgeFunction(joinedFunction);
				reachableStatements.put(withoutEdgeFunction, newFact);
				context.scheduler.schedule(new Job(newFact));
			}
		} else {
			log("Edge to "+factAtStmt);
			reachableStatements.put(withoutEdgeFunction, factAtStmt);
			context.scheduler.schedule(new Job(factAtStmt));
		}
	}

	void log(String message) {
		logger.trace("[{}; {}{}: "+message+"]", method, sourceFact, constraint);
	}

	@Override
	public String toString() {
		return method+"; "+sourceFact+constraint;
	}

	void processCall(final FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factAtStmt) {
		Collection<Method> calledMethods = context.icfg.getCalleesOfCallAt(factAtStmt.getStatement());
		for (final Method calledMethod : calledMethods) {
			new PropagationTemplate<Fact, Stmt, Method, Value>(this) {
				@Override
				protected void propagate(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer,
						Resolver<Fact, Stmt, Method, Value> resolver, Fact targetFact, EdgeFunction<Value> edgeFunction) {
					context.getAnalyzer(calledMethod).addIncomingEdge(new CallEdge<Fact, Stmt, Method, Value>(analyzer, factAtStmt.getFact(), 
							targetFact, edgeFunction, resolver, factAtStmt.getStatement()));
				}

				@Override
				protected FlowFunction<Fact, Value> getFlowFunction() {
					return context.flowFunctions.getCallFlowFunction(factAtStmt.getStatement(), calledMethod);
				}
			}.propagate(factAtStmt);
		}
	}

	void processExit(final FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factAtStmt) {
		log("New Summary: "+factAtStmt);
		if(!summaries.add(factAtStmt))
			throw new AssertionError();

		callEdgeResolver.applySummaries(factAtStmt);

		if(context.followReturnsPastSeeds && isZeroSource()) {
			Collection<Stmt> callSites = context.icfg.getCallersOf(method);
			for(final Stmt callSite : callSites) {
				Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(callSite);
				for(final Stmt returnSite : returnSites) {
					new PropagationTemplate<Fact, Stmt, Method, Value>(this) {
						@Override
						protected void propagate(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer,
								Resolver<Fact, Stmt, Method, Value> resolver, Fact targetFact, EdgeFunction<Value> edgeFunction) {
							context.getAnalyzer(context.icfg.getMethodOf(callSite)).addUnbalancedReturnFlow(
									new FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value>(targetFact, edgeFunction, resolver, returnSite), callSite);
						}

						@Override
						protected FlowFunction<Fact, Value> getFlowFunction() {
							return context.flowFunctions.getReturnFlowFunction(callSite, method, factAtStmt.getStatement(), returnSite);
						}
					}.propagate(factAtStmt);
				}
			}
			//in cases where there are no callers, the return statement would normally not be processed at all;
			//this might be undesirable if the flow function has a side effect such as registering a taint;
			//instead we thus call the return flow function with a null caller
			if(callSites.isEmpty()) {
				FlowFunction<Fact, Value> flowFunction = context.flowFunctions.getReturnFlowFunction(null, method, factAtStmt.getStatement(), null);
				flowFunction.computeTargets(factAtStmt.getFact());
			}
		}
	}
	
	private void processCallToReturnEdge(FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factAtStmt) {
		if(isLoopStart(factAtStmt.getStatement())) {
			ctrFlowJoinResolvers.getOrCreate(factAtStmt.getAsFactAtStatement()).addIncomingWithoutCheck(factAtStmt.withoutStatement());
		}
		else {
			processNonJoiningCallToReturnFlow(factAtStmt);
		}
	}

	private void processNonJoiningCallToReturnFlow(final FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factAtStmt) {
		Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(factAtStmt.getStatement());
		for(final Stmt returnSite : returnSites) {
			new PropagationTemplate<Fact, Stmt, Method, Value>(this) {
				@Override
				protected void propagate(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer,
						Resolver<Fact, Stmt, Method, Value> resolver, Fact targetFact, EdgeFunction<Value> edgeFunction) {
					analyzer.scheduleEdgeTo(new FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value>(targetFact, edgeFunction, resolver, returnSite));
				}

				@Override
				protected FlowFunction<Fact, Value> getFlowFunction() {
					return context.flowFunctions.getCallToReturnFlowFunction(factAtStmt.getStatement(), returnSite);
				}
			}.propagate(factAtStmt);
		}
	}

	private void processNormalFlow(FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factAtStmt) {
		if(isLoopStart(factAtStmt.getStatement())) {
			ctrFlowJoinResolvers.getOrCreate(factAtStmt.getAsFactAtStatement()).addIncomingWithoutCheck(factAtStmt.withoutStatement());
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

	void processFlowFromJoinStmt(FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factAtStmt) {
		if(context.icfg.isCallStmt(factAtStmt.getStatement()))
			processNonJoiningCallToReturnFlow(factAtStmt);
		else
			processNormalNonJoiningFlow(factAtStmt);
	}

	private void processNormalNonJoiningFlow(final FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factAtStmt) {
		final List<Stmt> successors = context.icfg.getSuccsOf(factAtStmt.getStatement());
		for(final Stmt successor : successors) {
			new PropagationTemplate<Fact, Stmt, Method, Value>(this) {
				@Override
				protected void propagate(PerAccessPathMethodAnalyzer<Fact,Stmt,Method,Value> analyzer, Resolver<Fact,Stmt,Method,Value> resolver, Fact targetFact, EdgeFunction<Value> edgeFunction) {
					analyzer.scheduleEdgeTo(new FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value>(targetFact, edgeFunction, resolver, successor));
				};

				@Override
				protected FlowFunction<Fact, Value> getFlowFunction() {
					return context.flowFunctions.getNormalFlowFunction(factAtStmt.getStatement(), successor);
				}
			}.propagate(factAtStmt);
		}
	}
	
	public void addIncomingEdgeWithoutCheck(CallEdge<Fact, Stmt, Method, Value> incEdge) {
		if(isBootStrapped()) {
			context.factHandler.merge(sourceFact, incEdge.getCalleeSourceFact());
		} else 
			bootstrapAtMethodStartPoints(incEdge.getEdgeFunctionAtCallee());
		callEdgeResolver.addIncomingWithoutCheck(incEdge);
	}

	void applySummary(final CallEdge<Fact, Stmt, Method, Value> incEdge, final FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> exitFact) {
		Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(incEdge.getCallSite());
		for(final Stmt returnSite : returnSites) {
			new PropagationTemplate<Fact, Stmt, Method, Value>(this) {
				@Override
				protected void propagate(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer,
						Resolver<Fact, Stmt, Method, Value> resolver, Fact targetFact, EdgeFunction<Value> edgeFunction) {
					context.factHandler.restoreCallingContext(targetFact, incEdge.getCallerCallSiteFact());
					scheduleReturnEdge(incEdge, new FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value>(targetFact, edgeFunction, resolver, returnSite));
				}

				@Override
				protected FlowFunction<Fact, Value> getFlowFunction() {
					return context.flowFunctions.getReturnFlowFunction(incEdge.getCallSite(), method, exitFact.getStatement(), returnSite);
				}
			}.propagate(exitFact);
		}
	}

	public void scheduleUnbalancedReturnEdgeTo(FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> target) {
		ReturnSiteResolver<Fact,Stmt,Method, Value> resolver = returnSiteResolvers.getOrCreate(target.getAsFactAtStatement());
		resolver.addIncomingWithoutCheck(target.withoutStatement(), null, EdgeIdentity.<Value>v());
	}
	
	private void scheduleReturnEdge(CallEdge<Fact, Stmt, Method, Value> incEdge, FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factAtStmt) {
		ReturnSiteResolver<Fact, Stmt, Method, Value> returnSiteResolver = incEdge.getCallerAnalyzer().returnSiteResolvers.getOrCreate(
				new FactAtStatement<Fact, Stmt>(factAtStmt.getFact(), factAtStmt.getStatement()));
		returnSiteResolver.addIncomingWithoutCheck(factAtStmt.withoutStatement(), incEdge.getResolverIntoCallee(), incEdge.getEdgeFunctionAtCallee());
	}

	void applySummaries(CallEdge<Fact, Stmt, Method, Value> incEdge) {
		for(FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> summary : summaries) {
			applySummary(incEdge, summary);
		}
	}
	
	public boolean isZeroSource() {
		return sourceFact.equals(context.zeroValue);
	}
	
	public CallEdgeResolver<Fact, Stmt, Method, Value> getCallEdgeResolver() {
		return callEdgeResolver;
	}

	private class Job implements Runnable {

		private FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factTuple;

		public Job(FactEdgeFnResolverStatementTuple<Fact, Stmt, Method, Value> factTuple) {
			this.factTuple = factTuple;
		}

		@Override
		public void run() {
			if (context.icfg.isCallStmt(factTuple.getStatement())) {
				processCall(factTuple);
				processCallToReturnEdge(factTuple);
			} else {
				if (context.icfg.isExitStmt(factTuple.getStatement())) {
					processExit(factTuple);
				}
				if (!context.icfg.getSuccsOf(factTuple.getStatement()).isEmpty()) {
					processNormalFlow(factTuple);
				}
			}
		}
	}
	
}
