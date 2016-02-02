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

public class NonTerminal {

	private Object representation;
	private Set<Rule> rules = Sets.newHashSet();
	private List<Listener> listeners = Lists.newLinkedList();

	public NonTerminal(Object representation) {
		this.representation = representation;
	}

	public void addRule(Rule rule) {
		if(rule instanceof RegularRule) {
			RegularRule regRule = (RegularRule) rule;
			if(regRule.getNonTerminal() == this && regRule.getTerminals().length == 0)
				return;
		}
		
		rules.add(rule);
		for(Listener listener : Lists.newLinkedList(listeners)) {
			if(listeners!=null)
				listener.addedRule(this, rule);
		}
	}
	
	public void removeRule(Rule rule) {
		if(rules.remove(rule)) {
			for(Listener listener : listeners) {
				listener.removedRule(this, rule);
			}
		}
	}
	
	public Collection<Rule> removeAllRules() {
		Set<Rule> tmp = rules;
		rules = Sets.newHashSet();
		for(Rule r : tmp)
			for(Listener l : listeners) 
				l.removedRule(this, r);
		return tmp;
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
	
	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}
	
	List<Listener> getListeners() {
		return listeners;
	}
	
	public static interface Listener {
		void addedRule(NonTerminal nt, Rule rule);
		
		void removedRule(NonTerminal nt, Rule rule);
	}
}
