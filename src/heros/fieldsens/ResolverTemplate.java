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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.AccessPath.PrefixTestResult;
import heros.fieldsens.FlowFunction.Constraint;
import heros.fieldsens.structs.AccessPathAndResolver;


public abstract class ResolverTemplate<Field, Fact, Stmt, Method, Incoming>  extends Resolver<Field, Fact, Stmt, Method> {

	protected Multimap<Resolver<Field, Fact, Stmt, Method>, Incoming> incomingEdges = LinkedHashMultimap.create();
	protected Set<Incoming> incomingEdgeValues = Sets.newLinkedHashSet();
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
	
	protected void registerTransitiveResolverCallback(Incoming inc, TransitiveResolverCallback<Field, Fact, Stmt, Method> callback) {
		getResolver(inc).registerTransitiveResolverCallback(callback);
	}
	
	@Override
	public void registerTransitiveResolverCallback(TransitiveResolverCallback<Field, Fact, Stmt, Method> callback) {
		callback.resolvedByIncomingAccessPath();
	}
	
//	@Override
//	public void registerTransitiveResolverCallback(TransitiveResolverCallback<Field, Fact, Stmt, Method> callback) {
//		transitiveResolverCallbacks.add(callback);
//		for(Resolver<Field, Fact, Stmt, Method> transRes : Lists.newLinkedList(incomingEdges.keySet())) {
//			if(transRes == null)
//				callback.resolvedByIncomingAccessPath();
//			else
//				callback.resolvedBy(transRes);
//		}
//	}
	
	protected abstract Resolver<Field, Fact, Stmt, Method> getResolver(Incoming inc);
	
	private boolean shouldDismiss(Incoming inc, Resolver<Field, Fact, Stmt, Method> transitiveResolver) {
		return transitiveResolver != null && incomingEdges.containsKey(transitiveResolver);
//		if(incomingEdges.containsKey(transitiveResolver)) {
//			for(Incoming existingInc : incomingEdges.get(transitiveResolver)) {
//				if(getResolver(existingInc).isParentOf(getResolver(inc)))
//					return true;
//				else if(getResolver(inc).isParentOf(getResolver(existingInc)))
//					System.out.println();
//			}
//		}
//		return false;
	}

	public void addIncoming(final Incoming inc) {
		AccessPath<Field> incAccPath = getAccessPathOf(inc);
		if((this instanceof CallEdgeResolver || getResolver(inc) instanceof CallEdgeResolver) && incAccPath.equals(resolvedAccessPath)) {
			if(getResolver(inc) == this) {
				dismissByTransitiveResolver(inc, this);
				return;
			}
			registerTransitiveResolverCallback(inc, new TransitiveResolverCallback<Field, Fact, Stmt, Method>() {
				@Override
				public void resolvedByIncomingAccessPath() {
					addIncomingGuaranteedPrefix(inc, null);
				}
				
				@Override
				public void resolvedBy(Resolver<Field, Fact, Stmt, Method> resolver) {
					if(shouldDismiss(inc, resolver))
						dismissByTransitiveResolver(inc, resolver);
					else
						addIncomingGuaranteedPrefix(inc, resolver);
				}
			});
		} else if(resolvedAccessPath.isPrefixOf(incAccPath) == PrefixTestResult.GUARANTEED_PREFIX) {
			addIncomingGuaranteedPrefix(inc, null);
		}
		else if(incAccPath.isPrefixOf(resolvedAccessPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
//			lock();
			processIncomingPotentialPrefix(inc);
//			unlock();
		}
	}
	
	protected void dismissByTransitiveResolver(Incoming inc, Resolver<Field, Fact, Stmt, Method> resolver) {
		log("Dismissed Incoming Edge "+inc+" because we already saw the same transitive resolver: "+resolver);		
	}

	private void addIncomingGuaranteedPrefix(Incoming inc, Resolver<Field, Fact, Stmt, Method> transitiveResolver) {
		boolean isNewTransitiveResolver = !incomingEdges.containsKey(transitiveResolver);
		boolean isNewIncomingEdge = !incomingEdgeValues.contains(inc);
		if(!isNewTransitiveResolver && !isNewIncomingEdge)
			return;
		
		if(!((ResolverTemplate) getResolver(inc)).resolvedAccessPath.getExclusions().isEmpty())
			System.out.println(inc);
		if(getResolver(inc).toString().contains("ZERO-FIELD") && !getAccessPathOf(inc).toString().contains("ZERO-FIELD")) {
			System.out.println(inc);
		}
		
		incomingEdges.put(transitiveResolver, inc);
		incomingEdgeValues.add(inc);
		
		if(incomingEdgeValues.size() > 50000) {
			System.out.println(this+" has "+incomingEdgeValues.size()+" incoming edges:");
			System.out.println(Joiner.on("\n").join(incomingEdgeValues));
		}
		if(incomingEdges.keySet().size() > 1500) {
			List<String> output = Lists.newLinkedList();
			for(Resolver<Field, Fact, Stmt, Method> key : incomingEdges.keySet())
				output.add(String.valueOf(key));
			Collections.sort(output);
			System.out.println(Joiner.on("\n").join(output));
		}
		
		log("Incoming Edge: "+inc+ " (transitive resolver: "+transitiveResolver+")");
		
		if(isNewIncomingEdge) {
			interestByIncoming(inc);
			
			for(ResolverTemplate<Field, Fact, Stmt, Method, Incoming> nestedResolver : Lists.newLinkedList(nestedResolvers.values())) {
				nestedResolver.addIncoming(inc);
			}
			
			processIncomingGuaranteedPrefix(inc);
		}
		
		if(isNewTransitiveResolver) {
			for(TransitiveResolverCallback<Field, Fact, Stmt, Method> callback : Lists.newLinkedList(transitiveResolverCallbacks)) {
				if(transitiveResolver == null)
					callback.resolvedByIncomingAccessPath();
				else
					callback.resolvedBy(transitiveResolver);
			}
		}
	}

	@Override
	public void interest(AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
		assert ((ResolverTemplate)accPathResolver.resolver).resolvedAccessPath.getExclusions().isEmpty() || accPathResolver.resolver instanceof ZeroCallEdgeResolver;
		super.interest(accPathResolver);
	}
	
	protected void interestByIncoming(Incoming inc) {
		if(getAccessPathOf(inc).equals(resolvedAccessPath) || resolvedAccessPath.getExclusions().size() < 1) {
			interest(new AccessPathAndResolver<Field, Fact, Stmt, Method>(getAnalyzer(inc), AccessPath.<Field>empty(), this));
		}
		else {
			AccessPath<Field> deltaTo = resolvedAccessPath.getDeltaToAsAccessPath(getAccessPathOf(inc));
			deltaTo = AccessPath.<Field>empty().append(deltaTo.getFirstAccess());
			AccessPath<Field> newAccPath = resolvedAccessPath.append(deltaTo);
			ResolverTemplate<Field,Fact,Stmt,Method,Incoming> nestedResolver = getOrCreateNestedResolver(newAccPath);
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
			if(constraint.canBeAppliedTo(resolvedAccessPath)) {
				AccessPath<Field> newAccPath = constraint.applyToAccessPath(resolvedAccessPath);
				ResolverTemplate<Field,Fact,Stmt,Method,Incoming> nestedResolver = getOrCreateNestedResolver(newAccPath);
				if(nestedResolver == this) {
					return;
				}
				assert nestedResolver.resolvedAccessPath.equals(constraint.applyToAccessPath(resolvedAccessPath));
				nestedResolver.registerCallback(callback);
			}
		}
	}

	protected ResolverTemplate<Field, Fact, Stmt, Method, Incoming> getOrCreateNestedResolver(AccessPath<Field> newAccPath) {
		if(resolvedAccessPath.equals(newAccPath))
			return this;
		
		assert newAccPath.length() <= resolvedAccessPath.length() +1;
		
		if(!nestedResolvers.containsKey(newAccPath)) {
			if(allResolversInExclHierarchy.containsKey(newAccPath)) {
				return allResolversInExclHierarchy.get(newAccPath);
			}
			else {
				ResolverTemplate<Field, Fact, Stmt, Method, Incoming> nestedResolver = createNestedResolver(newAccPath);
				allResolversInExclHierarchy.put(newAccPath, nestedResolver);
				nestedResolvers.put(newAccPath, nestedResolver);
				
				for(Incoming inc : Sets.newLinkedHashSet(incomingEdgeValues)) {
					nestedResolver.addIncoming(inc);
				}
				return nestedResolver;
			}
		}
		return nestedResolvers.get(newAccPath);
	}
	
	protected abstract ResolverTemplate<Field, Fact, Stmt, Method, Incoming> createNestedResolver(AccessPath<Field> newAccPath);
}