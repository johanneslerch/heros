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


public class ContextFreeRule implements Rule {

	@Override
	public boolean isSolved() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPossible() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> T accept(RuleVisitor<T> ruleVisitor) {
		return ruleVisitor.visit(this);
	}

	@Override
	public Rule append(Terminal... terminals) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean containsNonTerminals() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Terminal[] getTerminals() {
		// TODO Auto-generated method stub
		return null;
	}
}
