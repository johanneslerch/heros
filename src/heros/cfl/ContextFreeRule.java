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


public class ContextFreeRule implements Rule {

	private final Terminal[] leftTerminals;
	private final Terminal[] rightTerminals;
	private final NonTerminal nonTerminal;
	
	public ContextFreeRule(Terminal[] leftTerminals, NonTerminal nonTerminal, Terminal[] rightTerminals) {
		this.leftTerminals = leftTerminals;
		this.nonTerminal = nonTerminal;
		this.rightTerminals = rightTerminals;
	}
	
	@Override
	public boolean isSolved() {
		return false;
	}

	@Override
	public boolean isPossible() {
		if(TerminalUtil.isBalanced(leftTerminals) == BalanceResult.IMBALANCED)
			return false;
		if(TerminalUtil.isBalanced(rightTerminals) == BalanceResult.IMBALANCED)
			return false;
		return true;
	}

	@Override
	public <T> T accept(RuleVisitor<T> ruleVisitor) {
		return ruleVisitor.visit(this);
	}

	@Override
	public Rule append(Terminal... terminals) {
		return new NonLinearRule(new ConstantRule(leftTerminals), new RegularRule(nonTerminal, TerminalUtil.append(rightTerminals, terminals)));
	}


	public NonTerminal getNonTerminal() {
		return nonTerminal;
	}
	
	@Override
	public boolean containsNonTerminals() {
		return true;
	}

	@Override
	public Terminal[] getTerminals() {
		throw new IllegalStateException();
	}
	
	public Terminal[] getRightTerminals() {
		return rightTerminals;
	}
	
	@Override
	public String toString() {
		return Joiner.on("").join(leftTerminals) + nonTerminal.toString() + Joiner.on("").join(rightTerminals);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(leftTerminals);
		result = prime * result + ((nonTerminal == null) ? 0 : nonTerminal.hashCode());
		result = prime * result + Arrays.hashCode(rightTerminals);
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
		ContextFreeRule other = (ContextFreeRule) obj;
		if (!Arrays.equals(leftTerminals, other.leftTerminals))
			return false;
		if (nonTerminal == null) {
			if (other.nonTerminal != null)
				return false;
		} else if (!nonTerminal.equals(other.nonTerminal))
			return false;
		if (!Arrays.equals(rightTerminals, other.rightTerminals))
			return false;
		return true;
	}
}
