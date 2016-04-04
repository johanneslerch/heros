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

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

public interface Guard {

	public Guard dependOn(Guard g);
	
	public void addAlternative(Guard guard);

	boolean isStillPossible();
	
	public class RuleGuard implements Guard, NonTerminal.Listener {

		private Rule guardedRule;
		private boolean isGuardedRulePossible = true;
		private Set<Guard> alternatives = Sets.newHashSet();
		private boolean recursionLock = false;

		public RuleGuard(Rule rule) {
			guardedRule = rule;
		}
		
		public boolean isStillPossible() {
			return isGuardedRulePossible || areAlternativesPossible();
		}

		private boolean areAlternativesPossible() {
			if(recursionLock)
				return isGuardedRulePossible;
			
			try {
				recursionLock = true;
				Iterator<Guard> it = alternatives.iterator();
				Guard current;
				while(it.hasNext()) {
					current = it.next();
					if(current.isStillPossible())
						return true;
					else
						it.remove();
				}
				return false;
			} finally {
				recursionLock = false;
			}
		}

		public void addAlternative(Guard guard) {
			if(guard == this)
				System.out.println();
			alternatives.add(guard);
		}

		public Guard dependOn(Guard g) {
			if(g == this)
				return this;
			return new DependentGuard(this, g);
		}

		@Override
		public void addedRule(NonTerminal nt, Rule rule) {
		}

		@Override
		public void removedRule(NonTerminal nt, Rule rule) {
			if(guardedRule.equals(rule)) {
				isGuardedRulePossible = false;
				nt.removeListener(this);
			}
		}
	}

	
	static class DependentGuard implements Guard {

		private Set<Guard> alternatives = Sets.newHashSet();
		private Guard dependee2;
		private Guard dependee1;
		private boolean alternativeLock = false;
		private boolean dependeeLock = false;

		public DependentGuard(Guard dependee1, Guard dependee2) {
			this.dependee1 = dependee1;
			this.dependee2 = dependee2;
		}

		@Override
		public boolean isStillPossible() {
			if(dependeeLock)
				return true;
			
			try {
				dependeeLock = true;
				if(dependee1.isStillPossible() && dependee2.isStillPossible())
					return true;
				else
					return areAlternativesPossible();
			} finally {
				dependeeLock = false;
			}
		}
		
		private boolean areAlternativesPossible() {
			if(alternativeLock)
				return false;
			
			try {
				alternativeLock  = true;
				Iterator<Guard> it = alternatives.iterator();
				Guard current;
				while(it.hasNext()) {
					current = it.next();
					if(current.isStillPossible())
						return true;
					else
						it.remove();
				}
				return false;
			} finally {
				alternativeLock = false;
			}
		}

		@Override
		public void addAlternative(Guard guard) {
			alternatives.add(guard);
		}

		@Override
		public Guard dependOn(Guard g) {
			if(g == this)
				return this;
			return new DependentGuard(this, g);
		}
	}
}
