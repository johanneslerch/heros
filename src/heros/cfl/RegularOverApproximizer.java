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

import heros.solver.Pair;
import heros.utilities.DefaultValueMap;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class RegularOverApproximizer {

	final DefaultValueMap<NonTerminal, NonTerminal> endStates = new DefaultValueMap<NonTerminal, NonTerminal>() {
		@Override
		protected NonTerminal createItem(NonTerminal key) {
			NonTerminal result = new NonTerminal(key.getRepresentation() + "'");
			result.addRule(new ConstantRule());
			return result;
		}
	};
	
	public NonTerminal createNonTerminalPrime(NonTerminal nt) {
		return endStates.getOrCreate(nt);
	}
	
	public void approximate(Rule root) {
		Set<NonTerminal> entryPoints = root.accept(new RuleVisitor.CollectingRuleVisitor<NonTerminal, Set<NonTerminal>>(Sets.<NonTerminal>newHashSet()) {
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
		});
		Collection<Set<NonTerminal>> sccs = new StrongConnectedComponentDetector(entryPoints).results();
		for(Set<NonTerminal> scc : sccs) {
			if(containsNonLeftLinearRules(scc))
				rewrite(scc);
		}
	}

	private static boolean containsNonLeftLinearRules(final Set<NonTerminal> scc) {
		for(NonTerminal nt : scc) {
			for(Rule rule : nt.getRules()) {
				if(rule.accept(new RuleVisitor<Boolean>() {
					@Override
					public Boolean visit(ContextFreeRule contextFreeRule) {
						return scc.contains(contextFreeRule.getNonTerminal());
					}

					@Override
					public Boolean visit(NonLinearRule nonLinearRule) {
						ContainsNonTerminal ruleVisitor = new ContainsNonTerminal(scc);
						return nonLinearRule.getLeft().accept(ruleVisitor) && nonLinearRule.getRight().accept(ruleVisitor);
					}

					@Override
					public Boolean visit(RegularRule regularRule) {
						return false;
					}

					@Override
					public Boolean visit(ConstantRule constantRule) {
						return false;
					}
				}))
					return true;
			}
		}
		return false;
	}
	
	private static class ContainsNonTerminal implements RuleVisitor<Boolean> {

		private Set<NonTerminal> set;

		public ContainsNonTerminal(Set<NonTerminal> set) {
			this.set = set;
		}
		
		@Override
		public Boolean visit(ContextFreeRule contextFreeRule) {
			return set.contains(contextFreeRule.getNonTerminal());
		}

		@Override
		public Boolean visit(NonLinearRule nonLinearRule) {
			return nonLinearRule.getLeft().accept(this) || nonLinearRule.getRight().accept(this);
		}

		@Override
		public Boolean visit(RegularRule regularRule) {
			return set.contains(regularRule.getNonTerminal());
		}

		@Override
		public Boolean visit(ConstantRule constantRule) {
			return false;
		}
	}

	private void rewrite(Set<NonTerminal> scc) {
		for(final NonTerminal current : scc) {
			final NonTerminal currentPrime = endStates.getOrCreate(current);
			for(Rule rule : current.dropRules()) {
				Pair<NonTerminal, Rule> result = rule.accept(new RewriteVisitor(scc, endStates, current, new ConstantRule()));
				result.getO1().addRule(new RegularRule(currentPrime).append(result.getO2()));
			}
		}
	}
	
	private static class RewriteVisitor implements RuleVisitor<Pair<NonTerminal, Rule>> {
		
		private Set<NonTerminal> scc;
		private NonTerminal current;
		private Rule rest;
		private DefaultValueMap<NonTerminal, NonTerminal> endStates;
		
		public RewriteVisitor(Set<NonTerminal> scc,  DefaultValueMap<NonTerminal, NonTerminal> endStates, NonTerminal current, Rule rest) {
			this.scc = scc;
			this.endStates = endStates;
			this.current = current;
			this.rest = rest;
		}
		
		@Override
		public Pair<NonTerminal, Rule> visit(ContextFreeRule contextFreeRule) {
			if(scc.contains(contextFreeRule.getNonTerminal())) {
				current.addRule(new RegularRule(contextFreeRule.getNonTerminal(), contextFreeRule.getRightTerminals()).append(rest));
				return new Pair<NonTerminal, Rule>(endStates.getOrCreate(contextFreeRule.getNonTerminal()), new ConstantRule(contextFreeRule.getLeftTerminals()));
			} else {
				return new Pair<NonTerminal, Rule>(current, contextFreeRule.append(rest));
			}
		}
		
		@Override
		public Pair<NonTerminal, Rule> visit(NonLinearRule nonLinearRule) {
			Pair<NonTerminal, Rule> result = nonLinearRule.getRight().accept(this);
			return nonLinearRule.getLeft().accept(new RewriteVisitor(scc, endStates, result.getO1(), result.getO2()));
		}
		
		@Override
		public Pair<NonTerminal, Rule> visit(RegularRule regularRule) {
			if(scc.contains(regularRule.getNonTerminal())) {
				current.addRule(regularRule.append(rest));
				return new Pair<NonTerminal, Rule>(endStates.getOrCreate(regularRule.getNonTerminal()), new ConstantRule());
			} else {
				return new Pair<NonTerminal, Rule>(current, regularRule.append(rest));
			}
		}

		@Override
		public Pair<NonTerminal, Rule> visit(ConstantRule constantRule) {
			return new Pair<NonTerminal, Rule>(current, constantRule.append(rest));
		}
	}
}
