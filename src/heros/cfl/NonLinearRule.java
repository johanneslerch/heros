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
package heros.cfl;

import com.google.common.base.Optional;

public class NonLinearRule implements Rule {

	private Rule left;
	private Rule right;

	public NonLinearRule(Rule left, Rule right) {
		this.left = left;
		this.right = right;
	}
	
	@Override
	public boolean containsConsumers() {
		return right.containsConsumers();
	}

	@Override
	public boolean isPossible() {
		return right.isPossible();
	}

	@Override
	public void accept(RuleVisitor ruleVisitor) {
		ruleVisitor.visit(this);
	}

	@Override
	public Rule append(Terminal... terminals) {
		// TODO Auto-generated method stub
		return null;
	}
}
