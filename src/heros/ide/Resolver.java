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

import java.util.List;

import com.google.common.collect.Lists;

public abstract class Resolver<Fact, Stmt, Method, Value> {

	private boolean interest = false;
	private List<InterestCallback<Fact, Stmt, Method, Value>> interestCallbacks = Lists.newLinkedList();
	protected PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer;
	private boolean canBeResolvedEmpty = false;
	
	public Resolver(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer) {
		this.analyzer = analyzer;
	}

	public abstract void resolve(InterestCallback<Fact, Stmt, Method, Value> callback);
	
	public void interest() {
		if(interest)
			return;

		log("Interest given");
		interest = true;
		for(InterestCallback<Fact, Stmt, Method, Value> callback : Lists.newLinkedList(interestCallbacks)) {
			callback.interest(analyzer, this);
		}
		
		if(canBeResolvedEmpty)
			interestCallbacks = null;
	}
	
	protected void canBeResolvedEmpty() {
		if(canBeResolvedEmpty)
			return;
		
		canBeResolvedEmpty = true;
		for(InterestCallback<Fact, Stmt, Method, Value> callback : Lists.newLinkedList(interestCallbacks)) {
			callback.canBeResolvedEmpty();
		}
		
		if(interest)
			interestCallbacks = null;
	}

	public boolean isInterestGiven() {
		return interest;
	}

	protected void registerCallback(InterestCallback<Fact, Stmt, Method, Value> callback) {
		if(interest) {
			callback.interest(analyzer, this);
		}
		else {
			log("Callback registered");
			interestCallbacks.add(callback);
		}

		if(canBeResolvedEmpty)
			callback.canBeResolvedEmpty();
	}
	
	protected abstract void log(String message);
	
}
