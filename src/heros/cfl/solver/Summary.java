/*******************************************************************************
 * Copyright (c) 2016 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.cfl.solver;

import heros.cfl.ConstantRule;
import heros.cfl.DisjointnessSolver.QueryListener;
import heros.cfl.Rule;

public class Summary<Field, Fact, Stmt, Method> {

	private WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt;
	private boolean requiresCallingContextCheck = true;

	public Summary(Context<Field, Fact, Stmt, Method> context, WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		this.factAtStmt = factAtStmt;
		
		if(factAtStmt.getRule() instanceof ConstantRule) {
			if(factAtStmt.getRule().isSolved())
				requiresCallingContextCheck = false;
		} else {
			context.disjointnessSolver.constantCheck(factAtStmt.getRule(), new QueryListener() {
				@Override
				public void solved() {
					requiresCallingContextCheck = false;
				}
			});
		}
	}

	public Stmt getStatement() {
		return factAtStmt.getStatement();
	}

	public Fact getFact() {
		return factAtStmt.getFact();
	}

	public Rule getRule() {
		return factAtStmt.getRule();
	}

	public boolean requiresCallingContextCheck() {
		return requiresCallingContextCheck;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((factAtStmt == null) ? 0 : factAtStmt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Summary other = (Summary) obj;
		if (factAtStmt == null) {
			if (other.factAtStmt != null)
				return false;
		} else if (!factAtStmt.equals(other.factAtStmt))
			return false;
		return true;
	}
}
