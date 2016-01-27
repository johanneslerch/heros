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
package heros.cfl;

import java.util.Set;

import com.google.common.collect.Sets;

import heros.cfl.RuleVisitor.CollectingRuleVisitor;

public class CollectNonTerminalsRuleVisitor extends CollectingRuleVisitor<NonTerminal, Set<NonTerminal>> {

	public CollectNonTerminalsRuleVisitor() {
		super(Sets.<NonTerminal>newHashSet());
	}

	@Override
	void _visit(ContextFreeRule contextFreeRule) {
		yield(contextFreeRule.getNonTerminal());
	}

	@Override
	void _visit(NonLinearRule nonLinearRule) {
		nonLinearRule.getLeft().accept(this);
		nonLinearRule.getRight().accept(this);
	}

	@Override
	void _visit(RegularRule regularRule) {
		yield(regularRule.getNonTerminal());
	}

	@Override
	void _visit(ConstantRule constantRule) {
	}

}
