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


public class ZeroCallEdgeResolver<Fact, Stmt, Method, Value> extends CallEdgeResolver<Fact, Stmt, Method, Value> {


	public ZeroCallEdgeResolver(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer) {
		super(analyzer);
	}

	@Override
	public void resolve(EdgeFunction<Value> constraint, InterestCallback<Fact, Stmt, Method, Value> callback) {
		//nothing to do. Should already be resolved via initial seed edge function or cannot be resolved anyways.
	}
	
	@Override
	public void resolvedUnbalanced() {
	}
	
	@Override
	protected ResolverTemplate<Fact, Stmt, Method, Value, CallEdge<Fact, Stmt, Method, Value>> getOrCreateNestedResolver(
			EdgeFunction<Value> constraint) {
		return this;
	}
	
	@Override
	public int hashCode() {
		return 31;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}
}
