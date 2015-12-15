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

import java.util.Arrays;
import java.util.Iterator;

final class PrefixIterator implements Iterator<RegularRule> {
	
	private final RegularRule regularRule;
	private final Terminal[] terminals;
	private int index = 0;

	PrefixIterator(RegularRule regularRule, Terminal[] terminals) {
		this.regularRule = regularRule;
		this.terminals = terminals;
	}

	@Override
	public boolean hasNext() {
		return index < terminals.length;
	}

	@Override
	public RegularRule next() {
		index++;
		return current();
	}
	
	public Terminal[] suffix() {
		return Arrays.copyOfRange(terminals, index-1, terminals.length);
	}

	public RegularRule current() {
		return new RegularRule(regularRule.getNonTerminal(), Arrays.copyOf(terminals, index));
	}
}