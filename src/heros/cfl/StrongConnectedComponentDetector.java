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

import heros.utilities.DefaultValueMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class StrongConnectedComponentDetector {
	
	private Collection<Set<NonTerminal>> results = Lists.newLinkedList();
	private DefaultValueMap<NonTerminal, Integer> indicesI = new DefaultValueMap<NonTerminal, Integer>() {
		@Override
		protected Integer createItem(NonTerminal key) {
			return 0;
		}
	};
	private Stack<NonTerminal> stackS = new Stack<NonTerminal>();
	private Stack<Integer> stackB = new Stack<Integer>();
	
	public StrongConnectedComponentDetector(NonTerminal... entryPoints) {
		for(NonTerminal current : entryPoints)
			if(indicesI.getOrCreate(current) == 0)
				dfs(current);
	}

	public StrongConnectedComponentDetector(Collection<NonTerminal> entryPoints) {
		for(NonTerminal current : entryPoints)
			if(indicesI.getOrCreate(current) == 0)
				dfs(current);
	}

	private void dfs(NonTerminal v) {
		stackS.push(v);
		indicesI.put(v, stackS.size());
		stackB.push(stackS.size());
		
		for(NonTerminal w : getEdgeTargets(v)) {
			if(indicesI.getOrCreate(w) == 0) {
				dfs(w);
			}
			else {
				while(indicesI.getOrCreate(w) < stackB.peek())
					stackB.pop();
			}
		}
		
		if(indicesI.getOrCreate(v) == stackB.peek()) {
			stackB.pop();
			Set<NonTerminal> scc = Sets.newHashSet();
			while(indicesI.getOrCreate(v) <= stackS.size()) {
				NonTerminal current = stackS.pop();
				scc.add(current);
				indicesI.put(current, Integer.MAX_VALUE);
			}
			results.add(scc);
		}
	}

	private Collection<NonTerminal> getEdgeTargets(NonTerminal v) {
		final Set<NonTerminal> result = Sets.newHashSet();
		for(Rule rule : v.getRules()) {
			rule.accept(new RuleVisitor<Void>() {
				@Override
				public Void visit(ContextFreeRule contextFreeRule) {
					result.add(contextFreeRule.getNonTerminal());
					return null;
				}

				@Override
				public Void visit(NonLinearRule nonLinearRule) {
					nonLinearRule.getLeft().accept(this);
					nonLinearRule.getRight().accept(this);
					return null;
				}

				@Override
				public Void visit(RegularRule regularRule) {
					result.add(regularRule.getNonTerminal());
					return null;
				}

				@Override
				public Void visit(ConstantRule constantRule) {
					return null;
				}
				
			});
		}
		return result;
	}

	public Collection<Set<NonTerminal>> results() {
		return results;
	}
}
