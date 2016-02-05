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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import static heros.cfl.StrongConnectedComponentDetector.*;

public class RegularOverApproximizer {

	private final DefaultValueMap<NonTerminal, NonTerminal> endStates = new DefaultValueMap<NonTerminal, NonTerminal>() {
		@Override
		protected NonTerminal createItem(NonTerminal key) {
			NonTerminal result = new NonTerminal(key.getRepresentation() + "'");
			result.addRule(new ConstantRule());
			return result;
		}
	};
	private Map<NonTerminal, Set<NonTerminal>> nonTerminalToScc = Maps.newHashMap();
	private Set<NonTerminal> visited = Sets.newHashSet();
	
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
		entryPoints.removeAll(visited);
		Collection<Set<NonTerminal>> sccs = new StrongConnectedComponentDetector(entryPoints).results();
		for(Set<NonTerminal> scc : sccs) {
			if(containsNonLeftLinearRules(scc)) {
				storeScc(scc);
				rewrite(scc);
			}
		}
	}

	private void storeScc(Set<NonTerminal> scc) {
		for(NonTerminal nt : scc) {
			nonTerminalToScc.put(nt, scc);
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
			rewriteRulesOf(current, scc);
		}
	}

	private void rewriteRulesOf(final NonTerminal current, Set<NonTerminal> scc) {
		for(Rule rule : current.removeAllRules()) {
			Pair<NonTerminal, Rule> result = rule.accept(new RewriteVisitor(scc, endStates, current, new ConstantRule()));
			result.getO1().addRule(new RegularRule(endStates.getOrCreate(current)).append(result.getO2()));
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

	public void addRule(final NonTerminal sourceNonTerminal, Rule rule) {
		final boolean isSourceApproximated = nonTerminalToScc.containsKey(sourceNonTerminal);
		rule.accept(new RuleVisitor<Void>() {
			@Override
			public Void visit(final ContextFreeRule contextFreeRule) {
				boolean isTargetApproximated = nonTerminalToScc.containsKey(contextFreeRule.getNonTerminal());
				if(isSourceApproximated && isTargetApproximated) {
					Set<NonTerminal> sourceScc = nonTerminalToScc.get(sourceNonTerminal);
					if(sourceScc.contains(contextFreeRule.getNonTerminal())) {
						//target was already part of source's scc
						sourceNonTerminal.addRule(new RegularRule(contextFreeRule.getNonTerminal(), contextFreeRule.getRightTerminals()));
						endStates.getOrCreate(contextFreeRule.getNonTerminal()).addRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal), contextFreeRule.getLeftTerminals()));
					} else {
						//new rule connects two different approximated sccs
						Set<NonTerminal> newScc = Sets.newHashSet(sourceScc);
						Set<NonTerminal> targetScc = nonTerminalToScc.get(contextFreeRule.getNonTerminal());
						newScc.addAll(targetScc);
						sourceNonTerminal.addRule(new RegularRule(contextFreeRule.getNonTerminal(), contextFreeRule.getRightTerminals()));
						endStates.getOrCreate(contextFreeRule.getNonTerminal()).addRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal), contextFreeRule.getLeftTerminals()));
						for(NonTerminal ntOfTargetScc : targetScc) {
							updateApproximatedRules(ntOfTargetScc, sourceScc);
						}
						storeScc(newScc);
					}
				} else {
					Collection<Set<NonTerminal>> newSccs = new StrongConnectedComponentDetector(from(sourceNonTerminal).to(contextFreeRule.getNonTerminal())).results();
					Optional<Set<NonTerminal>> scc = Iterables.tryFind(newSccs, new Predicate<Set<NonTerminal>>() {
						@Override
						public boolean apply(Set<NonTerminal> scc) {
							return scc.contains(sourceNonTerminal) && scc.contains(contextFreeRule.getNonTerminal());
						}
					});
					if(scc.isPresent()) {
						if(isSourceApproximated) {
							Set<NonTerminal> mergedScc = Sets.newHashSet(scc.get());
							mergedScc.addAll(nonTerminalToScc.get(sourceNonTerminal));
							for(NonTerminal current : scc.get()) {
								if(!nonTerminalToScc.get(sourceNonTerminal).contains(current))
									rewriteRulesOf(current, mergedScc);
							}
							sourceNonTerminal.addRule(new RegularRule(contextFreeRule.getNonTerminal(), contextFreeRule.getRightTerminals()));
							endStates.getOrCreate(contextFreeRule.getNonTerminal()).addRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal), contextFreeRule.getLeftTerminals()));
							storeScc(scc.get());
						} else if(isTargetApproximated) {
							for(NonTerminal current : scc.get()) {
								if(nonTerminalToScc.get(contextFreeRule.getNonTerminal()).contains(current))
									updateApproximatedRules(current, scc.get());
								else
									rewriteRulesOf(current, scc.get());
							}
							storeScc(scc.get());
							sourceNonTerminal.addRule(new RegularRule(contextFreeRule.getNonTerminal(), contextFreeRule.getRightTerminals()));
							endStates.getOrCreate(contextFreeRule.getNonTerminal()).addRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal), contextFreeRule.getLeftTerminals()));
						} else {
							storeScc(scc.get());
							rewrite(scc.get());
							sourceNonTerminal.addRule(new RegularRule(contextFreeRule.getNonTerminal(), contextFreeRule.getRightTerminals()));
							endStates.getOrCreate(contextFreeRule.getNonTerminal()).addRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal), contextFreeRule.getLeftTerminals()));
						}
					}
					else {
						if(isSourceApproximated)
							sourceNonTerminal.addRule(new NonLinearRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal), contextFreeRule.getLeftTerminals()), 
									new RegularRule(contextFreeRule.getNonTerminal(), contextFreeRule.getRightTerminals())));
						else
							sourceNonTerminal.addRule(contextFreeRule);
					}
				}
				return null;
			}

			@Override
			public Void visit(NonLinearRule nonLinearRule) {
				final Set<NonTerminal> targetNonTerminals = nonLinearRule.accept(new CollectNonTerminalsRuleVisitor());
				Collection<Set<NonTerminal>> newSccs = new StrongConnectedComponentDetector(from(sourceNonTerminal).to(
						targetNonTerminals)).results();
				Optional<Set<NonTerminal>> scc = Iterables.tryFind(newSccs, new Predicate<Set<NonTerminal>>() {
					@Override
					public boolean apply(Set<NonTerminal> scc) {
						return scc.contains(sourceNonTerminal) && !Sets.intersection(scc, targetNonTerminals).isEmpty();
					}
				});
				if(scc.isPresent()) {
					Set<NonTerminal> mergedScc = Sets.newHashSet(scc.get());
					if(isSourceApproximated) {
						mergedScc.addAll(nonTerminalToScc.get(sourceNonTerminal));
					}
					for(NonTerminal current : mergedScc) {
						if(nonTerminalToScc.containsKey(current)) {
							updateApproximatedRules(current, mergedScc);
						} else {
							rewriteRulesOf(current, mergedScc);
						}
					}
					Pair<NonTerminal, Rule> result = nonLinearRule.accept(new RewriteVisitor(mergedScc, endStates, sourceNonTerminal, new ConstantRule()));
					result.getO1().addRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal)).append(result.getO2()));
					storeScc(mergedScc);
				}
				else if(isSourceApproximated) {
					sourceNonTerminal.addRule(new NonLinearRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal)), nonLinearRule));
				}
				else {
					sourceNonTerminal.addRule(nonLinearRule);
				}
				return null;
			}

			@Override
			public Void visit(final RegularRule regularRule) {
				boolean isTargetApproximated = nonTerminalToScc.containsKey(regularRule.getNonTerminal());
				if(isSourceApproximated && isTargetApproximated) {
					Set<NonTerminal> sourceScc = nonTerminalToScc.get(sourceNonTerminal);
					if(sourceScc.contains(regularRule.getNonTerminal())) {
						//target was already part of source's scc
						sourceNonTerminal.addRule(regularRule);
						endStates.getOrCreate(regularRule.getNonTerminal()).addRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal)));
					} else {
						//new rule connects two different approximated sccs
						Set<NonTerminal> newScc = Sets.newHashSet(sourceScc);
						Set<NonTerminal> targetScc = nonTerminalToScc.get(regularRule.getNonTerminal());
						newScc.addAll(targetScc);
						sourceNonTerminal.addRule(regularRule);
						endStates.getOrCreate(regularRule.getNonTerminal()).addRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal)));
						for(NonTerminal ntOfTargetScc : targetScc) {
							updateApproximatedRules(ntOfTargetScc, sourceScc);
						}
						storeScc(newScc);
					}
				} else {
					Collection<Set<NonTerminal>> newSccs = new StrongConnectedComponentDetector(from(sourceNonTerminal).to(regularRule.getNonTerminal())).results();
					Optional<Set<NonTerminal>> scc = Iterables.tryFind(newSccs, new Predicate<Set<NonTerminal>>() {
						@Override
						public boolean apply(Set<NonTerminal> scc) {
							return scc.contains(sourceNonTerminal) && scc.contains(regularRule.getNonTerminal());
						}
					});
					if(scc.isPresent()) {
						if(isSourceApproximated) {
							Set<NonTerminal> mergedScc = Sets.newHashSet(scc.get());
							mergedScc.addAll(nonTerminalToScc.get(sourceNonTerminal));
							for(NonTerminal current : scc.get()) {
								if(!nonTerminalToScc.get(sourceNonTerminal).contains(current))
									rewriteRulesOf(current, mergedScc);
							}
							sourceNonTerminal.addRule(regularRule);
							endStates.getOrCreate(regularRule.getNonTerminal()).addRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal)));
							storeScc(mergedScc);
						} else if(isTargetApproximated) {
							for(NonTerminal current : scc.get()) {
								if(nonTerminalToScc.get(regularRule.getNonTerminal()).contains(current))
									updateApproximatedRules(current, scc.get());
								else
									rewriteRulesOf(current, scc.get());
							}
							sourceNonTerminal.addRule(regularRule);
							endStates.getOrCreate(regularRule.getNonTerminal()).addRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal)));
							storeScc(scc.get());
						} else {
							if(containsNonLeftLinearRules(scc.get())) {
								storeScc(scc.get());
								rewrite(scc.get());
								sourceNonTerminal.addRule(regularRule);
								endStates.getOrCreate(regularRule.getNonTerminal()).addRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal)));
							}
							else
								sourceNonTerminal.addRule(regularRule);
						}
					}
					else {
						if(isSourceApproximated)
							sourceNonTerminal.addRule(new NonLinearRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal)), regularRule));
						else
							sourceNonTerminal.addRule(regularRule);
					}
				}
				return null;
			}

			@Override
			public Void visit(ConstantRule constantRule) {
				if(isSourceApproximated)
					sourceNonTerminal.addRule(new RegularRule(endStates.getOrCreate(sourceNonTerminal), constantRule));
				else
					sourceNonTerminal.addRule(constantRule);
				return null;
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
							if(nonLinearRule.accept(new ContainsNonTerminal(newScc))) {
								nonTerminal.removeRule(nonLinearRule);
								Pair<NonTerminal, Rule> pair = nonLinearRule.accept(new RewriteVisitor(newScc, endStates, nonTerminal, new ConstantRule()));
								pair.getO1().addRule(pair.getO2());
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
							throw new IllegalStateException("Over-Approximation is not correct of rule: "+nonTerminal+" -> "+constantRule);
						}
					});
				}
			}
		});
	}
}