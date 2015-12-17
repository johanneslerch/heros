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

	public Collection<Rule> getRules() {
		return rules;
	}

	public Object getRepresentation() {
		return representation;
	}
}
