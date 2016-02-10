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

import heros.cfl.RegularOverApproximizer.NonTerminalPrimeManager;
import heros.utilities.DefaultValueMap;

import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Sets;

public class ReadableRuleTransformer {

	private DefaultValueMap<NonTerminal, NonTerminal> ntMapping = new DefaultValueMap<NonTerminal, NonTerminal>() {
		@Override
		protected NonTerminal createItem(NonTerminal key) {
			int ntIndex = ntMapping.size();
			char letter = (char) (65+ntIndex % 24);
			int index = (int) Math.floor(ntIndex / 24.0);
			return new NonTerminal(letter + (index>0 ? String.valueOf(index+1) : ""));
		}
	};
	private DefaultValueMap<Object, String> terminalMapping = new DefaultValueMap<Object, String>() {
		@Override
		protected String createItem(Object key) {
			int ntIndex = terminalMapping.size();
			char letter = (char) (97+ntIndex % 24);
			int index = (int) Math.floor(ntIndex / 24.0);
			return letter + (index>0 ? String.valueOf(index+1) : "");
		}
	};
	private NonTerminalPrimeManager prime;	
	
	public ReadableRuleTransformer(NonTerminalPrimeManager prime) {
		this.prime = prime;
	}

	public Set<NonTerminal> map(Set<NonTerminal> nonTerminals) {
		Set<NonTerminal> result = Sets.newHashSet();
		for(NonTerminal nt : nonTerminals) {
			NonTerminal mappedNt = map(nt);
			result.add(mappedNt);
			for(Rule r : nt.getRules()) {
				mappedNt.addRule(map(r));
			}
		}
		return result;
	}

	public Rule map(Rule r) {
		return r.accept(new RuleVisitor<Rule>() {
			@Override
			public Rule visit(ContextFreeRule contextFreeRule) {
				return new ContextFreeRule(map(contextFreeRule.getLeftTerminals()), map(contextFreeRule.getNonTerminal()), map(contextFreeRule.getRightTerminals()));
			}

			@Override
			public Rule visit(NonLinearRule nonLinearRule) {
				return new NonLinearRule(nonLinearRule.getLeft().accept(this), nonLinearRule.getRight().accept(this));
			}

			@Override
			public Rule visit(RegularRule regularRule) {
				return new RegularRule(map(regularRule.getNonTerminal()), map(regularRule.getTerminals()));
			}

			@Override
			public Rule visit(ConstantRule constantRule) {
				return new ConstantRule(map(constantRule.getTerminals()));
			}
		});
	}
	
	public Terminal[] map(Terminal[] terminals) {
		Terminal[] result = new Terminal[terminals.length];
		for(int i=0; i<terminals.length; i++) {
			String newRepr = terminalMapping.getOrCreate(terminals[i].getRepresentation());
			if(terminals[i] instanceof ProducingTerminal)
				result[i] = new ProducingTerminal(newRepr);
			else if(terminals[i] instanceof ConsumingTerminal)
				result[i] = new ConsumingTerminal(newRepr);
			else if(terminals[i] instanceof ExclusionTerminal)
				result[i] = new ExclusionTerminal(newRepr);
			else
				throw new IllegalStateException();
		}
		return result;
	}

	public NonTerminal map(NonTerminal nt) {
		if(ntMapping.containsKey(nt))
			return ntMapping.get(nt);
		
		if(prime.isPrime(nt)) {			
			NonTerminal mappedKey = ntMapping.getOrCreate(prime.getNonPrime(nt));
			NonTerminal result = new NonTerminal(mappedKey.getRepresentation()+"'");
			ntMapping.put(nt, result);
			return result;
		}
		
		return ntMapping.getOrCreate(nt);
	}
}
