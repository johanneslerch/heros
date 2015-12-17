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
import com.google.common.base.Optional;

public class RegularRule implements Rule {

	private final Optional<NonTerminal> nonTerminal;
	private final Terminal[] terminals;

	public RegularRule(Terminal... terminals) {
		this.nonTerminal = Optional.absent();
		this.terminals = terminals;
	}

	public RegularRule(NonTerminal nonTerminal, Terminal... terminals) {
		this.nonTerminal = Optional.of(nonTerminal);
		this.terminals = terminals;
	}

	public RegularRule(Optional<NonTerminal> nonTerminal, Terminal[] terminals) {
		this.nonTerminal = nonTerminal;
		this.terminals = terminals;
	}

	@Override
	public String toString() {
		if(!nonTerminal.isPresent() && terminals.length==0)
			return "\u03B5";
		else
			return (nonTerminal.isPresent() ? nonTerminal.get() : "") + Joiner.on("").join(terminals);
	}

	@Override
	public boolean isSolved() {
		for(Terminal t : terminals)
			if(t.isConsumer())
				return false;
		return true;
	}

	@Override
	public boolean isPossible() {
		switch(TerminalUtil.isBalanced(terminals)) {
		case BALANCED: return true;
		default:
		case IMBALANCED: return false;
		case MORE_CONSUMERS: return nonTerminal.isPresent();
		}
	}

	@Override
	public <T> T accept(RuleVisitor<T> ruleVisitor) {
		return ruleVisitor.visit(this);
	}

	public Optional<NonTerminal> getNonTerminal() {
		return nonTerminal;
	}

	public Rule applyForNonTerminal(Rule rule) {
		return rule.append(terminals);
	}

	@Override
	public boolean containsNonTerminals() {
		return nonTerminal.isPresent();
	}
	
	@Override
	public Terminal[] getTerminals() {
		return terminals;
	}
	
	@Override
	public Rule append(Terminal... terminals) {
		return new RegularRule(nonTerminal, TerminalUtil.append(this.terminals, terminals));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nonTerminal == null) ? 0 : nonTerminal.hashCode());
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
		RegularRule other = (RegularRule) obj;
		if (nonTerminal == null) {
			if (other.nonTerminal != null)
				return false;
		} else if (!nonTerminal.equals(other.nonTerminal))
			return false;
		if (!Arrays.equals(terminals, other.terminals))
			return false;
		return true;
	}
}
