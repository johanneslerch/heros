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


public interface Rule {

	boolean isSolved();

	boolean isPossible();

	<T> T accept(RuleVisitor<T> ruleVisitor);

	Rule append(Terminal... terminals);
	
	Rule append(Rule rule);

	boolean containsNonTerminals();

	Terminal[] getTerminals();

	boolean isEmpty();

}
