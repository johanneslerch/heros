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
import heros.fieldsens.FlowFunction.Constraint;
import heros.fieldsens.structs.AccessPathAndResolver;

public class RepeatedFieldCallEdgeResolver<Field, Fact, Stmt, Method> extends CallEdgeResolver<Field, Fact, Stmt, Method> {

	private Delta<Field> repeatedField;

	public RepeatedFieldCallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
			Debugger<Field, Fact, Stmt, Method> debugger, CallEdgeResolver<Field, Fact, Stmt, Method> parent, Delta<Field> repeatedField) {
		super(analyzer, debugger, parent);
		this.repeatedField = repeatedField;
	}
	
	@Override
	public void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback) {
		if(constraint.applyToAccessPath(resolvedAccessPath).isPrefixOf(repeatedField.applyTo(resolvedAccessPath)) == PrefixTestResult.GUARANTEED_PREFIX) {
			callback.interest(analyzer, new AccessPathAndResolver<Field, Fact, Stmt, Method>(AccessPath.<Field>empty()/* TODO double check*/, this));
		}
		else
			super.resolve(constraint, callback);
	}
	
	@Override
	public String toString() {
		return "<"+analyzer.getAccessPath()+"."+repeatedField+"*:"+analyzer.getMethod()+">";
	}
}
