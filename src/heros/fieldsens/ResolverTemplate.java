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


public abstract class ResolverTemplate<Field, Fact, Stmt, Method, Incoming>  extends Resolver<Field, Fact, Stmt, Method> {

	private boolean recursionLock = false;
	protected Set<Incoming> incomingEdges = Sets.newHashSet();
	private ResolverTemplate<Field, Fact, Stmt, Method, Incoming> parent;
	private Map<AccessPath<Field>, ResolverTemplate<Field, Fact, Stmt, Method, Incoming>> nestedResolvers = Maps.newHashMap();
	protected Debugger<Field, Fact, Stmt, Method> debugger;
	private Map<AccessPath<Field>, ResolverTemplate<Field, Fact, Stmt, Method, Incoming>> allResolvers;

	public ResolverTemplate(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, 
			ResolverTemplate<Field, Fact, Stmt, Method, Incoming> parent, 
			Debugger<Field, Fact, Stmt, Method> debugger) {
		super(analyzer);
		this.parent = parent;
		this.debugger = debugger;
		if(parent == null) {
			allResolvers = Maps.newHashMap();
		}
		else {
			allResolvers = parent.allResolvers;
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
	
	protected abstract AccessPath<Field> getResolvedAccessPath();
	
	protected abstract AccessPath<Field> getAccessPathOf(Incoming inc);
	
	public void addIncoming(Incoming inc) {
		if(getResolvedAccessPath().isPrefixOf(getAccessPathOf(inc)) == PrefixTestResult.GUARANTEED_PREFIX) {
			log("Incoming Edge: "+inc);
			if(!incomingEdges.add(inc))
				return;
			
			interest(this);
			
			for(ResolverTemplate<Field, Fact, Stmt, Method, Incoming> nestedResolver : Lists.newLinkedList(nestedResolvers.values())) {
				nestedResolver.addIncoming(inc);
			}
			
			processIncomingGuaranteedPrefix(inc);
		}
		else if(getAccessPathOf(inc).isPrefixOf(getResolvedAccessPath()).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
			processIncomingPotentialPrefix(inc);
		}
	}

	protected abstract void processIncomingPotentialPrefix(Incoming inc);

	protected abstract void processIncomingGuaranteedPrefix(Incoming inc);
	
	@Override
	public void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback) {
		log("Resolve: "+constraint);
		if(constraint.canBeAppliedTo(getResolvedAccessPath()) && !isLocked()) {
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
			
			if(allResolvers.containsKey(newAccPath)) {
				ResolverTemplate<Field, Fact, Stmt, Method, Incoming> nestedResolver = allResolvers.get(newAccPath);
//				nestedResolvers.put(newAccPath, nestedResolver); //don't add, otherwise incoming edges will be forwarded twice
				return nestedResolver;
			}
			else {
				ResolverTemplate<Field, Fact, Stmt, Method, Incoming> nestedResolver = createNestedResolver(newAccPath);
				allResolvers.put(newAccPath, nestedResolver);
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