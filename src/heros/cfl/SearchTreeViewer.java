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

import heros.solver.Pair;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class SearchTreeViewer {

	private Multimap<SearchTreeNode, Pair<RuleApplication, SearchTreeNode>> tree = HashMultimap.create();
	private Map<SearchTreeNode, PrefixIterator> blockedByUnreducedPrefixes = Maps.newHashMap();
	
	public void add(SearchTreeNode parent, SearchTreeNode child, RuleApplication ruleAppl) {
		tree.put(parent, new Pair<RuleApplication, SearchTreeNode>(ruleAppl, child));
	}

	public void remove(SearchTreeNode parent, SearchTreeNode child, RuleApplication ruleAppl) {
		tree.remove(parent, new Pair<RuleApplication, SearchTreeNode>(ruleAppl, child));
	}

	@Override
	public String toString() {
		Set<SearchTreeNode> visited = Sets.newHashSet();
		StringBuilder result = new StringBuilder();
		append(-1, result, visited, null, null);
		return result.toString();
	}
	
	public void addAuxiliaryComputation(SearchTreeNode node) {
		tree.put(null, new Pair<RuleApplication, SearchTreeNode>(null, node));
	}

	private void append(int depth, StringBuilder result, Set<SearchTreeNode> visited, RuleApplication ruleApplication, SearchTreeNode parent) {
		Collection<Pair<RuleApplication, SearchTreeNode>> childs = tree.get(parent);
		boolean newState = visited.add(parent);
		if(depth>=0) {
			result.append(Strings.repeat("\t", depth));
			if(ruleApplication!=null)
				result.append(ruleApplication).append(" = ");
			result.append(parent);
			if(!newState)
				result.append(" ⇪");
			if(parent.isSolution())
				result.append(" ✔");
			if(!parent.isPossible())
				result.append(" ╳");
			
			if(blockedByUnreducedPrefixes.containsKey(parent)) {
				result.append(" waiting on prefix: "+blockedByUnreducedPrefixes.get(parent).current());
			}
			
			result.append("\n");
		}
		if(newState) {
			for(Pair<RuleApplication, SearchTreeNode> child : childs) {
				append(depth+1, result, visited, child.getO1(), child.getO2());
			}
		}
	}

	public void associate(SearchTreeNode node, PrefixIterator prefixIterator) {
		blockedByUnreducedPrefixes.put(node, prefixIterator);
	}

	public void removePrefixIteratorAssociation(SearchTreeNode node) {
		blockedByUnreducedPrefixes.remove(node);
	}

}
