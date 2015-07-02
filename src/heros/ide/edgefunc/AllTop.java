/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros.ide.edgefunc;



public class AllTop<V> implements EdgeFunction<V> {
	
	private final V topElement; 

	public AllTop(V topElement){
		this.topElement = topElement;
	} 

	public V computeTarget(V source) {
		return topElement;
	}

	public EdgeFunction<V> composeWith(EdgeFunction<V> secondFunction) {
		return this;
	}

	public EdgeFunction<V> joinWith(EdgeFunction<V> otherFunction) {
		return otherFunction;
	}

	public boolean equals(Object other) {
		if(other instanceof AllTop) {
			@SuppressWarnings("rawtypes")
			AllTop allTop = (AllTop) other;
			if(allTop.topElement == null)
				return topElement == null;
			return allTop.topElement.equals(topElement);
		}		
		return false;
	}
	
	@Override
	public int hashCode() {
		return 7320587 + ((topElement == null) ? 0 : topElement.hashCode());
	}

	public String toString() {
		return "alltop";
	}

	@Override
	public boolean mayReturnTop() {
		return true;
	}
	
}
