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

import java.util.List;

import com.google.common.collect.Lists;

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

	public void expand(final Option<SearchTreeViewer> treeViewer) {
		assert childs == null;
		
		childs = Lists.newLinkedList();
		rule.accept(new ExpandingVisitor() {
			@Override
			protected void newResult(RuleApplication appl) {
				SearchTreeNode newChild = new SearchTreeNode(appl.result);
				childs.add(newChild);
				notifyListenersAboutNewChild(newChild);
				if(treeViewer.isSome())
					treeViewer.some().add(SearchTreeNode.this, newChild, appl);
			}
		});
	}

	private static abstract class ExpandingVisitor implements RuleVisitor<Void> {
		
		protected abstract void newResult(RuleApplication appl);
		
		@Override
		public Void visit(ContextFreeRule contextFreeRule) {
			throw new IllegalStateException();
		}

		@Override
		public Void visit(final NonLinearRule nonLinearRule) {
			final ExpandingVisitor outer = this;
			nonLinearRule.getRight().accept(new ExpandingVisitor() {
				@Override
				protected void newResult(RuleApplication appl) {
					if(appl.result.containsNonTerminals())
						outer.newResult(new RuleApplication(appl.nonTerminal, appl.appliedRule, new NonLinearRule(nonLinearRule.getLeft(), appl.result)));
					else
						outer.newResult(new RuleApplication(appl.nonTerminal, appl.appliedRule, nonLinearRule.getLeft().append(appl.result.getTerminals())));
				}
			});
			return null;
		}

		@Override
		public Void visit(RegularRule regularRule) {
			for(Rule rule : regularRule.getNonTerminal().getRules()) {
				newResult(new RuleApplication(regularRule.getNonTerminal(), rule, regularRule.applyForNonTerminal(rule)));
			}				
			return null;
		}

		@Override
		public Void visit(ConstantRule constantRule) {
			throw new IllegalStateException();
		}
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

	public Rule getRule() {
		return rule;
	}

	public void removeSubTreeListener(SubTreeListener listener) {
		listeners.remove(listener);
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
