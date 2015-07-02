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

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class Resolver<Fact, Stmt, Method, Value> {

	private boolean resolvedUnbalanced = false;
	private List<InterestCallback<Fact, Stmt, Method, Value>> interestCallbacks = Lists.newLinkedList();
	protected PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer;
	private Set<EdgeFunction<Value>> balancedFunctions = Sets.newHashSet();
	
	public Resolver(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer) {
		this.analyzer = analyzer;
	}

	public abstract void resolve(EdgeFunction<Value> edgeFunction, InterestCallback<Fact, Stmt, Method, Value> callback);
	
	public void resolvedUnbalanced() {
		if(resolvedUnbalanced)
			return;

		log("Interest given");
		resolvedUnbalanced = true;
		for(InterestCallback<Fact, Stmt, Method, Value> callback : Lists.newLinkedList(interestCallbacks)) {
			callback.interest(analyzer, this);
		}
	}
	
	public boolean isResolvedUnbalanced() {
		return resolvedUnbalanced;
	}
	
	protected void continueBalancedTraversal(EdgeFunction<Value> edgeFunction) {
		if(balancedFunctions.add(edgeFunction)) {
			for(InterestCallback<Fact, Stmt, Method, Value> callback : Lists.newLinkedList(interestCallbacks)) {
				callback.continueBalancedTraversal(edgeFunction);
			}
		}
	}

	protected void registerCallback(InterestCallback<Fact, Stmt, Method, Value> callback) {
		if(resolvedUnbalanced) {
			callback.interest(analyzer, this);
		}
		else {
			log("Callback registered");
			interestCallbacks.add(callback);
		}

		if(!balancedFunctions.isEmpty()) {
			for(EdgeFunction<Value> edgeFunction : balancedFunctions) {
				callback.continueBalancedTraversal(edgeFunction);
			}
		}
	}
	
	protected abstract void log(String message);
	
}
