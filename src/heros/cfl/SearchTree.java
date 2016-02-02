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
import heros.utilities.DefaultValueMap;

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
	private Map<Rule, SearchTreeNode> visited = Maps.newHashMap();
	private Option<SearchTreeViewer> treeViewer;
	private Map<RegularRule, PrefixGuard> prefixGuards = Maps.newHashMap();
	private List<SearchTreeResultListener> listeners = Lists.newLinkedList();
	private boolean solved;
	private boolean running;
	private SearchTreeResultChecker resultChecker;

	public SearchTree(Rule rootRule, Option<SearchTreeViewer> treeViewer) {
		this(rootRule, treeViewer, new SearchTreeResultChecker() {
			@Override
			public boolean isSolution(Rule rule) {
				return rule.isSolved();
			}
		});
	}
	
	public SearchTree(Rule rootRule, Option<SearchTreeViewer> treeViewer, SearchTreeResultChecker resultChecker) {
		this.treeViewer = treeViewer;
		this.resultChecker = resultChecker;
		this.root = new SearchTreeNode(rootRule);
		worklist.add(root);
		if(treeViewer.isSome())
			treeViewer.some().add(null, root, null);
	}
	
	public void addListener(SearchTreeResultListener listener) {
		listeners.add(listener);
		if(solved)
			listener.solved();
	}

	public void search() {
		if(solved || running)
			return;
		running = true;
		
		while(!worklist.isEmpty()) {
			SearchTreeNode current = worklist.remove(0);
			if(visited.containsKey(current.getRule()))
				continue;
			else
				visited.put(current.getRule(), current);
			
			if(resultChecker.isSolution(current.getRule())) {
				solved();
				return;
			}
			
			if(current.isPossible()) {
				PrefixIterator prefixIterator = current.createPrefixIterator();
				if(treeViewer.isSome())
					treeViewer.some().associate(current, prefixIterator);
				
				checkPrefixesThenExpand(prefixIterator, current);
			}
		}
		running = false;
	}
	
	private void solved() {
		solved = true;
		running = false;
		
		for(SearchTreeNode node : worklist) {
			if(visited.containsKey(node.getRule()))
				assert node.ownListeners.isEmpty();
			else
				visited.put(node.getRule(), node);
		}
		worklist = null;
		for(SearchTreeNode node : visited.values()) {
			node.detach();
		}
		visited = null;
		prefixGuards = null;

		for(SearchTreeResultListener listener: listeners)
			listener.solved();
		
		listeners = null;
	}

	private void addToWorklist(SearchTreeNode node) {
		worklist.add(node);
		if(!solved && !running)
			search();
	}
	
	private void checkPrefixesThenExpand(PrefixIterator iterator, SearchTreeNode node) {
		while(iterator.hasNext()) {
			RegularRule prefixRule = iterator.next();
			if(prefixGuards.containsKey(prefixRule)) {
				PrefixGuard currentGuard = prefixGuards.get(prefixRule);
				if(currentGuard.isBetterSuitedThan(iterator.suffix())) {
					currentGuard.addReductionListener(new PrefixGuardListener(node, iterator));
					return;
				} else {
					currentGuard.swapNode(node, iterator.suffix());
				}
			} else
				if(createPrefixGuard(iterator, node, prefixRule))
					return;
		}

		if(treeViewer.isSome())
			treeViewer.some().removePrefixIteratorAssociation(node);
		
		node.addSubTreeListener(new SubTreeListener() {
			@Override
			public void newChildren(SearchTreeNode parent, SearchTreeNode child) {
				addToWorklist(child);
			}
		});
		node.expand(treeViewer);
	}

	private boolean createPrefixGuard(final PrefixIterator iterator, final SearchTreeNode node, final RegularRule prefixRule) {
		return node.getRule().accept(new RuleVisitor<Boolean>() {
			@Override
			public Boolean visit(ContextFreeRule contextFreeRule) {
				prefixGuards.put(prefixRule, new PrefixGuard(node, iterator.suffix()));
				return false;
			}

			@Override
			public Boolean visit(NonLinearRule nonLinearRule) {
				if(nonLinearRule.getRight() instanceof NonLinearRule)
					nonLinearRule.getRight().accept(this);
				else {
					boolean isNew = !visited.containsKey(nonLinearRule.getRight());
					SearchTreeNode prefixNode = isNew ? new SearchTreeNode(nonLinearRule.getRight()) : visited.get(nonLinearRule.getRight());
					PrefixGuard guard = new PrefixGuard(prefixNode, iterator.suffix());
					prefixGuards.put(prefixRule, guard);
					guard.addReductionListener(new PrefixGuardListener(node, iterator));
					
					if(isNew) {
						if(treeViewer.isSome())
							treeViewer.some().addAuxiliaryComputation(prefixNode);
						visited.put(nonLinearRule.getRight(), prefixNode);
						prefixNode.addSubTreeListener(new SubTreeListener() {
							@Override
							public void newChildren(SearchTreeNode parent, SearchTreeNode child) {
								addToWorklist(child);
							}
						});
						prefixNode.expand(treeViewer);
					}
				}
				return true;
			}

			@Override
			public Boolean visit(RegularRule regularRule) {
				prefixGuards.put(prefixRule, new PrefixGuard(node, iterator.suffix()));
				return false;
			}

			@Override
			public Boolean visit(ConstantRule constantRule) {
				throw new IllegalStateException();
			}
		});
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
			Rule result = node.getRule().accept(new ReplaceSuffixByConstant(constant));
			addToWorklist(node.newChild(new RuleApplication(prefixIterator.current(), new ConstantRule(constant), result), treeViewer));
		}

		public void reducedToEmpty() {
			if(!notified) {
				notified=true;
				checkPrefixesThenExpand(prefixIterator, node);
			}
		}
	}
	
	private static class ReplaceSuffixByConstant implements RuleVisitor<Rule> {
		
		private Terminal[] constant;

		public ReplaceSuffixByConstant(Terminal[] constant) {
			this.constant = constant;
		}
		
		@Override
		public Rule visit(ContextFreeRule contextFreeRule) {
			return new ConstantRule(TerminalUtil.append(contextFreeRule.getLeftTerminals(), constant));
		}

		@Override
		public Rule visit(final NonLinearRule nonLinearRule) {
			Rule result = nonLinearRule.getRight().accept(this);
			return nonLinearRule.getLeft().append(result);
//			
//			
//			final ReducedToConstantVisitor outer = this;
//			nonLinearRule.getRight().accept(new ReducedToConstantVisitor(prefix, constant) {
//				@Override
//				protected void newResult(RuleApplication appl) {
//					if(appl.result == null)
//						outer.newResult(new RuleApplication(appl.substitutedPlaceholder, appl.appliedRule, nonLinearRule.getLeft().append(constant)));
//					else
//						outer.newResult(new RuleApplication(appl.substitutedPlaceholder, appl.appliedRule, 
//								new NonLinearRule(nonLinearRule.getLeft(), appl.result)));
//				}
//			});
//			return null;
		}

		@Override
		public Rule visit(RegularRule regularRule) {
			return new ConstantRule(constant);
		}

		@Override
		public Rule visit(ConstantRule constantRule) {
			throw new IllegalStateException();
		}
	}
	
	public static interface SearchTreeResultChecker {
		public boolean isSolution(Rule rule);
	}
}
