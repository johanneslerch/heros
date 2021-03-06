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

import heros.ide.edgefunc.AllTop;
import heros.ide.edgefunc.ChainableEdgeFunction;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.edgefunc.EdgeIdentity;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public abstract class ResolverTemplate<Fact, Stmt, Method, Value, Incoming>  extends Resolver<Fact, Stmt, Method, Value> {

	protected Set<Incoming> incomingEdges = Sets.newLinkedHashSet();
	private Map<EdgeFunction<Value>, ResolverTemplate<Fact, Stmt, Method, Value, Incoming>> nestedResolvers = Maps.newHashMap();
	private Map<EdgeFunction<Value>, ResolverTemplate<Fact, Stmt, Method, Value, Incoming>> allResolvers;

	public ResolverTemplate(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, 
			ResolverTemplate<Fact, Stmt, Method, Value, Incoming> parent) {
		super(analyzer, parent);
		if(parent == null) {
			this.allResolvers = Maps.newHashMap();
		}
		else {
			this.allResolvers = parent.allResolvers;
		}
	}
	
	protected abstract EdgeFunction<Value> getEdgeFunction(Incoming inc);
	
	protected void addIncomingWithoutCheck(Incoming inc) {
		log("Incoming Edge: "+inc+ " with EdgeFunction: "+getEdgeFunction(inc));
		if(!incomingEdges.add(inc))
			return;
		
		resolvedUnbalanced(EdgeIdentity.<Value>v(), this);
		
		for(ResolverTemplate<Fact, Stmt, Method, Value, Incoming> nestedResolver : Lists.newLinkedList(nestedResolvers.values())) {
			nestedResolver.addIncoming(inc);
		}
		
		processIncomingGuaranteedPrefix(inc);
	}
	
	public void addIncoming(Incoming inc) {
		if(getResolvedFunction().mayReturnTop()) {
			EdgeFunction<Value> composedFunction = getEdgeFunction(inc).composeWith(getResolvedFunction());
			if(composedFunction.mayReturnTop()) {
				if(composedFunction instanceof AllTop)
					return;
				
//				assert !isLocked();
				if(isLocked())
					return;
				lock();
				processIncomingPotentialPrefix(inc); //TODO: Improve performance by passing composedFunction here
				unlock();
				return;
			}
		}

		addIncomingWithoutCheck(inc);
	}

	protected abstract void processIncomingPotentialPrefix(Incoming inc);

	protected abstract void processIncomingGuaranteedPrefix(Incoming inc);
	
	@Override
	public void resolve(EdgeFunction<Value> constraint, InterestCallback<Fact, Stmt, Method, Value> callback) {
		if(!isLocked()) {
			//do not include already resolvedPath, it was propagated to the next edge and therefore will be included in the given constraint.
//			EdgeFunction<Value> composedFunction = getResolvedFunction().composeWith(constraint);
			log("Resolve: "+constraint);
			ResolverTemplate<Fact,Stmt,Method, Value,Incoming> nestedResolver = getOrCreateNestedResolver(constraint);
			nestedResolver.registerCallback(callback);
		}
	}

	protected ResolverTemplate<Fact, Stmt, Method, Value, Incoming> getOrCreateNestedResolver(EdgeFunction<Value> constraint) {
		if(getResolvedFunction().equals(constraint))
			return this;
		
		if(getResolvedFunction() instanceof ChainableEdgeFunction && constraint instanceof ChainableEdgeFunction) {
			assert ((ChainableEdgeFunction) getResolvedFunction()).depth()+1 >= ((ChainableEdgeFunction) constraint).depth();
		}
	
		if(allResolvers.containsKey(constraint)) {
			return allResolvers.get(constraint);
		}
		else {
			if(!nestedResolvers.containsKey(constraint)) {
				ResolverTemplate<Fact,Stmt,Method,Value,Incoming> nestedResolver = createNestedResolver(constraint);
				nestedResolvers.put(constraint, nestedResolver);
				allResolvers.put(constraint, nestedResolver);
				
				for(Incoming inc : incomingEdges) {
					nestedResolver.addIncoming(inc);
				}
			}
			return nestedResolvers.get(constraint);
		}
	}
	
	protected abstract ResolverTemplate<Fact, Stmt, Method, Value, Incoming> createNestedResolver(EdgeFunction<Value> constraint);
}