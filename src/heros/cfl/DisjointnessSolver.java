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
import heros.cfl.ShortcutComputer.Context;
import heros.cfl.TerminalUtil.BalanceResult;
import heros.solver.Pair;
import heros.utilities.DefaultValueMap;

import java.util.List;
import java.util.Set;
import heros.cfl.Guard.*;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class DisjointnessSolver {

	private ShortcutComputer shortcutComp = new ShortcutComputer();
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
	
	public void query(Rule rule, QueryListener listener) {
		queries.getOrCreate(rule).addListener(listener);
	}
	
	public void constantCheck(Rule rule, final QueryListener listener) {
		constantCheckQueries.getOrCreate(rule).addListener(listener);
	}

	public void reduceToCallingContext(NonTerminal callingCtx, Rule rule, QueryListener queryListener) {
		reduceToCallingContextQueries.getOrCreate(new Pair<NonTerminal, Rule>(callingCtx, new RegularRule(callingCtx).append(rule))).addListener(queryListener);
	}

	public void updateRules() {
		shortcutComp.updateRules();
	}
	
	private class ConstantCheckQuery extends AbstractQuery {

		public ConstantCheckQuery(Rule queryRule) {
			super(DisjointnessSolver.this, queryRule);
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
			super(DisjointnessSolver.this, queryRule);
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
			super(DisjointnessSolver.this, queryRule);
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
		private DisjointnessSolver solver;
		private Rule queryRule;
		
		public AbstractQuery(DisjointnessSolver solver, Rule queryRule) {
			this.solver = solver;
			this.queryRule = queryRule;
		}
		
		public void start() {
			if(isSolution(queryRule)) {
				solved();
				return;
			}
			if(!allowsSubQuery(queryRule))
				return;
			
			final Optional<Rule> zeroCondition = Optional.<Rule>of(new ConstantRule(new ProducingTerminal("0")));
			solver.shortcutComp.resolve(new Context() {
				@Override
				public void addIncomingEdge(Optional<Rule> condition, Rule rule, Guard guard) {
					if(!condition.isPresent() || condition.equals(zeroCondition))
						handleResult(rule);
				}

				@Override
				public void setCanBeConstantConsuming(Context constantContext, Optional<Rule> condition, ConstantRule rule, Guard guard) {
					if(rule.isEmpty()) {
						constantContext.addIncomingEdge(zeroCondition, new ConstantRule(new ProducingTerminal("0")), guard);
					}
				}
			}, Optional.<Rule> absent(), queryRule, new RuleGuard(queryRule));
		}
		
		private void handleResult(Rule rule) {
			if(solved)
				return;
			
			if(isSolution(rule)) {
				solved();
			}
			else if(allowsSubQuery(rule)){
				AbstractQuery subQuery = triggerSubQuery(rule);
				if(subQuery != AbstractQuery.this)
					subQuery.addListener(AbstractQuery.this);
			}
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
	
}
