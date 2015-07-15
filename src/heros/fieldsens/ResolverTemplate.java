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
import heros.fieldsens.FlowFunction.Constraint;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public abstract class ResolverTemplate<Field, Fact, Stmt, Method, Incoming>  extends Resolver<Field, Fact, Stmt, Method> {

	private boolean recursionLock = false;
	protected Set<Incoming> incomingEdges = Sets.newHashSet();
	private ResolverTemplate<Field, Fact, Stmt, Method, Incoming> parent;
	private Map<AccessPath<Field>, ResolverTemplate<Field, Fact, Stmt, Method, Incoming>> nestedResolvers = Maps.newHashMap();
	protected Debugger<Field, Fact, Stmt, Method> debugger;
	private Map<AccessPath<Field>, ResolverTemplate<Field, Fact, Stmt, Method, Incoming>> allResolversInExclHierarchy;
	protected AccessPath<Field> resolvedAccessPath;

	public ResolverTemplate(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, 
			AccessPath<Field> resolvedAccessPath,
			ResolverTemplate<Field, Fact, Stmt, Method, Incoming> parent, 
			Debugger<Field, Fact, Stmt, Method> debugger) {
		super(analyzer);
		this.resolvedAccessPath = resolvedAccessPath;
		this.parent = parent;
		this.debugger = debugger;
		if(parent == null || resolvedAccessPath.getExclusions().isEmpty()) {
			allResolversInExclHierarchy = Maps.newHashMap();
		}
		else {
			allResolversInExclHierarchy = parent.allResolversInExclHierarchy;
		}
		
		debugger.newResolver(analyzer, this);
	}
	
	protected boolean isLocked() {
		if(recursionLock)
			return true;
		if(parent == null)
			return false;
		return parent.isLocked();
	}

	protected void lock() {
		recursionLock = true;
	}
	
	protected void unlock() {
		recursionLock = false;
	}
	
	protected abstract AccessPath<Field> getAccessPathOf(Incoming inc);
	
	public void addIncoming(Incoming inc) {
		AccessPath<Field> incAccPath = getAccessPathOf(inc);
		if(resolvedAccessPath.isPrefixOf(incAccPath) == PrefixTestResult.GUARANTEED_PREFIX) {
			log("Incoming Edge: "+inc);
			if(!incomingEdges.add(inc))
				return;
			
			if(shouldSpecialize(incAccPath)) {
//				System.out.println(this+ ": specializing using the incoming access path: "+incAccPath);
				Delta<Field> delta = resolvedAccessPath.getDeltaTo(incAccPath).limitToFirstAccess();
				specialize(delta, getOrCreateNestedResolver(delta.applyTo(resolvedAccessPath)));
			}
			else
				interest(this);
			
			for(ResolverTemplate<Field, Fact, Stmt, Method, Incoming> nestedResolver : Lists.newLinkedList(nestedResolvers.values())) {
				nestedResolver.addIncoming(inc);
			}
			
			processIncomingGuaranteedPrefix(inc);
		}
		else if(incAccPath.isPrefixOf(resolvedAccessPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
			processIncomingPotentialPrefix(inc);
		}
	}

	protected boolean shouldSpecialize(AccessPath<Field> incAccPath) {
		return !resolvedAccessPath.getExclusions().isEmpty() &&
				incAccPath.getExclusions().isEmpty() &&
				parent != null &&
				allResolversInExclHierarchy.size() > 5;
	}

	protected abstract void processIncomingPotentialPrefix(Incoming inc);

	protected abstract void processIncomingGuaranteedPrefix(Incoming inc);
	
	@Override
	public void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback) {
		log("Resolve: "+constraint);
		if(constraint.canBeAppliedTo(resolvedAccessPath) && !isLocked()) {
			AccessPath<Field> newAccPath = constraint.applyToAccessPath(resolvedAccessPath);
			ResolverTemplate<Field,Fact,Stmt,Method,Incoming> nestedResolver = getOrCreateNestedResolver(newAccPath);
			assert nestedResolver.resolvedAccessPath.equals(constraint.applyToAccessPath(resolvedAccessPath));
			nestedResolver.registerCallback(callback);
		}
	}

	protected ResolverTemplate<Field, Fact, Stmt, Method, Incoming> getOrCreateNestedResolver(AccessPath<Field> newAccPath) {
		if(resolvedAccessPath.equals(newAccPath))
			return this;
		
		if(!nestedResolvers.containsKey(newAccPath)) {
			assert resolvedAccessPath.getDeltaTo(newAccPath).accesses.length <= 1;
			
			if(allResolversInExclHierarchy.containsKey(newAccPath)) {
				ResolverTemplate<Field, Fact, Stmt, Method, Incoming> nestedResolver = allResolversInExclHierarchy.get(newAccPath);
//				nestedResolvers.put(newAccPath, nestedResolver); //don't add, otherwise incoming edges will be forwarded twice
				return nestedResolver;
			}
			else {
				ResolverTemplate<Field, Fact, Stmt, Method, Incoming> nestedResolver = createNestedResolver(newAccPath);
				if(!resolvedAccessPath.getExclusions().isEmpty() || !newAccPath.getExclusions().isEmpty())
					allResolversInExclHierarchy.put(newAccPath, nestedResolver);
				nestedResolvers.put(newAccPath, nestedResolver);
				for(Incoming inc : incomingEdges) {
					nestedResolver.addIncoming(inc);
				}
				return nestedResolver;
			}
			
		}
		return nestedResolvers.get(newAccPath);
	}
	
	protected abstract ResolverTemplate<Field, Fact, Stmt, Method, Incoming> createNestedResolver(AccessPath<Field> newAccPath);
}