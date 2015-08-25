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

import heros.ide.edgefunc.EdgeFunction;
import heros.solver.Pair;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public abstract class Resolver<Fact, Stmt, Method, Value> {

	private boolean recursionLock = false;
	private Resolver<Fact, Stmt, Method, Value> parent;
	private Map<Resolver<Fact, Stmt, Method, Value>, EdgeFunction<Value>> resolvedUnbalanced = Maps.newHashMap();
	private List<InterestCallback<Fact, Stmt, Method, Value>> interestCallbacks = Lists.newLinkedList();
	protected PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer;
	private Set<EdgeFunction<Value>> balancedFunctions = Sets.newHashSet();
	
	public Resolver(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Resolver<Fact, Stmt, Method, Value> parent) {
		this.analyzer = analyzer;
		this.parent = parent;
	}

	public abstract void resolve(EdgeFunction<Value> edgeFunction, InterestCallback<Fact, Stmt, Method, Value> callback);

	public abstract EdgeFunction<Value> getResolvedFunction();
	
	protected boolean isLocked() {
		if(recursionLock)
			return true;
		if(parent == null)
			return false;
		return parent.isLocked();
	}

	protected void lock() {
		recursionLock = true;
		if(parent != null)
			parent.lock();
	}
	
	protected void unlock() {
		recursionLock = false;
		if(parent != null)
			parent.unlock();
	}
	
	private boolean isParentOf(Resolver<Fact, Stmt, Method, Value> resolver) {
		if(getClass() != resolver.getClass())
			return false;
		if(resolver.parent == this)
			return true;
		else if(resolver.parent != null)
			return isParentOf(resolver.parent);
		else
			return false;
	}
	
	public void resolvedUnbalanced(EdgeFunction<Value> edgeFunction, Resolver<Fact, Stmt, Method, Value> resolver) {
		if(resolvedUnbalanced.containsKey(resolver)) {
			return;
		}
				
		for(Resolver<Fact, Stmt, Method, Value> candidate : resolvedUnbalanced.keySet()) {
			if(candidate.isParentOf(resolver))
				return;
		}
		resolvedUnbalanced.put(resolver, edgeFunction);
		
		log("Interest given by EdgeFunction: "+edgeFunction);
		for(InterestCallback<Fact, Stmt, Method, Value> callback : Lists.newLinkedList(interestCallbacks)) {
			callback.interest(analyzer, resolver, edgeFunction);
		}
	}
	
	public boolean isResolvedUnbalanced() {
		return !resolvedUnbalanced.isEmpty();
	}
	
	protected void continueBalancedTraversal(EdgeFunction<Value> edgeFunction) {
//		edgeFunction = edgeFunction.composeWith(parent.getResolvedFunction());
		
		if(balancedFunctions.add(edgeFunction)) {
			for(InterestCallback<Fact, Stmt, Method, Value> callback : Lists.newLinkedList(interestCallbacks)) {
				callback.continueBalancedTraversal(edgeFunction);
			}
		}
	}

	protected void registerCallback(InterestCallback<Fact, Stmt, Method, Value> callback) {
		for(Entry<Resolver<Fact, Stmt, Method, Value>, EdgeFunction<Value>> resolved : Lists.newLinkedList(resolvedUnbalanced.entrySet())) {
			callback.interest(analyzer, resolved.getKey(), resolved.getValue());
		}
		log("Callback registered");
		interestCallbacks.add(callback);

		if(!balancedFunctions.isEmpty()) {
			for(EdgeFunction<Value> edgeFunction : Lists.newLinkedList(balancedFunctions)) {
				callback.continueBalancedTraversal(edgeFunction);
			}
		}
	}
	
	protected abstract void log(String message);
	
}
