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


/**
 * Is passed as callback to Resolver.resolve(constraint, callback);
 *
 */
public interface InterestCallback<Fact, Stmt, Method, Value> {

	/**
	 * 
	 * @param analyzer
	 * @param resolver The resolver that was able to resolve the constraint. This may be a nested resolver of the resolver
	 *  that was asked to resolve the constraint or some preceding resolver.
	 * @param edgeFunction id if the resolver that was asked was able to resolve the request, otherwise an edge function representing 
	 * the edge functions between the resolver that was asked and the resolver that was able to resolve the request (the latter is given as argument as well).
	 */
	void interest(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Resolver<Fact, Stmt, Method, Value> resolver, EdgeFunction<Value> edgeFunction);
	
	/**
	 * 
	 * @param edgeFunction A partially resolved edge function that does not yet satisfy the constraint. This edge function is
	 *  not composed with the constraint nor with previous constraints of the resolver!
	 */
	void continueBalancedTraversal(EdgeFunction<Value> edgeFunction);
}
