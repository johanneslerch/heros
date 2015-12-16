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

import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ToStringRuleVisitor implements RuleVisitor<Void> {

	private StringBuilder result = new StringBuilder();
	private HashSet<NonTerminal> visited = Sets.newHashSet();
	private List<NonTerminal> worklist = Lists.newLinkedList();
	
	public ToStringRuleVisitor(NonTerminal...nonTerminals) {
		for(NonTerminal nt : nonTerminals)
			worklist.add(nt);
	}
	
	public ToStringRuleVisitor(Rule rule) {
		rule.accept(this);
		result = new StringBuilder();
	}

	private void include(NonTerminal nt) {
		if(!visited.add(nt))
			return;
		result.append(nt).append(": ");
		
		boolean first = true;
		for(Rule rule : nt.getRules()) {
			if(!first)
				result.append(" | ");
			else
				first = false;
			rule.accept(this);
		}
		
		result.append(String.format("%n"));
	}

	@Override
	public Void visit(ContextFreeRule contextFreeRule) {
		// TODO Auto-generated method stub
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
		if(regularRule.getNonTerminal().isPresent())
			worklist.add(regularRule.getNonTerminal().get());
		
		result.append(regularRule.toString());
		return null;
	}

	@Override
	public String toString() {
		while(!worklist.isEmpty()) {
			NonTerminal current = worklist.remove(0);
			include(current);
		}
		return result.toString();
	}
}
