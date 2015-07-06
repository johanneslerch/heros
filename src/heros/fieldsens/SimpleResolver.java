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
import heros.fieldsens.AccessPath.PrefixTestResult;

import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class SimpleResolver<Field, Fact, Stmt, Method> {

	private Stmt statement;
	private Fact sourceFact;
	private Set<IncomingEdge<Field, Fact, Stmt, Method>> incomingEdges;
	private Multimap<AccessPath<Field>, Callback<Field, Fact, Stmt, Method>> callbacks;
	
	public SimpleResolver(Stmt statement, Fact sourceFact) {
		this.statement = statement;
		this.sourceFact = sourceFact;
		this.incomingEdges = Sets.newHashSet();
		this.callbacks = HashMultimap.create();
	}
	
	public void addIncoming(IncomingEdge<Field, Fact, Stmt, Method> incomingEdge) {
		incomingEdges.add(incomingEdge);
		
		for(Entry<AccessPath<Field>, Callback<Field, Fact, Stmt, Method>> callbackEntry : callbacks.entries()) {
			process(incomingEdge, callbackEntry.getKey(), callbackEntry.getValue());
		}
	}
	
	public void registerCallback(AccessPath<Field> condition, Callback<Field, Fact, Stmt, Method> callback) {
		callbacks.put(condition, callback);
		for(IncomingEdge<Field, Fact, Stmt, Method> incomingEdge : incomingEdges) {
			process(incomingEdge, condition, callback);
		}
	}
	
	private void process(final IncomingEdge<Field, Fact, Stmt, Method> incomingEdge, AccessPath<Field> condition,
			final Callback<Field, Fact, Stmt, Method> callback) {
		if(condition.isPrefixOf(incomingEdge.accessPath) == PrefixTestResult.GUARANTEED_PREFIX) {
			callback.resolved(incomingEdge);
		} else if(incomingEdge.accessPath.isPrefixOf(condition).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
			final AccessPath<Field> transitiveCondition = incomingEdge.accessPath.getDeltaTo(condition).applyTo(new AccessPath<Field>());
			if(incomingEdge.resolver != null) {
				incomingEdge.resolver.registerCallback(transitiveCondition, new CallbackWrapper<Field, Fact, Stmt, Method>(callback) {
					@Override
					public void partiallyResolved(AccessPath<Field> resolvedAccessPath) {
						if(incomingEdge.transitiveReference != null) {
							AccessPath<Field> condition = resolvedAccessPath.getDeltaTo(transitiveCondition).applyTo(new AccessPath<Field>());
							process(incomingEdge.transitiveReference, condition, callback);
						}
					}
				});
			}
			else {
				callback.partiallyResolved(incomingEdge.accessPath);
			}
		}
	}

	public static class IncomingEdge<Field, Fact, Stmt, Method> {
		private SimpleResolver<Field, Fact, Stmt, Method> resolver;
		private AccessPath<Field> accessPath;
		private IncomingEdge<Field, Fact, Stmt, Method> transitiveReference;
	}
	
	public static interface Callback<Field, Fact, Stmt, Method> {
		void resolved(IncomingEdge<Field, Fact, Stmt, Method> incomingEdge);
		
		void partiallyResolved(AccessPath<Field> resolvedAccessPath);
	}
	
	public static abstract class CallbackWrapper<Field, Fact, Stmt, Method> implements Callback<Field, Fact, Stmt, Method> {

		private Callback<Field, Fact, Stmt, Method> delegate;

		public CallbackWrapper(Callback<Field, Fact, Stmt, Method> delegate) {
			this.delegate = delegate;
		}
		
		@Override
		public void resolved(IncomingEdge<Field, Fact, Stmt, Method> incomingEdge) {
			delegate.resolved(incomingEdge);
		}
	}
}
