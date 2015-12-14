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

	private Optional<NonTerminal> nonTerminal;
	private Terminal[] terminals;

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
	public boolean containsConsumers() {
		for(Terminal t : terminals)
			if(t.isConsumer())
				return true;
		return false;
	}

	@Override
	public boolean isPossible() {
		int firstConsumer = -1;
		for(int i=0; i<terminals.length; i++) {
			if(terminals[i].isConsumer()) {
				if(firstConsumer < 0)
					firstConsumer = i;
				int correspondingProducerIndex = 2*firstConsumer-i-1;
				if(correspondingProducerIndex >= 0) {
					if(!terminals[correspondingProducerIndex].isProducing((ConsumingTerminal) terminals[i]))
						return false;
				}	
				else
					return true;
			}
		}
		return true;
	}

	@Override
	public void accept(RuleVisitor ruleVisitor) {
		ruleVisitor.visit(this);
	}

	public Optional<NonTerminal> getNonTerminal() {
		return nonTerminal;
	}

	public Rule applyForNonTerminal(Rule rule) {
		return rule.append(terminals);
	}

	@Override
	public Rule append(Terminal... terminals) {
		if(terminals.length == 0)
			return this;
		if(this.terminals.length == 0)
			return new RegularRule(nonTerminal, terminals);
		
		int skip = 0;
		for(int i=0; i<terminals.length; i++) {
			if(terminals[i].isConsumer() && this.terminals.length>i && this.terminals[this.terminals.length-i-1].isProducing((ConsumingTerminal) terminals[i]))  {
				skip++;
			}
			else
				break;
		}
		
		Terminal[] newTerminals = Arrays.copyOf(this.terminals, this.terminals.length+terminals.length -skip*2);
		System.arraycopy(terminals, skip, newTerminals, this.terminals.length-skip, terminals.length-skip);
		return new RegularRule(nonTerminal, newTerminals);
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
