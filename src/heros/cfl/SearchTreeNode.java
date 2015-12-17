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
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fj.data.Option;

public class SearchTreeNode {

	private Rule rule;
	private List<SubTreeListener> listeners = Lists.newLinkedList();
	private List<SearchTreeNode> childs;

	public SearchTreeNode(Rule rule) {
		this.rule = rule;
	}
	
	public boolean isSolution() {
		return rule.isSolved();
	}
	
	public boolean isPossible() {
		return rule.isPossible();
	}

	public Collection<? extends SearchTreeNode> expand(final Option<SearchTreeViewer> treeViewer) {
		assert childs == null;
		
		List<RuleApplication> list = rule.accept(new RuleVisitor<List<RuleApplication>>() {
			@Override
			public List<RuleApplication> visit(ContextFreeRule contextFreeRule) {
				throw new IllegalStateException();
			}

			@Override
			public List<RuleApplication> visit(NonLinearRule nonLinearRule) {
				List<RuleApplication> result = Lists.newLinkedList();
				List<RuleApplication> rightApplied = nonLinearRule.getRight().accept(this);
				for(RuleApplication appl : rightApplied) {
					if(appl.result.containsNonTerminals())
						result.add(new RuleApplication(appl.nonTerminal, appl.appliedRule, new NonLinearRule(nonLinearRule.getLeft(), appl.result)));
					else
						result.add(new RuleApplication(appl.nonTerminal, appl.appliedRule, nonLinearRule.getLeft().append(appl.result.getTerminals())));
				}
				return result;
			}

			@Override
			public List<RuleApplication> visit(RegularRule regularRule) {
				final List<RuleApplication> result = Lists.newLinkedList();
				for(Rule rule : regularRule.getNonTerminal().getRules()) {
					result.add(new RuleApplication(regularRule.getNonTerminal(), rule, regularRule.applyForNonTerminal(rule)));
				}				
				return result;
			}

			@Override
			public List<RuleApplication> visit(ConstantRule constantRule) {
				throw new IllegalStateException();
			}
		});
		childs = Lists.newLinkedList();
		for(RuleApplication appl : list) {
			SearchTreeNode newChild = new SearchTreeNode(appl.result);
			childs.add(newChild);
			notifyListenersAboutNewChild(newChild);
			if(treeViewer.isSome())
				treeViewer.some().add(SearchTreeNode.this, newChild, appl);
		}
		return childs;
	}
	
	protected void notifyListenersAboutNewChild(SearchTreeNode child) {
		for(SubTreeListener listener : Lists.newLinkedList(listeners)) {
			listener.newChildren(this, child);
		}
	}

	public PrefixIterator createPrefixIterator() {
		return rule.accept(new RuleVisitor<PrefixIterator>() {
			
			@Override
			public PrefixIterator visit(final RegularRule regularRule) {
				final Terminal[] terminals = regularRule.getTerminals();
				return new PrefixIterator(regularRule, terminals);
			}
			
			@Override
			public PrefixIterator visit(NonLinearRule nonLinearRule) {
				return nonLinearRule.getRight().accept(this);
			}
			
			@Override
			public PrefixIterator visit(ContextFreeRule contextFreeRule) {
				throw new IllegalStateException();
			}

			@Override
			public PrefixIterator visit(ConstantRule constantRule) {
				throw new IllegalStateException();
			}
		});
	}

	public boolean containsSuffix(final Terminal[] suffix) {
		return rule.accept(new RuleVisitor<Boolean>() {
			@Override
			public Boolean visit(ContextFreeRule contextFreeRule) {
				throw new IllegalStateException();
			}

			@Override
			public Boolean visit(NonLinearRule nonLinearRule) {
				return nonLinearRule.getRight().accept(this);
			}

			@Override
			public Boolean visit(RegularRule regularRule) {
				return visit(regularRule.getConstantRule());
			}

			@Override
			public Boolean visit(ConstantRule constantRule) {
				Terminal[] terminals = constantRule.getTerminals();
				if(terminals.length < suffix.length)
					return false;
				for(int i=0; i<suffix.length; i++) {
					if(!suffix[i].equals(terminals[terminals.length-suffix.length+i]))
						return false;
				}
				return true;
			}
		});
	}
	
	public void addSubTreeListener(SubTreeListener listener) {
		if(listeners.add(listener) && childs!=null) {
			for(SearchTreeNode child : childs) {
				listener.newChildren(this, child);
			}
		}
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
