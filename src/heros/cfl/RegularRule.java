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
		return (nonTerminal.isPresent() ? nonTerminal.get() : "") + Joiner.on("").join(terminals);
	}

	@Override
	public Optional<NonTerminal> getNonTerminal() {
		return nonTerminal;
	}

	@Override
	public boolean areSuccessorsPossible(ConsumingTerminal... consumingTerminals) {
		for(int i=0; i<consumingTerminals.length && i<terminals.length; i++) {
			if(terminals[terminals.length -1 -i].isConsumer())
				return true;
			else if(!terminals[terminals.length -1 -i].isProducing(consumingTerminals[i]))
				return false;
		}
		return true;
	}

	@Override
	public Rule apply(ConsumingTerminal... consumingTerminals) {
		for(int i=0; i<consumingTerminals.length && i<terminals.length; i++) {
			if(terminals[terminals.length -1 -i].isConsumer())
				return new RegularRule(nonTerminal, merge(i, consumingTerminals));
			else if(!terminals[terminals.length -1 -i].isProducing(consumingTerminals[i]))
				throw new IllegalStateException(toString()+" cannot be succeeded by "+Joiner.on("").join(consumingTerminals));
		}
		return new RegularRule(nonTerminal, Arrays.copyOfRange(consumingTerminals, terminals.length, consumingTerminals.length));
	}
	
	private Terminal[] merge(int index, ConsumingTerminal[] consumingTerminals) {
		Terminal[] result = new Terminal[terminals.length-index + consumingTerminals.length-index];
		System.arraycopy(terminals, 0, result, 0, terminals.length-index);
		System.arraycopy(consumingTerminals, index, result, terminals.length-index, consumingTerminals.length-index);
		return result;
	}

	@Override
	public boolean containsConsumers() {
		for(Terminal t : terminals)
			if(t.isConsumer())
				return true;
		return false;
	}

	@Override
	public ConsumingTerminal[] getConsumers() {
		for(int i=0; i<terminals.length; i++) 
			if(!terminals[i].isConsumer())
				return Arrays.copyOfRange(terminals, 0, i, ConsumingTerminal[].class);
		return Arrays.copyOf(terminals, terminals.length, ConsumingTerminal[].class);
	}
}
