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

import static heros.cfl.StrongConnectedComponentDetector.from;
import heros.solver.Pair;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class RegularOverApproximizer {

	NonTerminalPrimeManager prime = new NonTerminalPrimeManager() {
		private BiMap<NonTerminal, NonTerminal> endStates = HashBiMap.create();

		@Override
		public NonTerminal get(NonTerminal nt) {
			if(endStates.containsValue(nt))
				return nt;
			else if(!endStates.containsKey(nt)) {
				NonTerminal value = new NonTerminal(nt.getRepresentation()+"'");
				value.addRule(new ConstantRule());
				endStates.put(nt, value);
			}
			return endStates.get(nt);
		}

		@Override
		public boolean isPrime(NonTerminal nt) {
			return endStates.containsValue(nt);
		}

		@Override
		public NonTerminal getNonPrime(NonTerminal prime) {
			return endStates.inverse().get(prime);
		}
	};
	private Map<NonTerminal, Set<NonTerminal>> nonTerminalToScc = Maps.newHashMap();
	private Set<NonTerminal> visited = Sets.newHashSet();

	public static interface NonTerminalPrimeManager {
		NonTerminal get(NonTerminal nt);
		boolean isPrime(NonTerminal nt);
		NonTerminal getNonPrime(NonTerminal prime);
	}

	public RegularOverApproximizer() {}

	public RegularOverApproximizer(NonTerminalPrimeManager prime) {
		this.prime = prime;
	}

	public void approximate(Collection<NonTerminal> entryPoints) {
		entryPoints.removeAll(visited);
		Collection<Set<NonTerminal>> sccs = new StrongConnectedComponentDetector(entryPoints).results();
		for(Set<NonTerminal> scc : sccs) {
			if(containsNonLeftLinearRules(scc)) {
				storeScc(scc);
				rewrite(scc);
			}
		}
	}
	
	public void approximate(Rule root) {
		approximate(root.accept(new RuleVisitor.CollectingRuleVisitor<NonTerminal, Set<NonTerminal>>(Sets.<NonTerminal>newHashSet()) {
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
		}));
	}

	private void storeScc(Set<NonTerminal> scc) {
		for(NonTerminal nt : scc) {
			nonTerminalToScc.put(nt, scc);
		}
	}

	private static boolean isNonLeftLinearRule(final Set<NonTerminal> scc, Rule rule) {
		return rule.accept(new RuleVisitor<Boolean>() {
			@Override
			public Boolean visit(ContextFreeRule contextFreeRule) {
				return scc.contains(contextFreeRule.getNonTerminal());
			}

			@Override
			public Boolean visit(NonLinearRule nonLinearRule) {
				return nonLinearRule.getRight().accept(new ContainsNonTerminal(scc)) || nonLinearRule.getLeft().accept(this);
			}

			@Override
			public Boolean visit(RegularRule regularRule) {
				return false;
			}

			@Override
			public Boolean visit(ConstantRule constantRule) {
				return false;
			}
		});
	}
	
	private static boolean containsNonLeftLinearRules(final Set<NonTerminal> scc) {
		for(NonTerminal nt : scc) {
			for(Rule rule : nt.getRules()) {
				if(isNonLeftLinearRule(scc, rule))
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
			rewriteRulesOf(current, scc);
		}
	}

	private void rewriteRulesOf(final NonTerminal current, Set<NonTerminal> scc) {
		for(Rule rule : current.removeAllRules()) {
			addRewrittenRule(current, rule, scc);
		}
	}
	
	private void addRewrittenRule(NonTerminal sourceNonTerminal, Rule rule, Set<NonTerminal> scc) {
		Pair<NonTerminal, Rule> result = rule.accept(new RewriteVisitor(scc, prime, sourceNonTerminal, new ConstantRule()));
		result.getO1().addRule(new RegularRule(prime.get(sourceNonTerminal)).append(result.getO2()));
	}
	
	private static boolean partOfScc(NonTerminalPrimeManager prime, Collection<NonTerminal> scc, NonTerminal nt) {
		if(scc.contains(nt))
			return true;
		if(prime.isPrime(nt) && scc.contains(prime.getNonPrime(nt)))
			return true;
		else
			return false;
	}
	
	private static class RewriteVisitor implements RuleVisitor<Pair<NonTerminal, Rule>> {
		
		private Set<NonTerminal> scc;
		private NonTerminal current;
		private Rule rest;
		private NonTerminalPrimeManager prime;
		
		public RewriteVisitor(Set<NonTerminal> scc,  NonTerminalPrimeManager prime, NonTerminal current, Rule rest) {
			this.scc = scc;
			this.prime = prime;
			this.current = current;
			this.rest = rest;
		}
		
		@Override
		public Pair<NonTerminal, Rule> visit(ContextFreeRule contextFreeRule) {
			if(partOfScc(prime, scc, contextFreeRule.getNonTerminal())) {
				current.addRule(new RegularRule(contextFreeRule.getNonTerminal(), contextFreeRule.getRightTerminals()).append(rest));
				return new Pair<NonTerminal, Rule>(prime.get(contextFreeRule.getNonTerminal()), new ConstantRule(contextFreeRule.getLeftTerminals()));
			} else {
				return new Pair<NonTerminal, Rule>(current, contextFreeRule.append(rest));
			}
		}
		
		@Override
		public Pair<NonTerminal, Rule> visit(NonLinearRule nonLinearRule) {
			Pair<NonTerminal, Rule> result = nonLinearRule.getRight().accept(this);
			return nonLinearRule.getLeft().accept(new RewriteVisitor(scc, prime, result.getO1(), result.getO2()));
		}
		
		@Override
		public Pair<NonTerminal, Rule> visit(RegularRule regularRule) {
			if(partOfScc(prime, scc, regularRule.getNonTerminal())) {
				current.addRule(regularRule.append(rest));
				return new Pair<NonTerminal, Rule>(prime.get(regularRule.getNonTerminal()), new ConstantRule());
			} else {
				return new Pair<NonTerminal, Rule>(current, regularRule.append(rest));
			}
		}

		@Override
		public Pair<NonTerminal, Rule> visit(ConstantRule constantRule) {
			return new Pair<NonTerminal, Rule>(current, constantRule.append(rest));
		}
	}

	private void updateApproximatedRules(final NonTerminal nonTerminal, final Set<NonTerminal> newScc) {
		for(Rule rule : Lists.newLinkedList(nonTerminal.getRules())) {
			rule.accept(new RuleVisitor<Void>() {
				@Override
				public Void visit(ContextFreeRule contextFreeRule) {
					throw new IllegalStateException("Over-Approximation is not correct of rule: "+nonTerminal+" -> "+contextFreeRule);
				}

				@Override
				public Void visit(NonLinearRule nonLinearRule) {
					if(isNonLeftLinearRule(newScc, nonLinearRule)) {
						nonTerminal.removeRule(nonLinearRule);
						Pair<NonTerminal, Rule> pair = nonLinearRule.accept(new RewriteVisitor(newScc, prime, nonTerminal, new ConstantRule()));
						assert pair.getO2().isEmpty();
					}
					return null;
				}

				@Override
				public Void visit(RegularRule regularRule) {
					//nothing to do
					return null;
				}

				@Override
				public Void visit(ConstantRule constantRule) {
					//nothing to do
					return null;
				}
			});
		}
	}
	
	private boolean isApproximated(NonTerminal nt) {
		return nonTerminalToScc.containsKey(nt);
	}

	private boolean isApproximated(Collection<NonTerminal> list) {
		for(NonTerminal nt : list) {
			if(isApproximated(nt))
				return true;
		}
		return false;
	}
	
	public void addRule(final NonTerminal sourceNonTerminal, Rule rule) {
		assert regular();
		Set<NonTerminal> targetNonTerminals = rule.accept(new CollectNonTerminalsRuleVisitor());
		
		Collection<Set<NonTerminal>> sccs = new StrongConnectedComponentDetector(from(sourceNonTerminal).to(targetNonTerminals)).results();
		Optional<Set<NonTerminal>> scc = Iterables.tryFind(sccs, new Predicate<Set<NonTerminal>>() {
			@Override
			public boolean apply(Set<NonTerminal> input) {
				return input.contains(sourceNonTerminal);
			}
		});
		if(scc.isPresent()) {
			boolean approximationRequired = isNonLeftLinearRule(scc.get(), rule) ||	
					isApproximated(scc.get()) || containsNonLeftLinearRules(scc.get());
			
			if(approximationRequired) {
				Set<NonTerminal> filteredScc = Sets.filter(scc.get(), new Predicate<NonTerminal>() {
					@Override
					public boolean apply(NonTerminal input) {
						return !prime.isPrime(input);
					}
				});

				boolean newNonTerminalsInScc = isApproximated(sourceNonTerminal) ? 
						!nonTerminalToScc.get(sourceNonTerminal).equals(filteredScc) : true;			
				if(newNonTerminalsInScc) {
					for(NonTerminal nt : filteredScc) {
						if(isApproximated(nt)) {
							updateApproximatedRules(nt, filteredScc);
							updateApproximatedRules(prime.get(nt), filteredScc);
						} else {
							rewriteRulesOf(nt, filteredScc);
						}
					}
					storeScc(filteredScc);
				}
				addRewrittenRule(sourceNonTerminal, rule, filteredScc);
			} else {
				sourceNonTerminal.addRule(rule);
			}
		}
		assert regular();
	}

	private boolean regular() {
		Collection<Set<NonTerminal>> sccs = new StrongConnectedComponentDetector(nonTerminalToScc.keySet().toArray(new NonTerminal[0])).results();
		boolean result = true;
		for(Set<NonTerminal> scc : sccs) {
			String nonLeftLinearRules = "";
			for(NonTerminal nt : scc) {
				for(Rule rule : nt.getRules()) {
					if(isNonLeftLinearRule(scc, rule))
						nonLeftLinearRules += nt+" -> "+rule+"\n";
				}
			}
			
			if(!nonLeftLinearRules.isEmpty()) {
				System.out.println("-----------------");
				System.out.println("Non left linear rules in SCC found:");
				System.out.println(nonLeftLinearRules);
				System.out.println("The respective SCC is: ");
				System.out.println(Joiner.on(", ").join(scc));
				System.out.println("-----------------");
				result = false;
			}
		}
		return result;
	}
}
