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
		String string = representation.toString();
		char[] result = new char[string.length()*2];
		for(int i=0; i<string.length(); i++) {
			result[i*2+1] = '\u0305';
			result[i*2] = string.charAt(i);
		}
		return new String(result);
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((representation == null) ? 0 : representation.hashCode());
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
		ConsumingTerminal other = (ConsumingTerminal) obj;
		if (representation == null) {
			if (other.representation != null)
				return false;
		} else if (!representation.equals(other.representation))
			return false;
		return true;
	}
}
