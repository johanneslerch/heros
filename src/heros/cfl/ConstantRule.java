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

import java.util.Arrays;

import com.google.common.base.Joiner;

import heros.cfl.TerminalUtil.BalanceResult;

public class ConstantRule implements Rule {

	private Terminal[] terminals;

	public ConstantRule(Terminal... terminals) {
		this.terminals = terminals;
	}
	
	@Override
	public String toString() {
		if(terminals.length==0)
			return "\u03B5";
		else
			return Joiner.on("").join(terminals);
	}
	
	@Override
	public boolean isSolved() {
		for(Terminal t : terminals)
			if(t.isConsumer() || t.isExclusion())
				return false;
		return true;
	}

	@Override
	public boolean isPossible() {
		return TerminalUtil.isBalanced(terminals) == BalanceResult.BALANCED;
	}

	@Override
	public <T> T accept(RuleVisitor<T> ruleVisitor) {
		return ruleVisitor.visit(this);
	}

	@Override
	public ConstantRule append(Terminal... terminals) {
		return new ConstantRule(TerminalUtil.append(this.terminals, terminals));
	}

	@Override
	public Rule append(Rule rule) {
		return rule.accept(new RuleVisitor<Rule>(){
			@Override
			public Rule visit(ContextFreeRule contextFreeRule) {
				return new ContextFreeRule(TerminalUtil.append(terminals, contextFreeRule.getLeftTerminals()), 
						contextFreeRule.getNonTerminal(), contextFreeRule.getRightTerminals());
			}

			@Override
			public Rule visit(NonLinearRule nonLinearRule) {
				return new NonLinearRule(append(nonLinearRule.getLeft()), nonLinearRule.getRight());
			}

			@Override
			public Rule visit(RegularRule regularRule) {
				return new ContextFreeRule(terminals, regularRule.getNonTerminal(), regularRule.getTerminals());
			}

			@Override
			public Rule visit(ConstantRule constantRule) {
				return append(constantRule.getTerminals());
			}
		});
	}

	@Override
	public boolean containsNonTerminals() {
		return false;
	}

	@Override
	public Terminal[] getTerminals() {
		return terminals;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(terminals);
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
		ConstantRule other = (ConstantRule) obj;
		if (!Arrays.equals(terminals, other.terminals))
			return false;
		return true;
	}

}
