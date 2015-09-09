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
import heros.fieldsens.structs.AccessPathAndResolver;
import heros.solver.Pair;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class Resolver<Field, Fact, Stmt, Method> {

	private boolean recursionLock = false;
	private Resolver<Field, Fact, Stmt, Method> parent;
	private Set<AccessPathAndResolver<Field, Fact, Stmt, Method>> interest = Sets.newHashSet();
	private List<InterestCallback<Field, Fact, Stmt, Method>> interestCallbacks = Lists.newLinkedList();
	protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer;
	private boolean canBeResolvedEmpty = false;
	
	public Resolver(Resolver<Field, Fact, Stmt, Method> parent, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer) {
		this.parent = parent;
		this.analyzer = analyzer;
	}
	
	public PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer() {
		return analyzer;
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
//		if(parent != null)
//			parent.lock();
	}
	
	protected void unlock() {
		recursionLock = false;
//		if(parent != null)
//			parent.unlock();
	}

	public abstract void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback);
	
	public void interest(AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
		if(!interest.add(accPathResolver))
			return;

		log("Interest given by: "+accPathResolver);
		for(InterestCallback<Field, Fact, Stmt, Method> callback : Lists.newLinkedList(interestCallbacks)) {
			callback.interest(accPathResolver.getLast().resolver.analyzer, accPathResolver);
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
			for(AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver : Lists.newLinkedList(interest))
				callback.interest(accPathResolver.getLast().resolver.analyzer, accPathResolver);
		}
		interestCallbacks.add(callback);

		if(canBeResolvedEmpty)
			callback.canBeResolvedEmpty();
	}
	
	protected abstract void log(String message);
	
}
