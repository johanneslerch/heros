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
package heros.fieldsens;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.AccessPath.PrefixTestResult;
import heros.fieldsens.FlowFunction.ConstrainedFact;
import heros.fieldsens.structs.AccessPathAndResolver;
import heros.fieldsens.structs.FactAtStatement;
import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;
import heros.utilities.DefaultValueMap;

import java.io.FileWriter;
import java.io.IOException;
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
	private final AccessPath<Field> accessPath;
	private Map<WrappedFactAtStatement<Field,Fact, Stmt, Method>, WrappedFactAtStatement<Field,Fact, Stmt, Method>> reachableStatements = Maps.newHashMap();
	private List<WrappedFactAtStatement<Field, Fact, Stmt, Method>> summaries = Lists.newLinkedList();
	private Context<Field, Fact, Stmt, Method> context;
	private Method method;
	private DefaultValueMap<FactAtStatement<Fact, Stmt>, ReturnSiteHandling<Field, Fact, Stmt, Method>> returnSiteResolvers = new DefaultValueMap<FactAtStatement<Fact, Stmt>, ReturnSiteHandling<Field,Fact,Stmt,Method>>() {
		@Override
		protected ReturnSiteHandling<Field, Fact, Stmt, Method> createItem(FactAtStatement<Fact, Stmt> key) {
			assert context.icfg.getMethodOf(key.stmt).equals(method);
			return new ReturnSiteHandling<Field, Fact, Stmt, Method>(PerAccessPathMethodAnalyzer.this, key.fact, key.stmt, debugger, getLogger());
		}
	};
	private DefaultValueMap<FactAtStatement<Fact, Stmt>, ControlFlowJoinResolver<Field, Fact, Stmt, Method>> ctrFlowJoinResolvers = new DefaultValueMap<FactAtStatement<Fact, Stmt>, ControlFlowJoinResolver<Field,Fact,Stmt,Method>>() {
		@Override
		protected ControlFlowJoinResolver<Field, Fact, Stmt, Method> createItem(FactAtStatement<Fact, Stmt> key) {
			assert context.icfg.getMethodOf(key.stmt).equals(method);
			return new ControlFlowJoinResolver<Field, Fact, Stmt, Method>(context.factHandler, key.stmt, key.fact, debugger, getLogger(), sourceFact, method);
		}
	};
	private DefaultValueMap<FactAtStatement<Fact, Stmt>, CallSiteResolver<Field, Fact, Stmt, Method>> callSiteResolvers = new DefaultValueMap<FactAtStatement<Fact, Stmt>, CallSiteResolver<Field,Fact,Stmt,Method>>() {
		@Override
		protected CallSiteResolver<Field, Fact, Stmt, Method> createItem(FactAtStatement<Fact, Stmt> key) {
			assert context.icfg.getMethodOf(key.stmt).equals(method);
			return new CallSiteResolver<Field, Fact, Stmt, Method>(context.factHandler, key.stmt, debugger, getLogger());
		}
	};
	private CallEdgeResolver<Field, Fact, Stmt, Method> callEdgeResolver;
	private PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> parent;
	private Debugger<Field, Fact, Stmt, Method> debugger;
	private Delta<Field> repeatedDelta;
	private PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> zeroVersion;

	public PerAccessPathMethodAnalyzer(Method method, Fact sourceFact, Context<Field, Fact, Stmt, Method> context, Debugger<Field, Fact, Stmt, Method> debugger) {
		this(method, sourceFact, context, debugger, new AccessPath<Field>(), null, null);
		this.callEdgeResolver = isZeroSource() ? new ZeroCallEdgeResolver<Field, Fact, Stmt, Method>(this, context.zeroHandler, debugger) : new CallEdgeResolver<Field, Fact, Stmt, Method>(this, debugger);
	}
	
	private PerAccessPathMethodAnalyzer(Method method, Fact sourceFact, Context<Field, Fact, Stmt, Method> context, 
			Debugger<Field, Fact, Stmt, Method> debugger, AccessPath<Field> accPath, 
			Delta<Field> repeatedDelta,
			PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> parent) {
		this.debugger = debugger;
		this.repeatedDelta = repeatedDelta;
		if(method == null)
			throw new IllegalArgumentException("Method must be not null");
		this.parent = parent;
		this.method = method;
		this.sourceFact = sourceFact;
		this.accessPath = accPath;
		this.context = context;
		if(parent != null) {
			this.ctrFlowJoinResolvers = parent.ctrFlowJoinResolvers;
			this.callSiteResolvers = parent.callSiteResolvers;
		}
	}
	
	Context<Field, Fact, Stmt, Method> getContext() {
		return context;
	}
	
	PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getParent() {
		return parent;
	}
	
	public PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> createWithZeroCallEdgeResolver() {
		if(callEdgeResolver instanceof ZeroCallEdgeResolver)
			return this;
		
		if(zeroVersion == null) {
			if(accessPath.getExclusions().isEmpty()) {
				zeroVersion = new PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>(
						method, sourceFact, context, debugger, accessPath, null, this);
				zeroVersion.callEdgeResolver = new ZeroCallEdgeResolver<Field, Fact, Stmt, Method>(zeroVersion, context.zeroHandler, debugger);
			}
			else {
				zeroVersion = parent.createWithZeroCallEdgeResolver();
			}
		}
		return zeroVersion;
	}
	
	PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> createWithAccessPath(AccessPath<Field> accPath) {
		assert !(callEdgeResolver instanceof ZeroCallEdgeResolver);
		
		PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> result = new PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>(
				method, sourceFact, context, debugger, accPath, null, this);
		result.callEdgeResolver = new CallEdgeResolver<Field, Fact, Stmt, Method>(result, debugger, callEdgeResolver);
		return result;
	}
	
	WrappedFact<Field, Fact, Stmt, Method> wrappedSource() {
		return new WrappedFact<Field, Fact, Stmt, Method>(sourceFact, new AccessPathAndResolver<Field, Fact, Stmt, Method>(this, accessPath, callEdgeResolver));
	}
	
	public AccessPath<Field> getAccessPath() {
		return accessPath;
	}

	private boolean isBootStrapped() {
		return callEdgeResolver.hasIncomingEdges() || !accessPath.isEmpty() || repeatedDelta != null;
	}

	private void bootstrapAtMethodStartPoints() {
		callEdgeResolver.interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(this, AccessPath.<Field>empty(), callEdgeResolver));
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
		assert factAtStmt.getAccessPathAndResolver().getAnalyzer().method.equals(method);
		assert context.icfg.getMethodOf(factAtStmt.getStatement()).equals(method);
		
		if(!factAtStmt.getAccessPathAndResolver().accessPath.getExclusions().isEmpty() ||
				!((ResolverTemplate)factAtStmt.getAccessPathAndResolver().resolver).resolvedAccessPath.getExclusions().isEmpty()) {
			System.out.println("Edge to "+factAtStmt);
		}
		
		if(factAtStmt.getAccessPathAndResolver().resolver.toString().contains("ZERO-FIELD") && !factAtStmt.getAccessPathAndResolver().accessPath.toString().contains("ZERO-FIELD")) {
			System.out.println(factAtStmt);
		}
		
		if (reachableStatements.containsKey(factAtStmt)) {
			log("Merging "+factAtStmt);
			context.factHandler.merge(reachableStatements.get(factAtStmt).getWrappedFact().getFact(), factAtStmt.getWrappedFact().getFact());
		} else {
			log("Edge to "+factAtStmt);
			reachableStatements.put(factAtStmt, factAtStmt);
			context.scheduler.schedule(new Job(factAtStmt));
			debugger.edgeTo(this, factAtStmt);
		}
	}

	void log(String message) {
		logger.trace(toString()+": "+message);
	}

	@Override
	public String toString() {
		String result = callEdgeResolver+"; "+sourceFact+accessPath;
		if(repeatedDelta != null)
			result+="."+repeatedDelta+"*";
		return result;
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
			FlowFunction<Field, Fact, Stmt, Method> flowFunction = context.flowFunctions.getCallFlowFunction(factAtStmt.getStatement(), calledMethod);
			Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts =  flowFunction.computeTargets(factAtStmt.getFact(),
					new AccessPathHandler<Field, Fact, Stmt, Method>(factAtStmt.getAccessPathAndResolver(), debugger));
			for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
				//TODO handle constraint
				MethodAnalyzer<Field, Fact, Stmt, Method> analyzer = context.getAnalyzer(calledMethod);
				analyzer.addIncomingEdge(new CallEdge<Field, Fact, Stmt, Method>(this,
						factAtStmt, targetFact.getFact()));
			}
		}
	}

	void processExit(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		log("New Summary: "+factAtStmt);
		if(!summaries.add(factAtStmt))
			throw new AssertionError();
		
		callEdgeResolver.applySummaries(factAtStmt);

		if(context.followReturnsPastSeeds && isZeroSource()) {
			Collection<Stmt> callSites = context.icfg.getCallersOf(method);
			for(Stmt callSite : callSites) {
				Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(callSite);
				for(Stmt returnSite : returnSites) {
					FlowFunction<Field, Fact, Stmt, Method> flowFunction = context.flowFunctions.getReturnFlowFunction(callSite, method, factAtStmt.getStatement(), returnSite);
					Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts = flowFunction.computeTargets(factAtStmt.getFact(), 
							new AccessPathHandler<Field, Fact, Stmt, Method>(factAtStmt.getAccessPathAndResolver(), debugger));
					for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
						//TODO handle constraint
						context.getAnalyzer(context.icfg.getMethodOf(callSite)).addUnbalancedReturnFlow(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, targetFact.getFact()), callSite);
					}
				}
			}
			//in cases where there are no callers, the return statement would normally not be processed at all;
			//this might be undesirable if the flow function has a side effect such as registering a taint;
			//instead we thus call the return flow function will a null caller
			if(callSites.isEmpty()) {
				FlowFunction<Field, Fact, Stmt, Method> flowFunction = context.flowFunctions.getReturnFlowFunction(null, method, factAtStmt.getStatement(), null);
				flowFunction.computeTargets(factAtStmt.getFact(), new AccessPathHandler<Field, Fact, Stmt, Method>(factAtStmt.getAccessPathAndResolver(), debugger));
			}
		}
	}

	private void processCallToReturnEdge(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		if(isLoopStart(factAtStmt.getStatement())) {
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
			Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts = flowFunction.computeTargets(factAtStmt.getFact(), 
					new AccessPathHandler<Field, Fact, Stmt, Method>(factAtStmt.getAccessPathAndResolver(), debugger));
			for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
				//TODO handle constraint
				scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, targetFact.getFact()));
			}
		}
	}

	private void processNormalFlow(WrappedFactAtStatement<Field,Fact, Stmt, Method> factAtStmt) {
		if(isLoopStart(factAtStmt.getStatement())) {
			ctrFlowJoinResolvers.getOrCreate(factAtStmt.getAsFactAtStatement()).addIncoming(factAtStmt.getWrappedFact());
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

	private void processNormalNonJoiningFlow(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		final List<Stmt> successors = context.icfg.getSuccsOf(factAtStmt.getStatement());
		FlowFunction<Field, Fact, Stmt, Method> flowFunction = context.flowFunctions.getNormalFlowFunction(factAtStmt.getStatement());
		Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts = flowFunction.computeTargets(factAtStmt.getFact(), 
				new AccessPathHandler<Field, Fact, Stmt, Method>(factAtStmt.getAccessPathAndResolver(), debugger));
		for (final ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
			if(targetFact.getConstraint() == null)
				scheduleEdgeTo(successors, targetFact.getFact());
			else {
				targetFact.getFact().getAccessPathAndResolver().resolve(targetFact.getConstraint(), new InterestCallback<Field, Fact, Stmt, Method>() {
					
					@Override
					public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
							AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
						if(isZeroSource())
							analyzer = PerAccessPathMethodAnalyzer.this;
						analyzer.scheduleEdgeTo(successors, new WrappedFact<Field, Fact, Stmt, Method>(
								targetFact.getFact().getFact(), accPathResolver.withAccessPath(
										targetFact.getFact().getAccessPathAndResolver().accessPath.append(accPathResolver.accessPath))));
					}

					@Override
					public void canBeResolvedEmpty(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer) {
						if(analyzer.equals(PerAccessPathMethodAnalyzer.this))
							callEdgeResolver.resolve(targetFact.getConstraint(), this);
					}
				});
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
			Set<ConstrainedFact<Field, Fact, Stmt, Method>> targets = flowFunction.computeTargets(exitFact.getFact(), 
					new AccessPathHandler<Field, Fact, Stmt, Method>(exitFact.getAccessPathAndResolver(), debugger));
			for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targets) {
				context.factHandler.restoreCallingContext(targetFact.getFact().getFact(), incEdge.getCallerCallSiteFact().getFact());
				//TODO handle constraint
				scheduleReturnEdge(incEdge, targetFact.getFact(), returnSite);
			}
		}
	}

	public void scheduleUnbalancedReturnEdgeTo(WrappedFactAtStatement<Field, Fact, Stmt, Method> fact) {
		ReturnSiteHandling<Field, Fact, Stmt, Method> resolver = returnSiteResolvers.getOrCreate(fact.getAsFactAtStatement());
		resolver.addIncomingEdge(fact.getAccessPathAndResolver(),
				new AccessPathAndResolver<Field, Fact, Stmt, Method>(this, AccessPath.<Field>empty(), callEdgeResolver), null);
	}
	
	private void scheduleReturnEdge(CallEdge<Field, Fact, Stmt, Method> callEdge, WrappedFact<Field, Fact, Stmt, Method> fact, Stmt returnSite) {
		AccessPath<Field> remainingAccPath = accessPath.getDeltaToAsAccessPath(callEdge.getCalleeSourceFact().getAccessPathAndResolver().accessPath);
		ReturnSiteHandling<Field, Fact, Stmt, Method> resolver = callEdge.getCallerAnalyzer().returnSiteResolvers.getOrCreate(new FactAtStatement<Fact, Stmt>(fact.getFact(), returnSite));
		resolver.addIncomingEdge(fact.getAccessPathAndResolver(), 
				callEdge.getCalleeSourceFact().getAccessPathAndResolver().withAccessPath(remainingAccPath), null);
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
			debugger.newJob(PerAccessPathMethodAnalyzer.this, factAtStmt);
		}

		@Override
		public void run() {
			debugger.jobStarted(PerAccessPathMethodAnalyzer.this, factAtStmt);
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
			debugger.jobFinished(PerAccessPathMethodAnalyzer.this, factAtStmt);
		}
		
		@Override
		public String toString() {
			return "Job: "+factAtStmt;
		}
	}

	public CallEdgeResolver<Field, Fact, Stmt, Method> getCallEdgeResolver() {
		return callEdgeResolver;
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
	
//	public void debugReachables() {
//		JsonDocument root = new JsonDocument();
//		
//		for(WrappedFactAtStatement<Field, Fact, Stmt, Method> fact : reachableStatements.keySet()) {
//			JsonDocument doc = root.doc(fact.getStatement().toString()).doc(fact.getFact().toString()).doc(fact.getResolver().toString()).doc(String.valueOf(fact.hashCode()));
//			doc.keyValue("fact", String.valueOf(fact.getFact().hashCode()));
//			doc.keyValue("resolver", String.valueOf(fact.getResolver().hashCode()));
//			doc.keyValue("resolver-analyzer", String.valueOf(fact.getResolver().analyzer.hashCode()));
//			doc.keyValue("resolver-class", String.valueOf(fact.getResolver().getClass().toString()));
//		}
//		try {
//			FileWriter writer = new FileWriter("debug/reachables.json");
//			StringBuilder builder = new StringBuilder();
//			builder.append("var root=");
//			root.write(builder, 0);
//			writer.write(builder.toString());
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
//	public void debugInterest() {
//		JsonDocument root = new JsonDocument();
//		
//		List<PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> worklist = Lists.newLinkedList();
//		worklist.add(this);
//		Set<PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> visited = Sets.newHashSet();
//		
//		while(!worklist.isEmpty()) {
//			PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> current = worklist.remove(0);
//			if(!visited.add(current))
//				continue;
//			
//			JsonDocument currentMethodDoc = root.doc(current.method.toString()+ "___"+current.sourceFact);
//			JsonDocument currentDoc = currentMethodDoc.doc("accPath").doc("_"+current.accessPath.toString());
//			
//			for(CallEdge<Field, Fact, Stmt, Method> incEdge : current.getCallEdgeResolver().incomingEdges) {
//				currentDoc.doc("incoming").doc(incEdge.getCallerAnalyzer().method+"___"+incEdge.getCallerAnalyzer().sourceFact).doc("_"+incEdge.getCallerAnalyzer().accessPath.toString());
//				worklist.add(incEdge.getCallerAnalyzer());
//			}
//		}
//		
//		try {
//			FileWriter writer = new FileWriter("debug/incoming.json");
//			StringBuilder builder = new StringBuilder();
//			builder.append("var root=");
//			root.write(builder, 0);
//			writer.write(builder.toString());
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public void debugNestings() {
//		PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> current = this;
//		while(current.parent != null)
//			current = current.parent;
//		
//		JsonDocument root = new JsonDocument();
//		debugNestings(current, root);
//		
//		try {
//			FileWriter writer = new FileWriter("debug/nestings.json");
//			StringBuilder builder = new StringBuilder();
//			builder.append("var root=");
//			root.write(builder, 0);
//			writer.write(builder.toString());
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void debugNestings(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> current, JsonDocument parentDoc) {
//		JsonDocument currentDoc = parentDoc.doc(current.accessPath.toString());
//		for(ResolverTemplate<Field, Fact, Stmt, Method, CallEdge<Field, Fact, Stmt, Method>> nestedAnalyzer : current.getCallEdgeResolver().nestedResolvers.values()) {
//			debugNestings(nestedAnalyzer.analyzer, currentDoc);
//		}
//	}
}
