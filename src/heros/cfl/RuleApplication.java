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

public class RuleApplication {

	private NonTerminal nonTerminal;
	private Rule rule;
	
	public RuleApplication(NonTerminal nonTerminal, Rule rule) {
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
		RuleApplication other = (RuleApplication) obj;
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
	
	@Override
	public String toString() {
		return nonTerminal + "→"+rule;
	}
}
