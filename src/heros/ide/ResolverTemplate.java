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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public abstract class ResolverTemplate<Fact, Stmt, Method, Value, Incoming>  extends Resolver<Fact, Stmt, Method, Value> {

	private boolean recursionLock = false;
	protected Set<Incoming> incomingEdges = Sets.newHashSet();
	private ResolverTemplate<Fact, Stmt, Method, Value, Incoming> parent;
	private Map<EdgeFunction<Value>, ResolverTemplate<Fact, Stmt, Method, Value, Incoming>> nestedResolvers = Maps.newHashMap();

	public ResolverTemplate(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, 
			ResolverTemplate<Fact, Stmt, Method, Value, Incoming> parent) {
		super(analyzer);
		this.parent = parent;
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
	
	protected abstract EdgeFunction<Value> getResolvedFunction();
	
	protected abstract EdgeFunction<Value> getEdgeFunction(Incoming inc);
	
	public void addIncoming(Incoming inc) {
		EdgeFunction<Value> composedFunction = getEdgeFunction(inc).composeWith(getResolvedFunction());
		if(composedFunction.mayReturnTop()) {
			if(composedFunction instanceof AllTop)
				return;
			processIncomingPotentialPrefix(inc); //TODO: Improve performance by passing composedFunction here
		}
		else {
			log("Incoming Edge: "+inc+ " with EdgeFunction: "+getEdgeFunction(inc));
			if(!incomingEdges.add(inc))
				return;
			
			interest();
			
			for(ResolverTemplate<Fact, Stmt, Method, Value, Incoming> nestedResolver : nestedResolvers.values()) {
				nestedResolver.addIncoming(inc);
			}
			
			processIncomingGuaranteedPrefix(inc);
		}
	}

	protected abstract void processIncomingPotentialPrefix(Incoming inc);

	protected abstract void processIncomingGuaranteedPrefix(Incoming inc);
	
	@Override
	public void resolve(EdgeFunction<Value> constraint, InterestCallback<Fact, Stmt, Method, Value> callback) {
		log("Resolve: "+constraint);
		if(!isLocked()) {
			ResolverTemplate<Fact,Stmt,Method, Value,Incoming> nestedResolver = getOrCreateNestedResolver(constraint);
			nestedResolver.registerCallback(callback);
		}
	}

	protected ResolverTemplate<Fact, Stmt, Method, Value, Incoming> getOrCreateNestedResolver(EdgeFunction<Value> constraint) {
		if(getResolvedFunction().equals(constraint))
			return this;
		
		if(!nestedResolvers.containsKey(constraint)) {
			ResolverTemplate<Fact,Stmt,Method,Value,Incoming> nestedResolver = createNestedResolver(constraint);
			nestedResolvers.put(constraint, nestedResolver);
			
			for(Incoming inc : incomingEdges) {
				nestedResolver.addIncoming(inc);
			}
		}
		return nestedResolvers.get(constraint);
	}
	
	protected abstract ResolverTemplate<Fact, Stmt, Method, Value, Incoming> createNestedResolver(EdgeFunction<Value> constraint);
}