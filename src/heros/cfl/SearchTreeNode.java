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
		return !rule.containsConsumers();
	}
	
	public boolean isPossible() {
		return rule.isPossible();
	}

	public Collection<? extends SearchTreeNode> expand(final Option<SearchTreeViewer> treeViewer) {
		assert childs == null;
		
		childs = rule.accept(new RuleVisitor<List<SearchTreeNode>>() {
			@Override
			public List<SearchTreeNode> visit(ContextFreeRule contextFreeRule) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<SearchTreeNode> visit(NonLinearRule nonLinearRule) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<SearchTreeNode> visit(RegularRule regularRule) {
				final List<SearchTreeNode> result = Lists.newLinkedList();
				if(regularRule.getNonTerminal().isPresent()) {
					for(Rule rule : regularRule.getNonTerminal().get().getRules()) {
						SearchTreeNode newNode = new SearchTreeNode(regularRule.applyForNonTerminal(rule));
						result.add(newNode);
						notifyListenersAboutNewChild(newNode);
						if(treeViewer.isSome())
							treeViewer.some().add(SearchTreeNode.this, newNode, new RuleApplication(regularRule.getNonTerminal().get(), rule));
					}				
				}
				return result;
			}
		});
		return childs;
	}
	
	protected void notifyListenersAboutNewChild(SearchTreeNode child) {
		for(SubTreeListener listener : listeners) {
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
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public PrefixIterator visit(ContextFreeRule contextFreeRule) {
				// TODO Auto-generated method stub
				return null;
			}
		});
	}

	public boolean containsSuffix(final Terminal[] suffix) {
		return rule.accept(new RuleVisitor<Boolean>() {
			@Override
			public Boolean visit(ContextFreeRule contextFreeRule) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Boolean visit(NonLinearRule nonLinearRule) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Boolean visit(RegularRule regularRule) {
				Terminal[] terminals = regularRule.getTerminals();
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
