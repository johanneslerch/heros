/*******************************************************************************
 * Copyright (c) 2016 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.cfl;

import heros.cfl.NonTerminal.Listener;

import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class TransactionalNonTerminalListener implements NonTerminal.Listener {

	private Multimap<NonTerminal, NonTerminal.Listener> listeners = HashMultimap.create();
	private Set<Event> addEvents = Sets.newHashSet();
	private Set<Event> removeEvents = Sets.newHashSet();
	
	public void addListener(NonTerminal nt, NonTerminal.Listener listener) {
		if(!listeners.containsKey(nt))
			nt.addListener(this);
		listeners.put(nt, listener);
	}
	
	public void endTransaction() {
		Set<Event> tmp = removeEvents;
		removeEvents = Sets.newHashSet();
		for(Event removeEvent : tmp) {
			for(Listener listener : Lists.newArrayList(listeners.get(removeEvent.nonTerminal))) {
				listener.removedRule(removeEvent.nonTerminal, removeEvent.rule);
			}
		}
		tmp = addEvents;
		addEvents = Sets.newHashSet();
		for(Event addEvent : tmp) {
			for(Listener listener : Lists.newArrayList(listeners.get(addEvent.nonTerminal))) {
				listener.addedRule(addEvent.nonTerminal, addEvent.rule);
			}
		}
	}
	
	@Override
	public void addedRule(NonTerminal nt, Rule rule) {
		addEvents.add(new Event(nt, rule));
	}

	@Override
	public void removedRule(NonTerminal nt, Rule rule) {
		Event event = new Event(nt, rule);
		if(!addEvents.remove(event))
			removeEvents.add(event);
	}

	private static class Event {
		
		public final NonTerminal nonTerminal;
		public final Rule rule;
		
		public Event(NonTerminal nonTerminal, Rule rule) {
			this.nonTerminal = nonTerminal;
			this.rule = rule;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((nonTerminal == null) ? 0 : nonTerminal.hashCode());
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
			Event other = (Event) obj;
			if (nonTerminal == null) {
				if (other.nonTerminal != null)
					return false;
			} else if (!nonTerminal.equals(other.nonTerminal))
				return false;
			if (rule == null) {
				if (other.rule != null)
					return false;
			} else if (!rule.equals(other.rule))
				return false;
			return true;
		}
	}
}
