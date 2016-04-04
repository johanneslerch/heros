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
import heros.utilities.KeyTuple;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import heros.cfl.Guard.*;

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
	
	void resolve(final Context context, final Optional<Rule> condition, Rule rule, final Guard guard) {
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
							if(edge.condition.isPresent()) {
								if(edge.condition.get().equals(new ConstantRule(contextFreeRule.getLeftTerminals()))) {
									Rule newRule = edge.rule.append(contextFreeRule.getRightTerminals());
									resolve(context, condition, newRule, guard.dependOn(edge.guard));
								}
							}
							else {
								Rule newRule = new ConstantRule(contextFreeRule.getLeftTerminals()).append(edge.rule).append(contextFreeRule.getRightTerminals());
								resolve(context, concatenate(condition, edge.condition), newRule, guard.dependOn(edge.guard));
							}
						}

						@Override
						public void canBeConstantConsuming(ConstantConsumingContainer constantContainer) {
							if(!condition.isPresent()) {
								resolve(context, concatenate(condition, constantContainer.condition), 
										new ConstantRule(contextFreeRule.getLeftTerminals()).append(constantContainer.rule).append(contextFreeRule.getRightTerminals()), 
										guard.dependOn(constantContainer.guard));
							}
						}
					});
					return null;
				}

				@Override
				public Void visit(final NonLinearRule nonLinearRule) {
					resolve(new Context() {
						@Override
						public void addIncomingEdge(final Optional<Rule> rightCondition, final Rule rule, Guard guard) {
							if(rightCondition.isPresent()) {
								resolve(new Context() {
									@Override
									public void addIncomingEdge(Optional<Rule> innerCondition, Rule leftRule, Guard guard) {
										if(leftRule.equals(rightCondition.get()))
											context.addIncomingEdge(concatenate(condition, innerCondition), rule, guard);
									}

									@Override
									public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> condition, ConstantRule rule, Guard guard) {
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
						public void setCanBeConstantConsuming(final Context constantContext, final Optional<Rule> rightCondition, final ConstantRule constantRule, Guard guard) {
							if(rightCondition.isPresent()) {
								resolve(new Context() {
									@Override
									public void addIncomingEdge(Optional<Rule> innerCondition, Rule leftRule, Guard guard) {
										if(leftRule.equals(rightCondition.get()))
											context.setCanBeConstantConsuming(context, innerCondition, constantRule, guard);
									}

									@Override
									public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> condition, ConstantRule rule, Guard guard) {
										//condition not satisfied...
									}
								}, Optional.<Rule> absent(), nonLinearRule.getLeft(), guard);
							}
							else {
								resolve(new Context() {
									@Override
									public void addIncomingEdge(final Optional<Rule> innerCondition, Rule rule, Guard guard) {
										//drop innerCondition on purpose
										resolve(constantContext, Optional.of(rule), rule.append(constantRule.getTerminals()), guard);
									}
	
									@Override
									public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> innerCondition, ConstantRule rule, Guard guard) {
										if(!innerCondition.isPresent())
											context.setCanBeConstantConsuming(constantContext, condition, rule, guard);									
									}
								}, condition, nonLinearRule.getLeft(), guard);
							}
						}
					}, Optional.<Rule> absent(), nonLinearRule.getRight(), guard);
					return null;
				}

				@Override
				public Void visit(final RegularRule regularRule) {
					final NonTerminalContext incCtx = resolve(regularRule.getNonTerminal());
					incCtx.addListener(new ContextListener() {
						@Override
						public void newIncomingEdge(Edge edge) {
							NonTerminalContext ctx = incCtx;
							resolve(context, concatenate(condition, edge.condition), edge.rule.append(regularRule.getTerminals()), guard.dependOn(edge.guard));
						}

						@Override
						public void canBeConstantConsuming(ConstantConsumingContainer constantContainer) {
							if(!condition.isPresent())
								context.setCanBeConstantConsuming(constantContainer.constantContext, concatenate(condition, constantContainer.condition),
										constantContainer.rule, guard.dependOn(constantContainer.guard));
						}
					});
					return null;
				}

				@Override
				public Void visit(ConstantRule constantRule) {
					context.setCanBeConstantConsuming(context, condition, constantRule, guard);
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
		
		public void addIncomingEdge(Optional<Rule> condition, Rule rule, Guard guard);

		public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> condition, ConstantRule rule, Guard guard);
	}
	
	class NonTerminalContext implements Context {
		
		private Map<Pair<Optional<Rule>, Rule>, Edge> incomingEdges = Maps.newHashMap();
		private List<ContextListener> listeners = Lists.newLinkedList();
		private NonTerminal nt;
		private Map<KeyTuple, ConstantConsumingContainer> constantRules = Maps.newHashMap();
		
		public NonTerminalContext(NonTerminal nt) {
			this.nt = nt;
		}

		@Override
		public void addIncomingEdge(Optional<Rule> condition, Rule rule, Guard guard) {
			if(!guard.isStillPossible())
				return;
			
			Pair<Optional<Rule>, Rule> key = new Pair<Optional<Rule>, Rule>(condition, rule);
			boolean createNewEdge = true;
			if(incomingEdges.containsKey(key)) {
				Edge edge = incomingEdges.get(key);
				if(edge.guard.isStillPossible()) {
					edge.guard.addAlternative(guard);
					createNewEdge = false;
				}
			}
			if(createNewEdge) {
				Edge edge = new Edge(condition, rule, guard);
//				System.err.println(nt + " -> "+edge);
				incomingEdges.put(key, edge);
				for(ContextListener listener : Lists.newArrayList(listeners))
					listener.newIncomingEdge(edge);
			}
		}
		
		@Override
		public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> condition, ConstantRule rule, Guard guard) {
			if(!guard.isStillPossible())
				return;
			
			KeyTuple key = new KeyTuple(constantContext, condition, rule);
			boolean createNew = true;
			if(constantRules.containsKey(key)) {
				ConstantConsumingContainer container = constantRules.get(key);
				if(container.guard.isStillPossible()) {
					container.guard.addAlternative(guard);
					createNew = false;
				}
			}
			if(createNew) {
				ConstantConsumingContainer container = new ConstantConsumingContainer(constantContext, condition, rule, guard);
//				String condString = condition.isPresent() ? "["+condition.get()+"] " : "";
//				System.err.println(nt + " can be constant consuming via context ("+constantContext+"): "+condString+rule);
				constantRules.put(key, container);
				for(ContextListener listener : Lists.newArrayList(listeners)) 
					listener.canBeConstantConsuming(container);
			}
		}
		
		public void addListener(ContextListener ctxListener) {
			listeners.add(ctxListener);
			for(Entry<Pair<Optional<Rule>, Rule>, Edge> edge : Lists.newArrayList(incomingEdges.entrySet()))
				if(edge.getValue().guard.isStillPossible())
					ctxListener.newIncomingEdge(edge.getValue());
				else 
					incomingEdges.remove(edge.getKey());
			
			for(Entry<KeyTuple, ConstantConsumingContainer> container : Lists.newArrayList(constantRules.entrySet()))
				if(container.getValue().guard.isStillPossible())
					ctxListener.canBeConstantConsuming(container.getValue());
				else
					constantRules.remove(container.getKey());
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
		private Guard guard;

		public ConstantConsumingContainer(Context constantContext, Optional<Rule> condition, ConstantRule rule, Guard guard) {
			this.constantContext = constantContext;
			this.condition = condition;
			this.rule = rule;
			this.guard = guard;
		}
	}
	
	static interface ContextListener {
		void newIncomingEdge(Edge edge);

		void canBeConstantConsuming(ConstantConsumingContainer constantContainer);
	}
	
	static class Edge {
		private Rule rule;
		private Optional<Rule> condition;
		private Guard guard;

		public Edge(Optional<Rule> condition, Rule rule, Guard guard) {
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
