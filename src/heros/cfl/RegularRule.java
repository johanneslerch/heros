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

import com.google.common.base.Joiner;


public class RegularRule implements Rule {

	private final NonTerminal nonTerminal;
	private final ConstantRule constantRule;

	public RegularRule(NonTerminal nonTerminal, ConstantRule constantRule) {
		this.nonTerminal = nonTerminal;
		this.constantRule = constantRule;
	}
	
	public RegularRule(NonTerminal nonTerminal, Terminal... terminals) {
		this.nonTerminal = nonTerminal;
		this.constantRule = new ConstantRule(terminals);
	}

	@Override
	public String toString() {
		return nonTerminal.toString() + Joiner.on("").join(constantRule.getTerminals());
	}

	@Override
	public boolean isSolved() {
		return constantRule.isSolved();
	}

	@Override
	public boolean isPossible() {
		switch(TerminalUtil.isBalanced(constantRule.getTerminals())) {
		case BALANCED: return true;
		default:
		case IMBALANCED: return false;
		case MORE_CONSUMERS: return true;
		}
	}

	@Override
	public <T> T accept(RuleVisitor<T> ruleVisitor) {
		return ruleVisitor.visit(this);
	}

	public NonTerminal getNonTerminal() {
		return nonTerminal;
	}

	public Rule applyForNonTerminal(Rule rule) {
		return rule.append(constantRule.getTerminals());
	}

	@Override
	public boolean containsNonTerminals() {
		return true;
	}
	
	@Override
	public Terminal[] getTerminals() {
		return constantRule.getTerminals();
	}

	public ConstantRule getConstantRule() {
		return constantRule;
	}
	
	@Override
	public Rule append(Terminal... terminals) {
		return new RegularRule(nonTerminal, constantRule.append(terminals));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constantRule == null) ? 0 : constantRule.hashCode());
		result = prime * result + ((nonTerminal == null) ? 0 : nonTerminal.hashCode());
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
		RegularRule other = (RegularRule) obj;
		if (constantRule == null) {
			if (other.constantRule != null)
				return false;
		} else if (!constantRule.equals(other.constantRule))
			return false;
		if (nonTerminal == null) {
			if (other.nonTerminal != null)
				return false;
		} else if (!nonTerminal.equals(other.nonTerminal))
			return false;
		return true;
	}

}
