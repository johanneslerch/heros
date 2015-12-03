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

public class ConsumingTerminal implements Terminal {

	private Object representation;

	public ConsumingTerminal(Object representation) {
		this.representation = representation;
	}

	@Override
	public String toString() {
		return "-"+representation;
	}

	public Object getRepresentation() {
		return representation;
	}

	@Override
	public boolean isConsumer() {
		return true;
	}

	@Override
	public boolean isProducing(ConsumingTerminal consumingTerminal) {
		return false;
	}
}
