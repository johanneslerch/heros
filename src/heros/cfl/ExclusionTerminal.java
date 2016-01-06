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

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public class ExclusionTerminal implements Terminal {

	private Set<Object> representation;
	
	public ExclusionTerminal(Set<Object> representation) {
		this.representation = representation;
	}
	
	public ExclusionTerminal(Object... representation) {
		if(representation.length == 0)
			throw new IllegalArgumentException();
		this.representation = Sets.newHashSet(representation);
	}
	
	@Override
	public boolean isConsumer() {
		return false;
	}

	@Override
	public Object getRepresentation() {
		return representation;
	}
	
	@Override
	public boolean isExcluding(Object representation) {
		return this.representation.contains(representation);
	}
	
	@Override
	public boolean isExclusion() {
		return true;
	}
	
	@Override
	public boolean isProducing(ConsumingTerminal consumingTerminal) {
		return false;
	}

	public ExclusionTerminal merge(ExclusionTerminal exclusionTerminal) {
		HashSet<Object> set = Sets.newHashSet(representation);
		set.addAll(exclusionTerminal.representation);
		return new ExclusionTerminal(set);
	}
	
	@Override
	public String toString() {
		String result = "¬"; 
		if(representation.size() > 1) {
			result += "{";
			result += Joiner.on(",").join(representation);
			result += "}";
		}
		else
			result += representation.iterator().next();
		return result;
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
		ExclusionTerminal other = (ExclusionTerminal) obj;
		if (representation == null) {
			if (other.representation != null)
				return false;
		} else if (!representation.equals(other.representation))
			return false;
		return true;
	}

}
