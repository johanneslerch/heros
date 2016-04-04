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

import static heros.cfl.TerminalUtil.BalanceResult.BALANCED;
import static heros.cfl.TerminalUtil.BalanceResult.IMBALANCED;
import fj.function.Effect2;
import heros.cfl.IntersectionSolver.QueryListener;
import heros.cfl.TerminalUtil.BalanceResult;
import heros.solver.Pair;
import heros.utilities.DefaultValueMap;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class IntersectionSolver {

	private Map<Rule, NonTerminal> intermediateNonTerminals = Maps.newHashMap();
	private DefaultValueMap<Rule, Query> queries = new DefaultValueMap<Rule, Query>() {
		@Override
		protected Query createItem(Rule key) {
			return new Query(key);
		}
	};
	private DefaultValueMap<Rule, ConstantCheckQuery> constantCheckQueries = new DefaultValueMap<Rule, ConstantCheckQuery>() {
		@Override
		protected ConstantCheckQuery createItem(Rule key) {
			return new ConstantCheckQuery(key);
		}
	};
	private DefaultValueMap<Pair<NonTerminal,Rule>, ReduceToCallingContextQuery> reduceToCallingContextQueries = new DefaultValueMap<Pair<NonTerminal,Rule>, ReduceToCallingContextQuery>() {
		@Override
		protected ReduceToCallingContextQuery createItem(Pair<NonTerminal,Rule> key) {
			return new ReduceToCallingContextQuery(key.getO1(), key.getO2());
		}
	};
	private DefaultValueMap<SubstitutionKey, Substitution> substitutions = new DefaultValueMap<SubstitutionKey, Substitution>() {
		@Override
		protected Substitution createItem(SubstitutionKey key) {
			return new Substitution(key);
		}
	};
	private TransactionalNonTerminalListener transactionalListener = new TransactionalNonTerminalListener();

	public void updateRules() {
		transactionalListener.endTransaction();
	}
	
	public void query(Rule rule, QueryListener listener) {
		queries.getOrCreate(rule).addListener(listener);
	}
	
	public void substitute(Rule rule, SubstitutionListener listener) {
		substitutions.getOrCreate(new SubstitutionKey(new ConstantRule(), rule)).addListener(listener);
	}

	private void substitute(SubstitutionKey key, SubstitutionListener listener) {
		substitutions.getOrCreate(key).addListener(listener);
	}
	
	public void constantCheck(Rule rule, final QueryListener listener) {
		constantCheckQueries.getOrCreate(rule).addListener(listener);
	}

	public void reduceToCallingContext(NonTerminal callingCtx, Rule rule, QueryListener queryListener) {
		reduceToCallingContextQueries.getOrCreate(new Pair<NonTerminal, Rule>(callingCtx, new RegularRule(callingCtx).append(rule))).addListener(queryListener);
	}
	
	private class ConstantCheckQuery extends AbstractQuery {

		public ConstantCheckQuery(Rule queryRule) {
			super(IntersectionSolver.this, queryRule);
		}
		
		@Override
		protected boolean isSolution(Rule rule) {
			return rule instanceof ConstantRule && TerminalUtil.isBalanced(rule) == BALANCED;
		}

		@Override
		protected boolean allowsSubQuery(Rule rule) {
			return !(rule instanceof ConstantRule) && TerminalUtil.isBalanced(rule) != BalanceResult.IMBALANCED;
		}

		@Override
		protected AbstractQuery triggerSubQuery(Rule rule) {
			return constantCheckQueries.getOrCreate(TerminalUtil.removeTrailingProductions(rule));
		}
		
	}
	
	private class ReduceToCallingContextQuery extends AbstractQuery {

		private NonTerminal callingContext;

		public ReduceToCallingContextQuery(NonTerminal callingContext, Rule queryRule) {
			super(IntersectionSolver.this, queryRule);
			this.callingContext = callingContext;
		}

		@Override
		protected boolean isSolution(Rule rule) {
			Set<NonTerminal> nonTerminals = rule.accept(new CollectNonTerminalsRuleVisitor());
			return (!nonTerminals.contains(callingContext) || nonTerminals.size()==1) && TerminalUtil.isBalanced(rule) == BALANCED;
		}

		@Override
		protected boolean allowsSubQuery(Rule rule) {
			return !(rule instanceof ConstantRule) && TerminalUtil.isBalanced(rule) != IMBALANCED;
		}

		@Override
		protected AbstractQuery triggerSubQuery(Rule rule) {
			return reduceToCallingContextQueries.getOrCreate(new Pair<NonTerminal, Rule>(callingContext, TerminalUtil.removeTrailingProductions(rule)));
		}
		
	}
	
	private class Query extends AbstractQuery {
		
		public Query(Rule queryRule) {
			super(IntersectionSolver.this, queryRule);
		}
		
		@Override
		protected boolean isSolution(Rule rule) {
			return TerminalUtil.isBalanced(rule) == BALANCED;
		}

		@Override
		protected boolean allowsSubQuery(Rule rule) {
			return !(rule instanceof ConstantRule) && TerminalUtil.isBalanced(rule) != BalanceResult.IMBALANCED;
		}
		
		@Override
		protected AbstractQuery triggerSubQuery(Rule r) {
			return queries.getOrCreate(r);
		}
	}
	
	private static abstract class AbstractQuery implements QueryListener {
		
		private boolean solved = false;
		private List<QueryListener> listeners = Lists.newLinkedList();
		private RulePair pair;
		private IntersectionSolver solver;
		private Rule queryRule;
		
		public AbstractQuery(IntersectionSolver solver, Rule queryRule) {
			this.solver = solver;
			this.queryRule = queryRule;
			this.pair = RulePair.of(queryRule);
		}
		
		public void start() {
			if(isSolution(queryRule)) {
				solved();
				return;
			}
			if(!allowsSubQuery(queryRule))
				return;
			
			solver.substitute(pair.substitutablePart, new SubstitutionListener() {
				@Override
				public void newProducingSubstitution(Rule rule, Guard guard) {
					if(solved)
						return;
					
					Rule append = rule.append(pair.terminals);
					if(isSolution(append)) {
						solved();
					}
					else if(allowsSubQuery(append)){
						AbstractQuery subQuery = triggerSubQuery(append);
						if(subQuery != AbstractQuery.this)
							subQuery.addListener(AbstractQuery.this);
					}
				}
			});
		}
		
		protected abstract boolean isSolution(Rule rule);
		protected abstract boolean allowsSubQuery(Rule rule);
		protected abstract AbstractQuery triggerSubQuery(Rule rule);
		
		protected void addListener(QueryListener listener) {
			if(solved)
				listener.solved();
			else if(listeners.isEmpty()) {
				listeners.add(listener);
				start();
			}
			else {
				listeners.add(listener);
			}
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

	public static interface QueryListener {
		void solved();
	}
	
	private static class SubstitutionKey {
		private Rule prefix;
		private Rule substitutableRule;

		public SubstitutionKey(Rule prefix, Rule substitutableRule) {
			this.prefix = prefix;
			this.substitutableRule = substitutableRule;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
			result = prime * result + ((substitutableRule == null) ? 0 : substitutableRule.hashCode());
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
			SubstitutionKey other = (SubstitutionKey) obj;
			if (prefix == null) {
				if (other.prefix != null)
					return false;
			} else if (!prefix.equals(other.prefix))
				return false;
			if (substitutableRule == null) {
				if (other.substitutableRule != null)
					return false;
			} else if (!substitutableRule.equals(other.substitutableRule))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return prefix+";"+substitutableRule;
		}
	}
	
	NonTerminal getIntermediateNonTerminal(NonTerminal nonTerminal, final Rule rule) {
		if(!intermediateNonTerminals.containsKey(rule)) {
			NonTerminal intermediateNonTerminal = new NonTerminal("{INTERM-"+nonTerminal.getRepresentation()+":"+rule+"}");
			intermediateNonTerminal.addRule(rule);
			intermediateNonTerminals.put(rule, intermediateNonTerminal);
		}
		return intermediateNonTerminals.get(rule);
	}
	
	private class Substitution {

		private List<SubstitutionListener> listeners = Lists.newLinkedList();
		private Map<Rule, Guard> producingSubstitutions = Maps.newHashMap();
		private SubstitutionKey key;
		
		public Substitution(SubstitutionKey key) {
			this.key = key;
		}

		private void start() {
			System.out.println("Interested in Substitutions for "+key);
			key.substitutableRule.accept(new RuleVisitor<Void>() {
				@Override
				public Void visit(final ContextFreeRule contextFreeRule) {
					assert contextFreeRule.getRightTerminals().length == 0;
					Substitution substitution = substitutions.getOrCreate(new SubstitutionKey(new ConstantRule(contextFreeRule.getLeftTerminals()),
							new RegularRule(contextFreeRule.getNonTerminal())));
					substitution.addListener(new SubstitutionListener() {
						@Override
						public void newProducingSubstitution(Rule rule, Guard guard) {
							addProducingSubstitution(rule, guard);
						}
					});
					return null;
				}

				@Override
				public Void visit(final NonLinearRule nonLinearRule) {
					Substitution substitution = substitutions.getOrCreate(new SubstitutionKey(key.prefix.append(nonLinearRule.getLeft()), nonLinearRule.getRight()));
					substitution.addListener(new SubstitutionListener() {
						@Override
						public void newProducingSubstitution(Rule rule, Guard guard) {
							addProducingSubstitution(rule, guard);
						}
					});
					return null;
				}

				@Override
				public Void visit(final RegularRule regularRule) {
					assert regularRule.getTerminals().length == 0;
					substitute(regularRule.getNonTerminal());
					return null;
				}

				@Override
				public Void visit(ConstantRule constantRule) {
					throw new IllegalStateException();
				}
			});
		}
		
		private void addProducingSubstitution(Rule rule, Guard guard) {
			if(TerminalUtil.isBalanced(rule) == BalanceResult.IMBALANCED || !guard.isStillPossible())
				return;
			if(producingSubstitutions.containsKey(rule)) {
				producingSubstitutions.get(rule).addAlternative(guard);
			}
			else {
				producingSubstitutions.put(rule, guard);
//				System.out.println("new substitution for "+key+" -> "+rule);
				for(SubstitutionListener listener : Lists.newArrayList(listeners)) {
					listener.newProducingSubstitution(rule, guard);
				}
			}
		}
		
		private void substitute(NonTerminal nt) {
			forAllRules(nt, createEffect(key.prefix));
		}
		
		private Rule introduceIntermediateNonTerminalIfNecessary(Rule rule) {
			return rule.accept(new RuleVisitor<Rule>() {
				@Override
				public Rule visit(ContextFreeRule contextFreeRule) {
					Pair<Terminal[],Terminal[]> split = TerminalUtil.split(contextFreeRule.getRightTerminals());
					if(split.getO1().length == 0 || split.getO2().length == 0)
						return contextFreeRule;
					else {
						NonTerminal intermediateNonTerminal = getIntermediateNonTerminal(contextFreeRule.getNonTerminal(), new ContextFreeRule(contextFreeRule.getLeftTerminals(), contextFreeRule.getNonTerminal(), split.getO1()));
						return new RegularRule(intermediateNonTerminal, split.getO2());
					}
				}

				@Override
				public Rule visit(NonLinearRule nonLinearRule) {
					return new NonLinearRule(nonLinearRule.getLeft(), nonLinearRule.getRight().accept(this));
				}

				@Override
				public Rule visit(RegularRule regularRule) {
					Pair<Terminal[],Terminal[]> split = TerminalUtil.split(regularRule.getTerminals());
					if(split.getO1().length == 0 || split.getO2().length == 0)
						return regularRule;
					else {
						NonTerminal intermediateNonTerminal = getIntermediateNonTerminal(regularRule.getNonTerminal(), new RegularRule(regularRule.getNonTerminal(), split.getO1()));
						return new RegularRule(intermediateNonTerminal, split.getO2());
					}
				}

				@Override
				public Rule visit(ConstantRule constantRule) {
					return constantRule;
				}
			});
		}
		
		private Effect2<Rule, Guard> createEffect(final Rule prefix) {
			return new Effect2<Rule, Guard> () {
				@Override
				public void f(final Rule rule, final Guard guard) {
					Rule prefixedRule = prefix.append(rule);
					BalanceResult balanceTest = TerminalUtil.isBalanced(prefixedRule);
					if(balanceTest == BALANCED && TerminalUtil.hasTerminals(prefixedRule))
						addProducingSubstitution(prefixedRule, guard);
					else if(balanceTest == BalanceResult.IMBALANCED)
						return;
					else { //more consumers or no terminal
						if(rule instanceof ConstantRule) {
							if(!(prefixedRule instanceof ConstantRule)) {
								final RulePair pair = RulePair.of(prefixedRule);
								IntersectionSolver.this.substitute(pair.substitutablePart, new SubstitutionListener() {
									@Override
									public void newProducingSubstitution(Rule rule, Guard locGuard) {
										createEffect(new ConstantRule()).f(rule.append(pair.terminals), guard.dependOn(locGuard));
									}
								});
							}
						} else {
							final RulePair pair = RulePair.of(rule);
							IntersectionSolver.this.substitute(new SubstitutionKey(prefix, pair.substitutablePart), new SubstitutionListener() {
								@Override
								public void newProducingSubstitution(Rule rule, Guard locGuard) {
									createEffect(new ConstantRule()).f(rule.append(pair.terminals), guard.dependOn(locGuard));
								}
							});
						}
					}
				}
			};
		}

		private void forAllRules(NonTerminal nt, final Effect2<Rule, Guard> substituteEffect) {
			transactionalListener.addListener(nt, new NonTerminal.Listener() {
				@Override
				public void removedRule(NonTerminal nt, Rule rule) {
				}
				
				@Override
				public void addedRule(NonTerminal nt, Rule rule) {
					RuleGuard ruleGuard = new RuleGuard(rule);
					transactionalListener.addListener(nt, ruleGuard);
					substituteEffect.f(introduceIntermediateNonTerminalIfNecessary(rule), ruleGuard);
				}
			});
			for(Rule rule : Lists.newArrayList(nt.getRules())) {
				RuleGuard ruleGuard = new RuleGuard(rule);
				transactionalListener.addListener(nt, ruleGuard);
				substituteEffect.f(introduceIntermediateNonTerminalIfNecessary(rule), ruleGuard);
			}
		}

		protected void addListener(SubstitutionListener listener) {
			if(listeners.isEmpty()) {
				listeners.add(listener);
				start();
			}
			else {
				listeners.add(listener);
				for(Entry<Rule, Guard> r : Lists.newArrayList(producingSubstitutions.entrySet())) {
					if(r.getValue().isStillPossible())
						listener.newProducingSubstitution(r.getKey(), r.getValue());
					else {
//						System.err.println("Substitution no longer possible: "+key+" -> "+r.getKey());
						producingSubstitutions.remove(r.getKey());
					}
				}
			}
		}
	}
	
	public static interface Guard {
		public boolean isStillPossible();
		public Guard addAlternative(Guard guard);
		public Guard dependOn(Guard g);
	}
	
	public static class RuleGuard implements Guard, NonTerminal.Listener {

		private Rule guardedRule;
		private boolean isPossible = true;

		public RuleGuard(Rule rule) {
			this.guardedRule = rule;
		}

		public boolean isStillPossible() {
			return isPossible;
		}

		public Guard addAlternative(Guard guard) {
			return new AlternativeGuard(this, guard);
		}

		public Guard dependOn(Guard g) {
			return new DependentGuard(this, g);
		}

		@Override
		public void addedRule(NonTerminal nt, Rule rule) {
		}

		@Override
		public void removedRule(NonTerminal nt, Rule rule) {
			if(guardedRule.equals(rule)) {
				isPossible = false;
				nt.removeListener(this);
			}
		}		
	}
	
	private static class DependentGuard implements Guard {

		private boolean isPossible = true;
		private Guard[] guards;

		public DependentGuard(Guard... guards) {
			this.guards = guards;
		}

		@Override
		public boolean isStillPossible() {
			if(!isPossible)
				return false;
			for(Guard guard : guards) {
				if(!guard.isStillPossible()) {
					isPossible = false;
					guards = null;
					return false;
				}
			}
			return true;
		}

		@Override
		public Guard addAlternative(Guard guard) {
			return new AlternativeGuard(this, guard);
		}

		@Override
		public Guard dependOn(Guard g) {
			Guard[] newGuards = Arrays.copyOf(guards, guards.length+1);
			newGuards[guards.length] = g;
			return new DependentGuard(newGuards);
		}
	}
	
	private static class AlternativeGuard implements Guard {

		private List<Guard> guards;
		
		public AlternativeGuard(Guard...guards) {
			assert guards.length > 0;
			this.guards = Lists.newArrayList(guards);
		}
		
		@Override
		public boolean isStillPossible() {
			Iterator<Guard> it = guards.iterator();
			while(it.hasNext()) {
				Guard current = it.next();
				if(current.isStillPossible())
					return true;
				else
					it.remove();
			}
			return false;
		}

		@Override
		public Guard addAlternative(Guard guard) {
			this.guards.add(guard);
			return this;
		}

		@Override
		public Guard dependOn(Guard g) {
			return new DependentGuard(this, g);
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
		void newProducingSubstitution(Rule rule, Guard guard);
	}

}
