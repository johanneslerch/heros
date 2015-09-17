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

import java.util.Collection;
import java.util.List;
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
	private List<TransitiveResolverCallback<Field, Fact, Stmt, Method>> transitiveResolverCallbacks = Lists.newLinkedList();
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
	
	protected abstract void registerTransitiveResolverCallback(Incoming inc, TransitiveResolverCallback<Field, Fact, Stmt, Method> callback);
	
	@Override
	public void registerTransitiveResolverCallback(TransitiveResolverCallback<Field, Fact, Stmt, Method> callback) {
		transitiveResolverCallbacks.add(callback);
		for(Resolver<Field, Fact, Stmt, Method> transRes : Lists.newLinkedList(incomingEdges.keySet())) {
			if(transRes == null)
				callback.resolvedByIncomingAccessPath();
			else
				callback.resolvedBy(transRes);
		}
	}
	
	protected abstract boolean addSameTransitiveResolver();
	
	public void addIncoming(final Incoming inc) {
		AccessPath<Field> incAccPath = getAccessPathOf(inc);
		if(incAccPath.equals(resolvedAccessPath)) {
			registerTransitiveResolverCallback(inc, new TransitiveResolverCallback<Field, Fact, Stmt, Method>() {
				@Override
				public void resolvedByIncomingAccessPath() {
					addIncomingGuaranteedPrefix(inc, null);
				}
				
				@Override
				public void resolvedBy(Resolver<Field, Fact, Stmt, Method> resolver) {
					if(incomingEdges.containsKey(resolver))
						dismissByTransitiveResolver(inc, resolver);
					else
						addIncomingGuaranteedPrefix(inc, resolver);
				}
			});
		} else if(resolvedAccessPath.isPrefixOf(incAccPath) == PrefixTestResult.GUARANTEED_PREFIX) {
			addIncomingGuaranteedPrefix(inc, null);
		}
		else if(incAccPath.isPrefixOf(resolvedAccessPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
			processIncomingPotentialPrefix(inc);
		}
	}
	
	protected void dismissByTransitiveResolver(Incoming inc, Resolver<Field, Fact, Stmt, Method> resolver) {
		log("Dismissed Incoming Edge "+inc+" because we already saw the same transitive resolver: "+resolver);		
	}

	private void addIncomingGuaranteedPrefix(Incoming inc, Resolver<Field, Fact, Stmt, Method> transitiveResolver) {
		boolean isNewTransitiveResolver = incomingEdges.containsKey(transitiveResolver);
		
		if(!incomingEdges.put(transitiveResolver, inc))
			return;
		log("Incoming Edge: "+inc+ "(transitive resolver: "+transitiveResolver+")");
		
		interestByIncoming(inc);
		
		for(ResolverTemplate<Field, Fact, Stmt, Method, Incoming> nestedResolver : Lists.newLinkedList(nestedResolvers.values())) {
			nestedResolver.addIncoming(inc);
		}
		
		processIncomingGuaranteedPrefix(inc);
		
		if(isNewTransitiveResolver) {
			for(TransitiveResolverCallback<Field, Fact, Stmt, Method> callback : Lists.newLinkedList(transitiveResolverCallbacks)) {
				if(transitiveResolver == null)
					callback.resolvedByIncomingAccessPath();
				else
					callback.resolvedBy(transitiveResolver);
			}
		}
	}

	protected void interestByIncoming(Incoming inc) {
		if(getAccessPathOf(inc).equals(resolvedAccessPath) || resolvedAccessPath.getExclusions().size() < 1) {
			interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(getAnalyzer(inc), AccessPath.<Field>empty(), this));
		}
		else {
			AccessPath<Field> deltaTo = resolvedAccessPath.getDeltaToAsAccessPath(getAccessPathOf(inc));
			ResolverTemplate<Field,Fact,Stmt,Method,Incoming> nestedResolver = getOrCreateNestedResolver(getAccessPathOf(inc));
			interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(nestedResolver.getAnalyzer(inc), deltaTo, nestedResolver));
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
					nestedResolver.addIncoming(inc.getValue());
				}
				return nestedResolver;
			}
		}
		return nestedResolvers.get(newAccPath);
	}
	
	protected abstract ResolverTemplate<Field, Fact, Stmt, Method, Incoming> createNestedResolver(AccessPath<Field> newAccPath);
}