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



public class AllBottom<V> implements EdgeFunction<V> {
	
	private final V bottomElement;

	public AllBottom(V bottomElement){
		this.bottomElement = bottomElement;
	} 

	public V computeTarget(V source) {
		return bottomElement;
	}

	public EdgeFunction<V> composeWith(EdgeFunction<V> secondFunction) {
		if (secondFunction instanceof EdgeIdentity)
			return this;
		return secondFunction;
	}

	public EdgeFunction<V> joinWith(EdgeFunction<V> otherFunction) {
		if(otherFunction == this || otherFunction.equals(this)) return this;
		if(otherFunction instanceof AllTop) {
			return this;
		}
		if(otherFunction instanceof EdgeIdentity) {
			return this;
		}
		if(otherFunction instanceof AllBottom)
			return this;
		//do not know how to join; hence ask other function to decide on this
		return otherFunction.joinWith(this);
	}

	public boolean equals(Object other) {
		if(other instanceof AllBottom) {
			@SuppressWarnings("rawtypes")
			AllBottom allBottom = (AllBottom) other;
			if(allBottom.bottomElement == null)
				return bottomElement == null;
			return allBottom.bottomElement.equals(bottomElement);
		}		
		return false;
	}
	
	@Override
	public int hashCode() {
		return 36609 + ((bottomElement == null) ? 0 : bottomElement.hashCode());
	}
	
	public String toString() {
		return "allbottom";
	}

	@Override
	public boolean mayReturnTop() {
		return false;
	}

}
