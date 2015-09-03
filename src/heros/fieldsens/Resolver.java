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
import heros.fieldsens.FlowFunction.Constraint;
import heros.solver.Pair;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class Resolver<Field, Fact, Stmt, Method> {

	private boolean recursionLock = false;
	private Resolver<Field, Fact, Stmt, Method> parent;
	private Set<Pair<Delta<Field>, Resolver<Field, Fact, Stmt, Method>>> interest = Sets.newHashSet();
	private List<InterestCallback<Field, Fact, Stmt, Method>> interestCallbacks = Lists.newLinkedList();
	protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer;
	private boolean canBeResolvedEmpty = false;
	
	public Resolver(Resolver<Field, Fact, Stmt, Method> parent, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer) {
		this.parent = parent;
		this.analyzer = analyzer;
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
		if(parent != null)
			parent.lock();
	}
	
	protected void unlock() {
		recursionLock = false;
		if(parent != null)
			parent.unlock();
	}

	public abstract void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback);
	
	public void interest(Delta<Field> delta, Resolver<Field, Fact, Stmt, Method> resolver) {
		Pair<Delta<Field>, Resolver<Field, Fact, Stmt, Method>> pair = new Pair<Delta<Field>, Resolver<Field, Fact, Stmt, Method>>(delta, resolver);
		if(!interest.add(pair))
			return;

		log("Interest given by delta: "+delta+" and resolver: "+resolver);
		for(InterestCallback<Field, Fact, Stmt, Method> callback : Lists.newLinkedList(interestCallbacks)) {
			callback.interest(resolver.analyzer, delta, resolver);
		}
	}
	
	protected void canBeResolvedEmpty() {
		if(canBeResolvedEmpty)
			return;
		
		canBeResolvedEmpty = true;
		for(InterestCallback<Field, Fact, Stmt, Method> callback : Lists.newLinkedList(interestCallbacks)) {
			callback.canBeResolvedEmpty();
		}
	}

	public boolean isInterestGiven() {
		return !interest.isEmpty();
	}

	protected void registerCallback(InterestCallback<Field, Fact, Stmt, Method> callback) {
		if(!interest.isEmpty()) {
			for(Pair<Delta<Field>, Resolver<Field, Fact, Stmt, Method>> pair : Lists.newLinkedList(interest))
				callback.interest(pair.getO2().analyzer, pair.getO1(), pair.getO2());
		}
		log("Callback registered");
		interestCallbacks.add(callback);

		if(canBeResolvedEmpty)
			callback.canBeResolvedEmpty();
	}
	
	protected abstract void log(String message);
	
}
