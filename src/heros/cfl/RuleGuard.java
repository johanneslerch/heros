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

import java.util.Set;

import com.google.common.collect.Sets;

public class RuleGuard implements NonTerminal.Listener {

	private Rule guardedRule;
	private boolean isGuardedRulePossible = true;
	private Set<RuleGuard> alternatives = Sets.newHashSet();
	private RuleGuard dependentOn;

	private RuleGuard(Rule rule, RuleGuard dependentOn) {
		guardedRule = rule;
		this.dependentOn = dependentOn;
	}
	
	public RuleGuard(Rule rule) {
		this.guardedRule = rule;
	}

	public boolean isStillPossible() {
		if(!isGuardedRulePossible)
			return areAlternativesPossible();
		if(dependentOn != null)
			return dependentOn.isStillPossible() || areAlternativesPossible();
		else
			return areAlternativesPossible();
	}

	private boolean areAlternativesPossible() {
		for(RuleGuard alt : alternatives) {
			if(alt.isStillPossible())
				return true;
		}
		return false;
	}

	public void addAlternative(RuleGuard guard) {
		alternatives.add(guard);
	}

	public RuleGuard dependOn(RuleGuard g) {
		return new RuleGuard(guardedRule, g);
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