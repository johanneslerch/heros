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
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fj.data.Option;
import fj.function.Effect1;

public class SearchTree {

	private SearchTreeNode root;
	private List<SearchTreeNode> worklist = Lists.newLinkedList();
	private Set<SearchTreeNode> visited = Sets.newHashSet();
	private Option<SearchTreeViewer> treeViewer;

	public SearchTree(Rule rootRule, Option<SearchTreeViewer> treeViewer) {
		this.treeViewer = treeViewer;
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
			
			if(current.isSolution()) {
				return SolverResult.Solvable;
			}
			if(current.isPossible()) {
				for(SearchTreeNode child : current.expand(treeViewer)) {
					worklist.add(child);
				}
			}
		}
		return SolverResult.NotSolvable;
	}
}
