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
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.AccessPath.PrefixTestResult;
import heros.fieldsens.FlowFunction.Constraint;
import heros.fieldsens.structs.AccessPathAndResolver;


public abstract class ResolverTemplate<Field, Fact, Stmt, Method, Incoming>  extends Resolver<Field, Fact, Stmt, Method> {

	protected Multimap<Resolver<Field, Fact, Stmt, Method>, Incoming> incomingEdges = HashMultimap.create();
	private Map<AccessPath<Field>, ResolverTemplate<Field, Fact, Stmt, Method, Incoming>> nestedResolvers = Maps.newHashMap();
	private Map<AccessPath<Field>, ResolverTemplate<Field, Fact, Stmt, Method, Incoming>> allResolversInExclHierarchy;
	protected AccessPath<Field> resolvedAccessPath;
	protected Debugger<Field, Fact, Stmt, Method> debugger;

	public ResolverTemplate(AccessPath<Field> resolvedAccessPath,
			ResolverTemplate<Field, Fact, Stmt, Method, Incoming> parent, 
			Debugger<Field, Fact, Stmt, Method> debugger) {
		super(parent);
		this.resolvedAccessPath = resolvedAccessPath;
		this.debugger = debugger;
		if(parent == null || resolvedAccessPath.getExclusions().isEmpty()) {
			allResolversInExclHierarchy = Maps.newHashMap();
		}
		else {
			allResolversInExclHierarchy = parent.allResolversInExclHierarchy;
		}
//		debugger.newResolver(analyzer, this);
	}
	
	protected abstract AccessPath<Field> getAccessPathOf(Incoming inc);
	
	protected abstract PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer(Incoming inc); 
	
	public void addIncoming(Incoming inc, Resolver<Field, Fact, Stmt, Method> transitiveResolver) {
		if(resolvedAccessPath.isPrefixOf(getAccessPathOf(inc)) == PrefixTestResult.GUARANTEED_PREFIX) {
			if(transitiveResolver != null && incomingEdges.containsKey(transitiveResolver))
				return;
			
			if(!incomingEdges.put(transitiveResolver, inc))
				return;
			log("Incoming Edge: "+inc+" (transitive resolver: "+transitiveResolver+")");
					
			interestByIncoming(inc, transitiveResolver);
			
			for(ResolverTemplate<Field, Fact, Stmt, Method, Incoming> nestedResolver : Lists.newLinkedList(nestedResolvers.values())) {
				nestedResolver.addIncoming(inc, transitiveResolver);
			}
			
			processIncomingGuaranteedPrefix(inc);
		}
		else if(getAccessPathOf(inc).isPrefixOf(resolvedAccessPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
			processIncomingPotentialPrefix(inc);
		}
	}
	
	protected void interestByIncoming(Incoming inc, Resolver<Field, Fact, Stmt, Method> transitiveResolver) {
		if(getAccessPathOf(inc).equals(resolvedAccessPath) || resolvedAccessPath.getExclusions().size() < 1) {
			interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(getAnalyzer(inc), AccessPath.<Field>empty(), this), transitiveResolver);
		}
		else {
			AccessPath<Field> deltaTo = resolvedAccessPath.getDeltaToAsAccessPath(getAccessPathOf(inc));
			ResolverTemplate<Field,Fact,Stmt,Method,Incoming> nestedResolver = getOrCreateNestedResolver(getAccessPathOf(inc));
			interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(getAnalyzer(inc), deltaTo, nestedResolver), null);
		}
	}
	
	protected abstract void processIncomingPotentialPrefix(Incoming inc);

	protected abstract void processIncomingGuaranteedPrefix(Incoming inc);
	
	@Override
	public void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback) {
		if(!isLocked()) {
			log("Resolve: "+constraint);
			debugger.askedToResolve(this, constraint);
			if(constraint.canBeAppliedTo(resolvedAccessPath) && !isLocked()) {
				AccessPath<Field> newAccPath = constraint.applyToAccessPath(resolvedAccessPath);
				ResolverTemplate<Field,Fact,Stmt,Method,Incoming> nestedResolver = getOrCreateNestedResolver(newAccPath);
				if(nestedResolver == this)
					return;
				assert nestedResolver.resolvedAccessPath.equals(constraint.applyToAccessPath(resolvedAccessPath));
				nestedResolver.registerCallback(callback);
			}
		}
	}

	protected ResolverTemplate<Field, Fact, Stmt, Method, Incoming> getOrCreateNestedResolver(AccessPath<Field> newAccPath) {
		if(resolvedAccessPath.equals(newAccPath))
			return this;
		
		if(!nestedResolvers.containsKey(newAccPath)) {
			if(allResolversInExclHierarchy.containsKey(newAccPath)) {
				return allResolversInExclHierarchy.get(newAccPath);
			}
			else {
				ResolverTemplate<Field, Fact, Stmt, Method, Incoming> nestedResolver = createNestedResolver(newAccPath);
				if(!resolvedAccessPath.getExclusions().isEmpty() || !newAccPath.getExclusions().isEmpty())
					allResolversInExclHierarchy.put(newAccPath, nestedResolver);
				nestedResolvers.put(newAccPath, nestedResolver);
				for(Entry<Resolver<Field, Fact, Stmt, Method>, Incoming> inc : Lists.newLinkedList(incomingEdges.entries())) {
					nestedResolver.addIncoming(inc.getValue(), inc.getKey());
				}
				return nestedResolver;
			}
		}
		return nestedResolvers.get(newAccPath);
	}
	
	protected abstract ResolverTemplate<Field, Fact, Stmt, Method, Incoming> createNestedResolver(AccessPath<Field> newAccPath);
}