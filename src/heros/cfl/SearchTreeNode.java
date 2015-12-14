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

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import fj.data.Option;

public class SearchTreeNode {

	private Rule rule;

	public SearchTreeNode(Rule rule) {
		this.rule = rule;
	}
	
	public boolean isSolution() {
		return !rule.containsConsumers();
	}
	
	public boolean isPossible() {
		return rule.isPossible();
	}

	public Collection<? extends SearchTreeNode> expand(final Option<SearchTreeViewer> treeViewer) {
		final List<SearchTreeNode> result = Lists.newLinkedList();
		rule.accept(new RuleVisitor() {
			@Override
			public void visit(ContextFreeRule contextFreeRule) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void visit(NonLinearRule nonLinearRule) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void visit(RegularRule regularRule) {
				if(regularRule.getNonTerminal().isPresent()) {
					for(Rule rule : regularRule.getNonTerminal().get().getRules()) {
						SearchTreeNode newNode = new SearchTreeNode(regularRule.applyForNonTerminal(rule));
						result.add(newNode);
						if(treeViewer.isSome())
							treeViewer.some().add(SearchTreeNode.this, newNode, new RuleApplication(regularRule.getNonTerminal().get(), rule));
					}				
				}
			}
		});
		return result;
	}

	@Override
	public String toString() {
		return rule.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		SearchTreeNode other = (SearchTreeNode) obj;
		if (rule == null) {
			if (other.rule != null)
				return false;
		} else if (!rule.equals(other.rule))
			return false;
		return true;
	}
}
