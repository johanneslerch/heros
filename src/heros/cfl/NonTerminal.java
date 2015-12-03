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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class NonTerminal {

	private Object representation;
	private List<Rule> rules = Lists.newLinkedList();

	public NonTerminal(Object representation) {
		this.representation = representation;
	}

	public void addRule(Rule rule) {
		rules.add(rule);
	}

	@Override
	public String toString() {
		return representation.toString();
	}

	public String printRules() {
		Set<NonTerminal> visited = Sets.newHashSet();
		StringBuilder builder = new StringBuilder();
		printRules(visited, builder);
		return builder.toString();
	}

	private void printRules(Set<NonTerminal> visited, StringBuilder builder) {
		if(visited.add(this)) {
			builder.append(representation.toString()+": " +Joiner.on(" | ").join(rules)+"\n");
			for(Rule rule : rules) {
				if(rule.getNonTerminal().isPresent()) {
					rule.getNonTerminal().get().printRules(visited, builder);
				}
			}
		}
	}

	public List<Rule> getRules() {
		return rules;
	}
}
