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

import com.google.common.base.Optional;

public class RuleApplication {

	public final Optional<NonTerminal> nonTerminal;
	public final Rule substitutedPlaceholder;
	public final Rule appliedRule;
	public final Rule result;
	
	public RuleApplication(NonTerminal substitutedPlaceholder, Rule rule, Rule result) {
		this.nonTerminal = Optional.of(substitutedPlaceholder);
		this.substitutedPlaceholder = new RegularRule(substitutedPlaceholder);
		this.appliedRule = rule;
		this.result = result;
	}
	
	public RuleApplication(Rule substitutedPlaceholder, Rule rule, Rule result) {
		this.nonTerminal = Optional.absent();
		this.substitutedPlaceholder = substitutedPlaceholder;
		this.appliedRule = rule;
		this.result = result;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((substitutedPlaceholder == null) ? 0 : substitutedPlaceholder.hashCode());
		result = prime * result + ((this.result == null) ? 0 : this.result.hashCode());
		result = prime * result + ((appliedRule == null) ? 0 : appliedRule.hashCode());
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
		RuleApplication other = (RuleApplication) obj;
		if (substitutedPlaceholder == null) {
			if (other.substitutedPlaceholder != null)
				return false;
		} else if (!substitutedPlaceholder.equals(other.substitutedPlaceholder))
			return false;
		if (result == null) {
			if (other.result != null)
				return false;
		} else if (!result.equals(other.result))
			return false;
		if (appliedRule == null) {
			if (other.appliedRule != null)
				return false;
		} else if (!appliedRule.equals(other.appliedRule))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return substitutedPlaceholder + "→"+appliedRule;
	}
}
