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

import heros.cfl.TerminalUtil.BalanceResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fj.data.Option;

public class SearchTree {

	private SearchTreeNode root;
	private List<SearchTreeNode> worklist = Lists.newLinkedList();
	private Set<SearchTreeNode> visited = Sets.newHashSet();
	private Option<SearchTreeViewer> treeViewer;
	private Map<RegularRule, PrefixGuard> prefixGuard = Maps.newHashMap();
	private boolean requireConstantSolution;

	public SearchTree(Rule rootRule, Option<SearchTreeViewer> treeViewer, boolean requireConstantSolution) {
		this.treeViewer = treeViewer;
		this.requireConstantSolution = requireConstantSolution;
		this.root = new SearchTreeNode(rootRule);
		worklist.add(root);
		if(treeViewer.isSome())
			treeViewer.some().add(null, root, null);
	}

	public SolverResult search() {
		while(!worklist.isEmpty()) {
			SearchTreeNode current = worklist.remove(0);
			if(!visited.add(current))
				continue;
			
			if(isSolution(current)) {
				return SolverResult.Solvable;
			}
			
			if(current.isPossible()) {
				PrefixIterator prefixIterator = current.createPrefixIterator();
				if(treeViewer.isSome())
					treeViewer.some().associate(current, prefixIterator);
				
				checkPrefixesThenExpand(prefixIterator, current);
			}
		}
		return SolverResult.NotSolvable;
	}
	
	private boolean isSolution(SearchTreeNode node) {
		if(requireConstantSolution)
			return node.getRule() instanceof ConstantRule && node.isSolution();
		else
			return node.isSolution();
	}
	
	private void checkPrefixesThenExpand(PrefixIterator iterator, SearchTreeNode node) {
		while(iterator.hasNext()) {
			RegularRule prefixRule = iterator.next();
			if(prefixGuard.containsKey(prefixRule)) {
				PrefixGuard currentGuard = prefixGuard.get(prefixRule);
				if(currentGuard.isBetterSuitedThan(iterator.suffix())) {
					currentGuard.addReductionListener(new PrefixGuardListener(node, iterator));
					return;
				} else {
					currentGuard.swapNode(node, iterator.suffix());
				}
			}
			else
				prefixGuard.put(prefixRule, new PrefixGuard(node, iterator.suffix()));
		}

		if(treeViewer.isSome())
			treeViewer.some().removePrefixIteratorAssociation(node);
		
		node.addSubTreeListener(new SubTreeListener() {
			@Override
			public void newChildren(SearchTreeNode parent, SearchTreeNode child) {
				worklist.add(child);
			}
		});
		node.expand(treeViewer);
	}
	
	private static class PrefixGuard {
		
		private SearchTreeNode guard;
		private Terminal[] suffix;
		private boolean reducedToEmpty = false;
		private List<PrefixGuardListener> listeners;
		private Set<Terminal[]> reducedToConstant;
		private SubTreeListener subTreeListener;

		private PrefixGuard(SearchTreeNode guard, Terminal[] suffix) {
			this.guard = guard;
			this.suffix = suffix;
			this.subTreeListener = new SubTreeListener() {

				@Override
				public void newChildren(SearchTreeNode parent, SearchTreeNode child) {
					if(PrefixGuard.this.guard==child || reducedToEmpty) 
						return;
					if(child.containsSuffix(PrefixGuard.this.suffix)) {
						child.addSubTreeListener(this);
						if(isConstantReduction(child)) {
							if(reducedToConstant == null)
								reducedToConstant = Sets.newHashSet();
							if(reducedToConstant.add(child.getRule().getTerminals()))
								notifyListenersAboutConstantResult(child.getRule().getTerminals());
						}
					} else {
						reducedToEmpty = true;
						notifyListeners();
					}						
				}

				private boolean isConstantReduction(SearchTreeNode child) {
					if(!(child.getRule() instanceof ConstantRule))
						return false;
					
					if(child.getRule().isPossible())
						return true;
					
					return TerminalUtil.isBalanced(((ConstantRule) child.getRule()).getTerminals()) == BalanceResult.MORE_CONSUMERS;
				}
			};
		}

		public void swapNode(SearchTreeNode newGuard, Terminal[] newSuffix) {
			this.guard.removeSubTreeListener(subTreeListener);
			this.guard = newGuard;
			this.suffix = newSuffix;
			if(listeners != null)
				this.guard.addSubTreeListener(subTreeListener);
		}

		public boolean isBetterSuitedThan(Terminal[] suffix) {
			return this.suffix.length <= suffix.length;
		}

		public void addReductionListener(final PrefixGuardListener postfixSubTreeListener) {
			if(reducedToEmpty)
				postfixSubTreeListener.reducedToEmpty();
			else if(listeners == null) {
				listeners = Lists.newLinkedList();
				listeners.add(postfixSubTreeListener);
				guard.addSubTreeListener(subTreeListener);
			}
			else {
				listeners.add(postfixSubTreeListener);
				if(reducedToConstant != null)
					for(Terminal[] constant : reducedToConstant)
						postfixSubTreeListener.reducedToConstant(constant);
			}
		}
		
		private void notifyListenersAboutConstantResult(Terminal[] constant) {
			for(PrefixGuardListener listener : listeners) 
				listener.reducedToConstant(constant);
		}

		private void notifyListeners() {
			for(PrefixGuardListener listener : listeners) {
				listener.reducedToEmpty();
			}
			listeners = null;
		}
	}
	
	private class PrefixGuardListener {

		private SearchTreeNode node;
		private PrefixIterator prefixIterator;
		private boolean notified;

		public PrefixGuardListener(SearchTreeNode node, PrefixIterator prefixIterator) {
			this.node = node;
			this.prefixIterator = prefixIterator;
		}

		public void reducedToConstant(final Terminal[] constant) {
			node.getRule().accept(new ReducedToConstantVisitor(prefixIterator.current(), constant) {
				@Override
				protected void newResult(RuleApplication appl) {
					if(appl.result == null)
						return;
					
					worklist.add(node.newChild(appl, treeViewer));
				}
			});
		}

		public void reducedToEmpty() {
			if(!notified) {
				notified=true;
				checkPrefixesThenExpand(prefixIterator, node);
			}
		}
	}
	
	private static abstract class ReducedToConstantVisitor implements RuleVisitor<Void> {
		
		private Terminal[] constant;
		private RegularRule prefix;

		public ReducedToConstantVisitor(RegularRule prefix, Terminal[] constant) {
			this.prefix = prefix;
			this.constant = constant;
		}
		
		protected abstract void newResult(RuleApplication appl);
		
		@Override
		public Void visit(ContextFreeRule contextFreeRule) {
			throw new IllegalStateException();
		}

		@Override
		public Void visit(final NonLinearRule nonLinearRule) {
			final ReducedToConstantVisitor outer = this;
			nonLinearRule.getRight().accept(new ReducedToConstantVisitor(prefix, constant) {
				@Override
				protected void newResult(RuleApplication appl) {
					if(appl.result == null)
						outer.newResult(new RuleApplication(appl.substitutedPlaceholder, appl.appliedRule, nonLinearRule.getLeft().append(constant)));
					else
						outer.newResult(new RuleApplication(appl.substitutedPlaceholder, appl.appliedRule, 
								new NonLinearRule(nonLinearRule.getLeft(), appl.result)));
				}
			});
			return null;
		}

		@Override
		public Void visit(RegularRule regularRule) {
			newResult(new RuleApplication(prefix, new ConstantRule(constant), null));
			return null;
		}

		@Override
		public Void visit(ConstantRule constantRule) {
			throw new IllegalStateException();
		}
	}
}
