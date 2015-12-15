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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fj.data.Option;
import fj.function.Effect1;

public class SearchTree {

	private SearchTreeNode root;
	private List<SearchTreeNode> worklist = Lists.newLinkedList();
	private Set<SearchTreeNode> visited = Sets.newHashSet();
	private Option<SearchTreeViewer> treeViewer;
	private Map<RegularRule, PrefixGuard> prefixGuard = Maps.newHashMap();

	public SearchTree(Rule rootRule, Option<SearchTreeViewer> treeViewer) {
		this.treeViewer = treeViewer;
		this.root = new SearchTreeNode(rootRule);
		worklist.add(root);
		if(treeViewer.isSome())
			treeViewer.some().add(null, root, null);
	}
	
	public SolverResult search() {
		outer: while(!worklist.isEmpty()) {
			SearchTreeNode current = worklist.remove(0);
			if(!visited.add(current))
				continue;
			
			
			if(current.isSolution()) {
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
	
	private void checkPrefixesThenExpand(PrefixIterator iterator, SearchTreeNode node) {
		while(iterator.hasNext()) {
			RegularRule prefixRule = iterator.next();
			if(prefixGuard.containsKey(prefixRule)) {
				prefixGuard.get(prefixRule).addReductionListener(new PrefixGuardListener(node, iterator));
				return;
			}
			else
				prefixGuard.put(prefixRule, new PrefixGuard(node, iterator.suffix()));
		}

		if(treeViewer.isSome())
			treeViewer.some().removePrefixIteratorAssociation(node);
		
		for(SearchTreeNode child : node.expand(treeViewer)) {
			worklist.add(child);
		}
	}
	
	private static class PrefixGuard {
		
		private SearchTreeNode guard;
		private Terminal[] suffix;
		private boolean reduced = false;
		private List<PrefixGuardListener> listeners;

		private PrefixGuard(SearchTreeNode guard, Terminal[] suffix) {
			this.guard = guard;
			this.suffix = suffix;
		}

		public void addReductionListener(final PrefixGuardListener postfixSubTreeListener) {
			if(reduced)
				postfixSubTreeListener.reduced();
			else if(listeners == null) {
				listeners = Lists.newLinkedList();
				listeners.add(postfixSubTreeListener);
				guard.addSubTreeListener(new SubTreeListener() {
					@Override
					public void newChildren(SearchTreeNode parent, SearchTreeNode child) {
						if(guard==child || reduced) 
							return;
						if(child.containsSuffix(suffix))
							child.addSubTreeListener(this);
						else {
							reduced = true;
							notifyListeners();
						}						
					}
				});
			}
			else
				listeners.add(postfixSubTreeListener);
		}
		
		private void notifyListeners() {
			for(PrefixGuardListener listener : listeners) {
				listener.reduced();
			}
			listeners = null;
		}
	}
	
	private class PrefixGuardListener {

		private SearchTreeNode node;
		private PrefixIterator prefixIterator;

		public PrefixGuardListener(SearchTreeNode node, PrefixIterator prefixIterator) {
			this.node = node;
			this.prefixIterator = prefixIterator;
		}

		public void reduced() {
			checkPrefixesThenExpand(prefixIterator, node);
		}
	}
}
