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

import fj.data.Option;
import fj.function.Effect1;
import heros.cfl.TerminalUtil.BalanceResult;
import heros.utilities.DefaultValueMap;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class IntersectionSolver {

	private DefaultValueMap<Rule, Query> queries = new DefaultValueMap<Rule, Query>() {
		@Override
		protected Query createItem(Rule key) {
			return new Query(key);
		}
	};
	private DefaultValueMap<Rule, Substitution> substitutions = new DefaultValueMap<Rule, Substitution>() {
		@Override
		protected Substitution createItem(Rule key) {
			return new Substitution(key);
		}
	};
	
	public void query(Rule rule, QueryListener listener) {
		queries.getOrCreate(rule).addListener(listener);
	}
	
	public void substitute(Rule rule, SubstitutionListener listener) {
		substitutions.getOrCreate(rule).addListener(listener);
	}
	
	private class Query implements QueryListener {
		
		private boolean solved = false;
		private List<QueryListener> listeners = Lists.newLinkedList();
		
		public Query(Rule queryRule) {
			final RulePair query = RulePair.of(queryRule);
			Substitution substitution = substitutions.getOrCreate(query.substitutablePart);
			substitution.addListener(new SubstitutionListener() {
				@Override
				public void newProducingSubstitution(Rule rule) {
					if(solved)
						return;
					
					Rule application = rule.append(query.terminals);
					switch(TerminalUtil.isBalanced(application)) {
					case IMBALANCED:
						return;
					case BALANCED:
						solved = true;
						return;
					case MORE_CONSUMERS:
						if(application instanceof ConstantRule)
							return; //not solvable
						Query subQuery = queries.getOrCreate(application);
						subQuery.addListener(Query.this);
					}
				}

				@Override
				public void newConstantSubstitution(ConstantRule rule) {
					//ignore
				}
			});
		}

		protected void addListener(QueryListener listener) {
			listeners.add(listener);
		}

		@Override
		public void solved() {
			if(this.solved)
				return;
			this.solved = true;
			for(QueryListener listener : listeners)
				listener.solved();
			listeners = null;
		}
		
	}

	private static interface QueryListener {
		void solved();
	}
	
	private class Substitution {

		private List<SubstitutionListener> listeners = Lists.newLinkedList();
		private Set<Rule> producingSubstitutions = Sets.newHashSet();
		private Set<ConstantRule> constantSubstitutions = Sets.newHashSet();
		private Rule substitutableRule;
		
		public Substitution(Rule substitutableRule) {
			this.substitutableRule = substitutableRule;
		}
		
		private void start() {
			substitutableRule.accept(new RuleVisitor<Void>() {
				@Override
				public Void visit(final ContextFreeRule contextFreeRule) {
					IntersectionSolver.this.substitute(new RegularRule(contextFreeRule.getNonTerminal()), new SubstitutionListener() {
						@Override
						public void newProducingSubstitution(Rule rule) {
							addProducingSubstitution(new ConstantRule(contextFreeRule.getLeftTerminals()).append(rule));
						}

						@Override
						public void newConstantSubstitution(ConstantRule rule) {
							addConstantSubstitution(new ConstantRule(TerminalUtil.append(contextFreeRule.getLeftTerminals(), rule.getTerminals())));
						}
					});
					return null;
				}

				@Override
				public Void visit(final NonLinearRule nonLinearRule) {
					final RulePair pair = RulePair.of(nonLinearRule.getRight());
					IntersectionSolver.this.substitute(pair.substitutablePart, new SubstitutionListener() {
						@Override
						public void newProducingSubstitution(Rule rule) {
							addProducingSubstitution(nonLinearRule.getLeft().append(rule).append(pair.terminals));
						}

						@Override
						public void newConstantSubstitution(ConstantRule rule) {
							IntersectionSolver.this.substitute(nonLinearRule.getLeft().append(rule.append(pair.terminals)), 
									new SubstitutionListener() {
								@Override
								public void newProducingSubstitution(Rule rule) {
									addProducingSubstitution(rule);
								}

								@Override
								public void newConstantSubstitution(ConstantRule rule) {
									addConstantSubstitution(rule);
								}
							});
						}
					});
					return null;
				}

				@Override
				public Void visit(final RegularRule regularRule) {
					if(regularRule.getTerminals().length == 0)
						substitute(regularRule.getNonTerminal());
					else 
						IntersectionSolver.this.substitute(new RegularRule(regularRule.getNonTerminal()), new SubstitutionListener() {
							@Override
							public void newProducingSubstitution(Rule rule) {
								addProducingSubstitution(rule.append(regularRule.getTerminals()));
							}
							
							@Override
							public void newConstantSubstitution(ConstantRule rule) {
								addConstantSubstitution(rule.append(regularRule.getTerminals()));
							}
						});
					return null;
				}

				@Override
				public Void visit(ConstantRule constantRule) {
					throw new IllegalStateException();
				}
			});
		}
		
		private void addProducingSubstitution(Rule rule) {
			if(TerminalUtil.isBalanced(rule) == BalanceResult.IMBALANCED)
				return;
			
			producingSubstitutions.add(rule);
			for(SubstitutionListener listener : Lists.newArrayList(listeners))
				listener.newProducingSubstitution(rule);
		}
		
		private void addConstantSubstitution(ConstantRule rule) {
			if(TerminalUtil.isBalanced(rule) == BalanceResult.IMBALANCED)
				return;
			
			constantSubstitutions.add(rule);
			for(SubstitutionListener listener : Lists.newArrayList(listeners))
				listener.newConstantSubstitution(rule);
		}
		
		private void substitute(NonTerminal nt) {
			final Effect1<Rule> substituteEffect = new Effect1<Rule> () {
				@Override
				public void f(Rule rule) {
					Option<Terminal> lastTerminal = TerminalUtil.lastTerminal(rule);
					if(lastTerminal.isNone() || lastTerminal.some() instanceof ProducingTerminal) {
						//producing or id rule
						addProducingSubstitution(rule);
					}
					else {
						//consuming or excluding rule
						if(rule instanceof ConstantRule)
							addConstantSubstitution((ConstantRule) rule);
						else {
							final RulePair pair = RulePair.of(rule);
							Substitution nestedSubstitution = substitutions.getOrCreate(pair.substitutablePart);
							nestedSubstitution.addListener(new SubstitutionListener() {
								@Override
								public void newProducingSubstitution(Rule rule) {
									Rule substitutedRule = rule.append(pair.terminals);
									if(TerminalUtil.isBalanced(substitutedRule) != BalanceResult.IMBALANCED)
										f(substitutedRule);
								}

								@Override
								public void newConstantSubstitution(ConstantRule rule) {
									addConstantSubstitution(rule.append(pair.terminals));
								}
							});
						}
					}
				}
			};
			nt.addListener(new NonTerminal.Listener() {
				@Override
				public void removedRule(NonTerminal nt, Rule rule) {
				}
				
				@Override
				public void addedRule(NonTerminal nt, Rule rule) {
					substituteEffect.f(rule);
				}
			});
			for(Rule rule : nt.getRules())
				substituteEffect.f(rule);
		}

		protected void addListener(SubstitutionListener listener) {
			if(listeners.isEmpty()) {
				listeners.add(listener);
				start();
			}
			else {
				listeners.add(listener);
				for(Rule r : Lists.newArrayList(producingSubstitutions))
					listener.newProducingSubstitution(r);
			}
		}
	}
	
	private static class RulePair {
	
		public final Rule substitutablePart;
		public final Terminal[] terminals;
		
		public RulePair(Rule identifier, Terminal[] terminals) {
			this.substitutablePart = identifier;
			this.terminals = terminals;
		}
		
		public static RulePair of(Rule rule) {
			return rule.accept(new RuleVisitor<RulePair>() {
				@Override
				public RulePair visit(ContextFreeRule contextFreeRule) {
					return new RulePair(new NonLinearRule(new ConstantRule(contextFreeRule.getLeftTerminals()), 
							new RegularRule(contextFreeRule.getNonTerminal())), contextFreeRule.getRightTerminals());
				}

				@Override
				public RulePair visit(NonLinearRule nonLinearRule) {
					RulePair rightResult = nonLinearRule.getRight().accept(this);
					return new RulePair(nonLinearRule.getLeft().append(rightResult.substitutablePart), rightResult.terminals);
				}

				@Override
				public RulePair visit(RegularRule regularRule) {
					return new RulePair(new RegularRule(regularRule.getNonTerminal()), regularRule.getTerminals());
				}

				@Override
				public RulePair visit(ConstantRule constantRule) {
					return new RulePair(new ConstantRule(), constantRule.getTerminals());
				}
			});
		}
	}
	
	public static interface SubstitutionListener {
//		void constant(ConstantRule constantRule);
		void newProducingSubstitution(Rule rule);

		void newConstantSubstitution(ConstantRule rule);
	}
}
