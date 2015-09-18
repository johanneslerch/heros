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

import heros.fieldsens.FlowFunction.Constraint;
import heros.fieldsens.structs.AccessPathAndResolver;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public abstract class Resolver<Field, Fact, Stmt, Method> {

	private boolean recursionLock = false;
	private Resolver<Field, Fact, Stmt, Method> parent;
	private Set<AccessPathAndResolver<Field, Fact, Stmt, Method>> interest = Sets.newHashSet();
	private List<InterestCallback<Field, Fact, Stmt, Method>> interestCallbacks = Lists.newLinkedList();
	private boolean canBeResolvedEmpty = false;
	
	public Resolver(Resolver<Field, Fact, Stmt, Method> parent) {
		this.parent = parent;
	}
	
	public boolean isParentOf(Resolver<Field, Fact, Stmt, Method> resolver) {
		if(resolver == this)
			return true;
		else if(resolver.parent != null)
			return isParentOf(resolver.parent);
		else
			return false;
	}
	
	protected boolean isLocked() {
		return recursionLock;
//		if(recursionLock)
//			return true;
//		if(parent == null)
//			return false;
//		return parent.isLocked();
	}

	protected void lock() {
		recursionLock = true;
	}
	
	protected void unlock() {
		recursionLock = false;
	}

	public abstract void registerTransitiveResolverCallback(TransitiveResolverCallback<Field, Fact, Stmt, Method> callback);
	
	public abstract void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback);
	
	public void interest(AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
		if(!interest.add(accPathResolver))
			return;

		log("Interest given by: "+accPathResolver);
		for(InterestCallback<Field, Fact, Stmt, Method> callback : Lists.newLinkedList(interestCallbacks)) {
			callback.interest(accPathResolver.getAnalyzer(), accPathResolver);
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
			for(AccessPathAndResolver<Field, Fact, Stmt, Method> entry : Lists.newLinkedList(interest))
				callback.interest(entry.getAnalyzer(), entry);
		}
		interestCallbacks.add(callback);

		if(canBeResolvedEmpty)
			callback.canBeResolvedEmpty();
	}
	
	protected abstract void log(String message);
	
}
