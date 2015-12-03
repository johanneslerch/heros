/*******************************************************************************
 * Copyright (c) 2015 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.cfl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Solver {

	private static int MAX_DEPTH = 5;
	
	private Context root;
	private LinkedList<Context> worklist = Lists.newLinkedList();
	private Set<Context> seenContexts = Sets.newHashSet();

	public Solver(List<Rule> rules, ConsumingTerminal...consumingTerminals) {
		root = new Context(null, rules, consumingTerminals);
		worklist.add(root);
	}

	public SolverResult solve() {
		for(int depth=0; depth<MAX_DEPTH && !worklist.isEmpty(); depth++) {
			List<Context> currentLevel = Lists.newLinkedList(worklist);
			for(Context context : currentLevel) {
				worklist.remove(context);
				SolverResult result = context.solve();
				if(result.equals(SolverResult.Solvable)) {
					updateReminingWorklistContexts();
					context.updateParents();
					return result;
				}
				else if(result.equals(SolverResult.Unknown))
					context.updateParents();
			}
		}
		if(worklist.isEmpty())
			return SolverResult.NotSolvable;
		else {
			updateReminingWorklistContexts();
			return SolverResult.Unknown;
		}
	}
	
	private void updateReminingWorklistContexts() {
		for(Context context : worklist) {
			context.result = SolverResult.Unknown;
			context.updateParents();
		}
	}

	public String explain() {
		StringBuilder builder = new StringBuilder();
		root.explain(0, builder);
		return builder.toString();
	}
	
	
	
	private class Context {
		private List<Rule> rules;
		private ConsumingTerminal[] consumingTerminals;
		private Map<Rule, Context> childrens = Maps.newHashMap();
		private SolverResult result = SolverResult.Unknown;
		private Context parent;

		public Context(Context parent, List<Rule> rules, ConsumingTerminal...consumingTerminals) {
			this.parent = parent;
			this.rules = rules;
			this.consumingTerminals = consumingTerminals;
			if(rules.isEmpty())
				result = SolverResult.Solvable;
		}
		
		public void updateParents() {
			if(parent != null) {
				if(!parent.result.equals(result)) {
					parent.result = result;
					parent.updateParents();
				}
			}				
		}

		public void explain(int depth, StringBuilder builder) {
			switch(result) {
			case Solvable: builder.append(" ✔\n"); break;
			case NotSolvable: builder.append(" ✦\n"); break;
			case Unknown: builder.append("❔\n"); break;
			}			
			
			if(depth == MAX_DEPTH) {
				builder.append(Strings.repeat("\t", depth));
				builder.append(" ✂\n");
			} else {
				for(Rule rule : rules) {
					builder.append(Strings.repeat("\t", depth));
					builder.append(rule+ " : " +Joiner.on("").join(consumingTerminals));
					if(childrens.containsKey(rule)) {
						childrens.get(rule).explain(depth+1, builder);
					}
					else
						builder.append(" ✦\n");
				}
			}
		}

		private SolverResult solve() {
			for(Rule rule : rules) {
				if(rule.areSuccessorsPossible(consumingTerminals)) {
					Rule newRule = rule.apply(consumingTerminals);
					if(newRule.containsConsumers()) {
						if(newRule.getNonTerminal().isPresent()) {
							createNewContext(rule, newRule.getNonTerminal().get(), newRule.getConsumers());
						}
					}
					else {
						childrens.put(rule, new Context(this, Lists.<Rule>newLinkedList(), new ConsumingTerminal[0]));
						result = SolverResult.Solvable;
						return result;
					}
				}
			}
			return result;
		}
		
		private void createNewContext(Rule derivedFromRule, NonTerminal nonTerminal, ConsumingTerminal[] consumingTerminals) {
			Context context = new Context(this, nonTerminal.getRules(), consumingTerminals);
			if(seenContexts.add(context)) {
				childrens.put(derivedFromRule, context);
				worklist.add(context);
				result = SolverResult.Unknown;
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(consumingTerminals);
			result = prime * result + ((rules == null) ? 0 : rules.hashCode());
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
			Context other = (Context) obj;
			if (!Arrays.equals(consumingTerminals, other.consumingTerminals))
				return false;
			if (rules == null) {
				if (other.rules != null)
					return false;
			} else if (!rules.equals(other.rules))
				return false;
			return true;
		}
	}
}
