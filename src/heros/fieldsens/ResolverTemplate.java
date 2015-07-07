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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.AccessPath.PrefixTestResult;
import heros.fieldsens.FlowFunction.Constraint;
import heros.fieldsens.structs.DeltaConstraint;


public abstract class ResolverTemplate<Field, Fact, Stmt, Method, Incoming>  extends Resolver<Field, Fact, Stmt, Method> {

	private Set<String> recursionLock = Sets.newHashSet();
	protected Set<Incoming> guaranteedIncomingEdges = Sets.newHashSet();
	private Set<Incoming> potentialIncomingEdges = Sets.newHashSet();
	private ResolverTemplate<Field, Fact, Stmt, Method, Incoming> parent;
	private Map<AccessPath<Field>, ResolverTemplate<Field, Fact, Stmt, Method, Incoming>> nestedResolvers = Maps.newHashMap();

	public ResolverTemplate(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, 
			ResolverTemplate<Field, Fact, Stmt, Method, Incoming> parent) {
		super(analyzer);
		this.parent = parent;
	}
	
	protected boolean isLocked(String key) {
		if(recursionLock.contains(key))
			return true;
		if(parent == null)
			return false;
		return parent.isLocked(key);
	}

	protected void lock(String lockKey) {
		if(!recursionLock.add(lockKey))
			throw new IllegalStateException();
	}
	
	protected void unlock(String lockKey) {
		if(!recursionLock.remove(lockKey))
			throw new IllegalStateException();
	}
	
	protected abstract AccessPath<Field> getResolvedAccessPath();
	
	protected abstract AccessPath<Field> getAccessPathOf(Incoming inc);
	
	public void addIncoming(Incoming inc) {
		if(getResolvedAccessPath().isPrefixOf(getAccessPathOf(inc)) == PrefixTestResult.GUARANTEED_PREFIX) {
			log("Incoming Edge: "+inc);
			if(!guaranteedIncomingEdges.add(inc))
				return;
			
			interest();
			
			for(ResolverTemplate<Field, Fact, Stmt, Method, Incoming> nestedResolver : Lists.newLinkedList(nestedResolvers.values())) {
				nestedResolver.addIncoming(inc);
			}
			
			processIncomingGuaranteedPrefix(inc);
		}
		else if(getAccessPathOf(inc).isPrefixOf(getResolvedAccessPath()).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
			Delta<Field> delta = getAccessPathOf(inc).getDeltaTo(getResolvedAccessPath());
			resolvePotentialIncoming(inc, new DeltaConstraint<Field>(delta));
		}
	}
	
	protected abstract void resolvePotentialIncoming(Incoming inc, DeltaConstraint<Field> delta);

	protected void addPotentialIncomingEdge(Incoming inc) {
		if(!potentialIncomingEdges.add(inc) || isLocked("addPotentialIncomingEdge"))
			return;
		lock("addPotentialIncomingEdge");
		Delta<Field> delta = getAccessPathOf(inc).getDeltaTo(getResolvedAccessPath());
		for(InterestCallback<Field, Fact, Stmt, Method> callback : Lists.newLinkedList(interestCallbacks)) {
			delegate(inc, new DeltaConstraint<Field>(delta), callback);
		}
		unlock("addPotentialIncomingEdge");
	}
	
	@Override
	protected void registerCallback(InterestCallback<Field, Fact, Stmt, Method> callback) {
		if(isLocked("registerCallback"))
			return;
		lock("registerCallback");
		super.registerCallback(callback);
		
		for(Incoming inc : Lists.newLinkedList(potentialIncomingEdges)) {
			Delta<Field> delta = getAccessPathOf(inc).getDeltaTo(getResolvedAccessPath());
			delegate(inc, new DeltaConstraint<Field>(delta), callback);
		}
		unlock("registerCallback");
	}
	
	protected abstract void delegate(Incoming inc, DeltaConstraint<Field> deltaConstraint, InterestCallback<Field, Fact, Stmt, Method> callback);

	protected abstract void processIncomingGuaranteedPrefix(Incoming inc);
	
	@Override
	public void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback) {
		log("Resolve: "+constraint);
		if(constraint.canBeAppliedTo(getResolvedAccessPath())) {
			AccessPath<Field> newAccPath = constraint.applyToAccessPath(getResolvedAccessPath());
			ResolverTemplate<Field,Fact,Stmt,Method,Incoming> nestedResolver = getOrCreateNestedResolver(newAccPath);
			assert nestedResolver.getResolvedAccessPath().equals(constraint.applyToAccessPath(getResolvedAccessPath()));
			nestedResolver.registerCallback(callback);
		}
	}

	protected ResolverTemplate<Field, Fact, Stmt, Method, Incoming> getOrCreateNestedResolver(AccessPath<Field> newAccPath) {
		if(getResolvedAccessPath().equals(newAccPath))
			return this;
		
		if(!nestedResolvers.containsKey(newAccPath)) {
			assert getResolvedAccessPath().getDeltaTo(newAccPath).accesses.length <= 1;
			ResolverTemplate<Field,Fact,Stmt,Method,Incoming> nestedResolver = createNestedResolver(newAccPath);
			nestedResolvers.put(newAccPath, nestedResolver);
			
			for(Incoming inc : guaranteedIncomingEdges) {
				nestedResolver.addIncoming(inc);
			}
		}
		return nestedResolvers.get(newAccPath);
	}
	
	protected abstract ResolverTemplate<Field, Fact, Stmt, Method, Incoming> createNestedResolver(AccessPath<Field> newAccPath);
}