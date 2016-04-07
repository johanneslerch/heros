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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
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
								resolve(context, concatenate(edge.condition, condition), newRule, guard.dependOn(edge.guard));
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
					resolve(new NonLinearRuleRightContext(context, condition, nonLinearRule), Optional.<Rule> absent(), nonLinearRule.getRight(), guard);
					return null;
				}

				@Override
				public Void visit(final RegularRule regularRule) {
					final NonTerminalContext incCtx = resolve(regularRule.getNonTerminal());
					incCtx.addListener(new ContextListener() {
						@Override
						public void newIncomingEdge(Edge edge) {
							NonTerminalContext ctx = incCtx;
							resolve(context, concatenate(edge.condition, condition), edge.rule.append(regularRule.getTerminals()), guard.dependOn(edge.guard));
						}

						@Override
						public void canBeConstantConsuming(ConstantConsumingContainer constantContainer) {
							context.setCanBeConstantConsuming(constantContainer.constantContext, concatenate(condition, constantContainer.condition),
									constantContainer.rule, guard.dependOn(constantContainer.guard));
						}
					});
					return null;
				}

				@Override
				public Void visit(ConstantRule constantRule) {
					context.setCanBeConstantConsuming(context, concatenate(condition, condition), constantRule, guard);
					return null;
				}
			});
		}
	}
	
	private static String conditionToString(Optional<Rule> condition) {
		if(condition.isPresent())
			return "["+condition.get()+"] ";
		else
			return "";
	}
	
	private abstract class RuleSuffixCheckContext implements Context {

		private Rule condition;
		private NonLinearRule nonLinearRule;

		private RuleSuffixCheckContext(NonLinearRule nonLinearRule, Rule condition) {
			this.nonLinearRule = nonLinearRule;
			this.condition = condition;
		}
		
		@Override
		public void addIncomingEdge(Optional<Rule> innerCondition, Rule leftRule, Guard guard) {
			SuffixChecker suffixChecker = new SuffixChecker(condition, leftRule);
			if(suffixChecker.isSuffix())
				conditionSatisfied(concatenate(innerCondition, suffixChecker.getPrefix()), leftRule, guard);
		}
		
		protected abstract void conditionSatisfied(Optional<Rule> prefixCondition, Rule satisfiedSuffix, Guard guard);

		@Override
		public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> condition, ConstantRule rule, Guard guard) {
			//ignore here
		}
		
		@Override
		public String toString() {
			return "<RuleSuffixCheckContext for left of "+nonLinearRule+", condition: "+condition+">";
		}
	}
	
	private class NonLinearRuleRightContext implements Context {

		private Context enclosingContext;
		private NonLinearRule nonLinearRule;
		private Optional<Rule> enclosingCondition;

		public NonLinearRuleRightContext(Context enclosingContext, Optional<Rule> enclosingCondition, NonLinearRule nonLinearRule) {
			this.enclosingContext = enclosingContext;
			this.enclosingCondition = enclosingCondition;
			this.nonLinearRule = nonLinearRule;
		}
		
		@Override
		public String toString() {
			return "<Right of "+nonLinearRule+"; Enclosing Context: "+conditionToString(enclosingCondition) + enclosingContext+">";
		}

		@Override
		public void addIncomingEdge(final Optional<Rule> rightCondition, final Rule rule, final Guard guard) {
			//System.out.println(this+": is producing: "+conditionToString(rightCondition)+rule);
			if(rightCondition.isPresent()) {
				resolve(new RuleSuffixCheckContext(nonLinearRule, rightCondition.get()) {
					@Override
					protected void conditionSatisfied(Optional<Rule> condition, Rule satisfiedSuffix, Guard condGuard) {
//						System.out.println("Left side of "+nonLinearRule+" satisfies condition "+rightCondition+
//								" yielding new edge "+conditionToString(condition)+rule+" to "+enclosingContext);
						enclosingContext.addIncomingEdge(condition, rule, guard.dependOn(condGuard));
					}
				}, Optional.<Rule> absent(), nonLinearRule.getLeft(), guard);
			}
			else {
				Rule append = nonLinearRule.getLeft().append(rule);
				if(TerminalUtil.isBalanced(append) != BalanceResult.IMBALANCED)
					enclosingContext.addIncomingEdge(enclosingCondition, append, guard);
			}
		}

		@Override
		public void setCanBeConstantConsuming(final Context constantContext, final Optional<Rule> rightCondition, final ConstantRule constantRule, Guard guard) {
			//System.out.println(this+": can be constant consuming: "+conditionToString(rightCondition)+constantContext+", rule: "+constantRule);
			if(rightCondition.isPresent()) {
				resolve(new RuleSuffixCheckContext(nonLinearRule, rightCondition.get()) {
					@Override
					protected void conditionSatisfied(final Optional<Rule> innerCondition, final Rule satisfiedSuffix, Guard guard) {
						//System.out.println("Left side of "+nonLinearRule+" satisfies of "+rightCondition.get()+" the suffix "+satisfiedSuffix+" with condition "+innerCondition);
						enclosingContext.setCanBeConstantConsuming(new Context() {
							@Override
							public void addIncomingEdge(Optional<Rule> condition, Rule rule, Guard guard) {
//								System.out.println("incoming edge "+conditionToString(condition)+rule+", adding to constant context "+constantContext+
//										" with condition "+conditionToString(concatenate(condition, innerCondition)));
								//do not use rightCondition here ?!
								constantContext.addIncomingEdge(concatenate(condition, innerCondition), rule, guard);
							}

							@Override
							public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> condition,
									ConstantRule rule, Guard guard) {
								// ignore here
							}
							
						}, Optional.<Rule> absent(), constantRule, guard);
					}
				}, Optional.<Rule> absent(), nonLinearRule.getLeft(), guard);
			}
			else {
				resolve(new NonLinearRuleLeftContext(enclosingContext, enclosingCondition, nonLinearRule, constantContext, constantRule),
						Optional.<Rule>absent(), nonLinearRule.getLeft(), guard);
			}
		}
	}
	
	private class NonLinearRuleLeftContext implements Context {
		
		private NonLinearRule nonLinearRule;
		private Context constantContext;
		private Rule constantRule;
		private Context enclosingContext;
		private Optional<Rule> enclosingCondition;

		public NonLinearRuleLeftContext(Context enclosingContext, Optional<Rule> enclosingCondition, NonLinearRule nonLinearRule, Context constantContext, Rule constantRule) {
			this.enclosingContext = enclosingContext;
			this.enclosingCondition = enclosingCondition;
			this.nonLinearRule = nonLinearRule;
			this.constantContext = constantContext;
			this.constantRule = constantRule;
		}
		
		@Override
		public void addIncomingEdge(final Optional<Rule> innerCondition, Rule rule, Guard guard) {
			//System.out.println(this+" is producing: "+conditionToString(innerCondition)+rule);
			Pair<Rule,Rule> pair = splitLeftAndProducingRight(rule);
			if(pair.getO1().isEmpty())
				//TODO include innerCondition?
				resolve(constantContext, Optional.of(rule), pair.getO2().append(constantRule.getTerminals()), guard);
			else {
				Rule newRight = pair.getO2().append(nonLinearRule.getRight());
				//System.out.println("Rewriting ("+rule+","+nonLinearRule.getRight()+") to ("+pair.getO1()+","+newRight+")");
				resolve(enclosingContext, enclosingCondition, new NonLinearRule(pair.getO1(), newRight), guard);
			}
		}

		@Override
		public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> innerCondition, ConstantRule rule, Guard guard) {
			//System.out.println(this+" can be constant consuming: "+conditionToString(innerCondition)+rule+" via context "+constantContext);
			if(!innerCondition.isPresent()) //TODO wrong?
				enclosingContext.setCanBeConstantConsuming(constantContext, enclosingCondition, rule, guard);		
		}
		
		@Override
		public String toString() {
			return "<Left of "+nonLinearRule+"; Enclosing Context: "+conditionToString(enclosingCondition) + enclosingContext+">";
		}
	}
	
	private Pair<Rule, Rule> splitLeftAndProducingRight(Rule rule) {
		return rule.accept(new RuleVisitor<Pair<Rule, Rule>>() {
			@Override
			public Pair<Rule, Rule> visit(ContextFreeRule contextFreeRule) {
				return new Pair<Rule, Rule>(new ConstantRule(), contextFreeRule);
			}

			@Override
			public Pair<Rule, Rule> visit(NonLinearRule nonLinearRule) {
				Pair<Rule,Rule> pair = nonLinearRule.getRight().accept(this);
				return new Pair<Rule, Rule>(nonLinearRule.getLeft().append(pair.getO1()), pair.getO2());
			}

			@Override
			public Pair<Rule, Rule> visit(RegularRule regularRule) {
				return new Pair<Rule, Rule>(new ConstantRule(), regularRule);
			}

			@Override
			public Pair<Rule, Rule> visit(ConstantRule constantRule) {
				return new Pair<Rule, Rule>(new ConstantRule(), constantRule);
			}
		});
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
		
		private Multimap<Rule, Edge> incomingEdges = HashMultimap.create();
		private List<ContextListener> listeners = Lists.newLinkedList();
		private NonTerminal nt;
		private Multimap<KeyTuple, ConstantConsumingContainer> constantRules = HashMultimap.create();
		
		public NonTerminalContext(NonTerminal nt) {
			this.nt = nt;
		}

		@Override
		public void addIncomingEdge(Optional<Rule> condition, Rule rule, Guard guard) {
			if(!guard.isStillPossible())
				return;
			
			boolean createNewEdge = true;
			if(incomingEdges.containsKey(rule)) {
				Collection<Edge> edges = incomingEdges.get(rule);
				for(Edge edge : Lists.newLinkedList(edges)) {
					if(edge.condition.isPresent()) {
						if(condition.isPresent()) {
							if(edge.condition.equals(condition) && edge.guard.isStillPossible()) {
								edge.guard.addAlternative(guard);
								createNewEdge = false;
							}
							else if(!new SuffixChecker(edge.condition.get(), condition.get()).isSuffix()) {
								createNewEdge = !edge.guard.isStillPossible();
							}
						}
					}
					else if(!condition.isPresent() && edge.guard.isStillPossible()) {
						edge.guard.addAlternative(guard);
						createNewEdge = false;
					}
				}
			}
			if(createNewEdge) {
				Edge edge = new Edge(condition, rule, guard);
				//System.out.println(nt + " -> "+edge);
				incomingEdges.put(rule, edge);
				for(ContextListener listener : Lists.newArrayList(listeners))
					listener.newIncomingEdge(edge);
			}
		}
		
		@Override
		public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> condition, ConstantRule rule, Guard guard) {
			if(!guard.isStillPossible())
				return;
			
			boolean createNewEdge = true;
			KeyTuple key = new KeyTuple(constantContext, rule);
			if(constantRules.containsKey(key)) {
				Collection<ConstantConsumingContainer> containers = constantRules.get(key);
				for(ConstantConsumingContainer container : Lists.newLinkedList(containers)) {
					if(container.condition.isPresent()) {
						if(condition.isPresent()) {
							if(container.condition.equals(condition) && container.guard.isStillPossible()) {
								container.guard.addAlternative(guard);
								createNewEdge = false;
							}
							else if(!new SuffixChecker(container.condition.get(), condition.get()).isSuffix()) {
								createNewEdge = !container.guard.isStillPossible();
							}
						}
					}
					else if(!condition.isPresent() && container.guard.isStillPossible()) {
						container.guard.addAlternative(guard);
						createNewEdge = false;
					}
				}
			}
			if(createNewEdge) {
				ConstantConsumingContainer container = new ConstantConsumingContainer(constantContext, condition, rule, guard);
				String condString = condition.isPresent() ? "["+condition.get()+"] " : "";
				//System.out.println(nt + " can be constant consuming via context ("+constantContext+"): "+condString+rule);
				constantRules.put(key, container);
				for(ContextListener listener : Lists.newArrayList(listeners)) 
					listener.canBeConstantConsuming(container);
			}
		}
		
		public void addListener(ContextListener ctxListener) {
			listeners.add(ctxListener);
			for(Entry<Rule, Edge> edge : Lists.newArrayList(incomingEdges.entries()))
				if(edge.getValue().guard.isStillPossible())
					ctxListener.newIncomingEdge(edge.getValue());
				else 
					incomingEdges.remove(edge.getKey(), edge.getValue());
			
			for(Entry<KeyTuple, ConstantConsumingContainer> container : Lists.newArrayList(constantRules.entries()))
				if(container.getValue().guard.isStillPossible())
					ctxListener.canBeConstantConsuming(container.getValue());
				else
					constantRules.remove(container.getKey(), container.getValue());
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
