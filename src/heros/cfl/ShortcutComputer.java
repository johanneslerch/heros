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


import heros.cfl.TerminalUtil.BalanceResult;
import heros.solver.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ShortcutComputer {

	private Map<Rule, NonTerminal> intermediateNonTerminals = Maps.newHashMap();
	private Map<NonTerminal, NonTerminalContext> contexts = Maps.newHashMap();
	private TransactionalNonTerminalListener transactionalListener = new TransactionalNonTerminalListener();

	public void updateRules() {
		transactionalListener.endTransaction();
	}
	
	public NonTerminalContext resolve(NonTerminal nt) {
		if(contexts.containsKey(nt))
			return contexts.get(nt);
		
		final NonTerminalContext context = new NonTerminalContext(nt);
		contexts.put(nt, context);
		transactionalListener.addListener(nt, new NonTerminal.Listener() {
			@Override
			public void removedRule(NonTerminal nt, Rule rule) {
			}
			
			@Override
			public void addedRule(NonTerminal nt, Rule rule) {
				RuleGuard ruleGuard = new RuleGuard(rule);
				transactionalListener.addListener(nt, ruleGuard);
				resolve(context, Optional.<Rule> absent(), introduceIntermediateNonTerminalIfNecessary(rule), ruleGuard);
			}
		});
		for(Rule rule : Lists.newArrayList(nt.getRules())) {
			RuleGuard ruleGuard = new RuleGuard(rule);
			transactionalListener.addListener(nt, ruleGuard);
			resolve(context, Optional.<Rule> absent(), introduceIntermediateNonTerminalIfNecessary(rule), ruleGuard);
		}
		return context;
	}
	
	void resolve(final Context context, final Optional<Rule> condition, Rule rule, final RuleGuard guard) {
		BalanceResult balanceResult = TerminalUtil.isBalanced(rule);
		if(balanceResult == BalanceResult.BALANCED && TerminalUtil.hasTerminals(rule)) {
			context.addIncomingEdge(condition, rule, guard);
		} else if(balanceResult == BalanceResult.IMBALANCED) {
			return;
		} else {
			//more consumers or no terminal
			rule.accept(new RuleVisitor<Void>() {

				@Override
				public Void visit(final ContextFreeRule contextFreeRule) {
					NonTerminalContext incCtx = resolve(contextFreeRule.getNonTerminal());
					incCtx.addListener(new ContextListener() {
						@Override
						public void newIncomingEdge(Edge edge) {
							Rule newRule = new ConstantRule(contextFreeRule.getLeftTerminals()).append(edge.rule).append(contextFreeRule.getRightTerminals());
							resolve(context, concatenate(condition, edge.condition), newRule, guard.dependOn(edge.guard));
						}

						@Override
						public void canBeConstantConsuming(Context constantContext, Optional<Rule> innerCondition, ConstantRule rule) {
//							resolve(context, concatenate(condition, innerCondition), 
//									new ConstantRule(contextFreeRule.getLeftTerminals()).append(rule), guard);
						}
					});
					return null;
				}

				@Override
				public Void visit(final NonLinearRule nonLinearRule) {
					resolve(new Context() {
						@Override
						public void addIncomingEdge(final Optional<Rule> rightCondition, final Rule rule, RuleGuard guard) {
							if(rightCondition.isPresent()) {
								resolve(new Context() {
									@Override
									public void addIncomingEdge(Optional<Rule> condition, Rule leftRule, RuleGuard guard) {
										if(leftRule.equals(rightCondition.get()))
											context.addIncomingEdge(condition, rule, guard);
									}

									@Override
									public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> condition, ConstantRule rule) {
										//ignore here
									}
								}, Optional.<Rule> absent(), nonLinearRule.getLeft(), guard);
							}
							else {
								Rule append = nonLinearRule.getLeft().append(rule);
								if(TerminalUtil.isBalanced(append) != BalanceResult.IMBALANCED)
									context.addIncomingEdge(condition, append, guard);
							}
						}

						@Override
						public void setCanBeConstantConsuming(final Context constantContext, final Optional<Rule> rightCondition, final ConstantRule constantRule) {
							if(rightCondition.isPresent()) {
								resolve(new Context() {
									@Override
									public void addIncomingEdge(Optional<Rule> innerCondition, Rule leftRule, RuleGuard guard) {
										if(leftRule.equals(rightCondition.get()))
											context.setCanBeConstantConsuming(context, innerCondition, constantRule);
									}

									@Override
									public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> condition, ConstantRule rule) {
										//condition not satisfied...
									}
								}, Optional.<Rule> absent(), nonLinearRule.getLeft(), guard);
							}
							else {
								resolve(new Context() {
									@Override
									public void addIncomingEdge(final Optional<Rule> innerCondition, Rule rule, RuleGuard guard) {
										//drop innerCondition on purpose
										resolve(constantContext, Optional.of(rule), rule.append(constantRule.getTerminals()), guard);
									}
	
									@Override
									public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> innerCondition, ConstantRule rule) {
										if(!innerCondition.isPresent())
											context.setCanBeConstantConsuming(constantContext, condition, rule);									
									}
								}, condition, nonLinearRule.getLeft(), guard);
							}
						}
					}, Optional.<Rule> absent(), nonLinearRule.getRight(), guard);
					return null;
				}

				@Override
				public Void visit(final RegularRule regularRule) {
					NonTerminalContext incCtx = resolve(regularRule.getNonTerminal());
					incCtx.addListener(new ContextListener() {
						@Override
						public void newIncomingEdge(Edge edge) {
							resolve(context, concatenate(condition, edge.condition), edge.rule.append(regularRule.getTerminals()), guard.dependOn(edge.guard));
						}

						@Override
						public void canBeConstantConsuming(Context constantContext, Optional<Rule> innerCondition, ConstantRule rule) {
							context.setCanBeConstantConsuming(constantContext, concatenate(condition, innerCondition), rule);
						}
					});
					return null;
				}

				@Override
				public Void visit(ConstantRule constantRule) {
					context.setCanBeConstantConsuming(context, condition, constantRule);
					return null;
				}
			});
		}
	}

	private static Optional<Rule> concatenate(Optional<Rule> first, Optional<Rule> second) {
		if(first.isPresent())
			if(second.isPresent())
				return Optional.of(first.get().append(second.get()));
			else
				return first;
		else
			return second;
	}
	
	NonTerminal getIntermediateNonTerminal(NonTerminal nonTerminal, final Rule rule) {
		if(!intermediateNonTerminals.containsKey(rule)) {
			NonTerminal intermediateNonTerminal = new NonTerminal("{INTERM-"+nonTerminal.getRepresentation()+":"+rule+"}");
			intermediateNonTerminal.addRule(rule);
			intermediateNonTerminals.put(rule, intermediateNonTerminal);
		}
		return intermediateNonTerminals.get(rule);
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
	
	
	
	interface Context {
		
		public void addIncomingEdge(Optional<Rule> condition, Rule rule, RuleGuard guard);

		public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> condition, ConstantRule rule);
	}
	
	class NonTerminalContext implements Context {
		
		private Map<Pair<Optional<Rule>, Rule>, Edge> incomingEdges = Maps.newHashMap();
		private List<ContextListener> listeners = Lists.newLinkedList();
		private NonTerminal nt;
		private Set<ConstantConsumingContainer> constantRules = Sets.newHashSet();
		
		public NonTerminalContext(NonTerminal nt) {
			this.nt = nt;
		}

		@Override
		public void addIncomingEdge(Optional<Rule> condition, Rule rule, RuleGuard guard) {
			Pair<Optional<Rule>, Rule> key = new Pair<Optional<Rule>, Rule>(condition, rule);
			if(incomingEdges.containsKey(key)) {
				Edge edge = incomingEdges.get(key);
				edge.guard.addAlternative(guard);
			}
			else {
				Edge edge = new Edge(condition, rule, guard);
				System.out.println(nt + " -> "+edge);
				incomingEdges.put(key, edge);
				for(ContextListener listener : Lists.newArrayList(listeners))
					listener.newIncomingEdge(edge);
			}
		}
		
		@Override
		public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> condition, ConstantRule rule) {
			if(constantRules.add(new ConstantConsumingContainer(constantContext, condition, rule))) {
				String condString = condition.isPresent() ? "["+condition.get()+"] " : "";
				System.out.println(nt + " can be constant consuming via context ("+constantContext+"): "+condString+rule);
				for(ContextListener listener : Lists.newArrayList(listeners)) 
					listener.canBeConstantConsuming(constantContext, condition, rule);
			}
		}
		
		public void addListener(ContextListener ctxListener) {
			listeners.add(ctxListener);
			for(Edge edge : Lists.newArrayList(incomingEdges.values()))
				ctxListener.newIncomingEdge(edge);
			
			for(ConstantConsumingContainer container : Lists.newArrayList(constantRules))
				ctxListener.canBeConstantConsuming(container.constantContext, container.condition, container.rule);
		}
		
		@Override
		public String toString() {
			return nt.toString();
		}
	}
	
	private static class ConstantConsumingContainer {

		private Context constantContext;
		private Optional<Rule> condition;
		private ConstantRule rule;

		public ConstantConsumingContainer(Context constantContext, Optional<Rule> condition, ConstantRule rule) {
			this.constantContext = constantContext;
			this.condition = condition;
			this.rule = rule;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((condition == null) ? 0 : condition.hashCode());
			result = prime * result + ((constantContext == null) ? 0 : constantContext.hashCode());
			result = prime * result + ((rule == null) ? 0 : rule.hashCode());
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
			ConstantConsumingContainer other = (ConstantConsumingContainer) obj;
			if (condition == null) {
				if (other.condition != null)
					return false;
			} else if (!condition.equals(other.condition))
				return false;
			if (constantContext == null) {
				if (other.constantContext != null)
					return false;
			} else if (!constantContext.equals(other.constantContext))
				return false;
			if (rule == null) {
				if (other.rule != null)
					return false;
			} else if (!rule.equals(other.rule))
				return false;
			return true;
		}
	}
	
	static interface ContextListener {
		void newIncomingEdge(Edge edge);

		void canBeConstantConsuming(Context constantContext, Optional<Rule> condition, ConstantRule rule);
	}
	
	static class Edge {
		private Rule rule;
		private Optional<Rule> condition;
		private RuleGuard guard;

		public Edge(Optional<Rule> condition, Rule rule, RuleGuard guard) {
			this.condition = condition;
			this.rule = rule;
			this.guard = guard;
		}

		public Rule getRule() {
			return rule;
		}
		
		@Override
		public String toString() {
			String result = "";
			if(condition.isPresent())
				result +="["+condition.get()+"] ";
			return result+rule;
		}

		public Optional<Rule> getCondition() {
			return condition;
		}
	}
}
