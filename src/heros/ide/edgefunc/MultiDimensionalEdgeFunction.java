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
package heros.ide.edgefunc;

import java.util.Arrays;

import com.google.common.base.Joiner;

public class MultiDimensionalEdgeFunction<Value extends MultiDimensionalEdgeFunction.MultiDimensionalValue> implements EdgeFunction<Value> {

	private EdgeFunction[] functions;
	private AbstractFactory<Value> factory;
	
	public MultiDimensionalEdgeFunction(AbstractFactory<Value> factory, EdgeFunction... functions) {
		this.factory = factory;
		this.functions = functions;
	}

	@Override
	public Value computeTarget(Value source) {
		Object[] values = new Object[functions.length];
		for(int i=0; i<functions.length; i++) {
			values[i] = functions[i].computeTarget(source.getValue(i));
		}
		return (Value) source.create(values);
	}

	@Override
	public EdgeFunction<Value> composeWith(EdgeFunction<Value> secondFunction) {
		if(secondFunction instanceof AllTop)
			return secondFunction;
		else if(secondFunction instanceof EdgeIdentity)
			return this;
		else if(secondFunction instanceof MultiDimensionalEdgeFunction) {
			EdgeFunction[] newFunctions = new EdgeFunction[functions.length];
			boolean changed=false;
			for(int i=0; i<functions.length; i++) {
				newFunctions[i] = functions[i].composeWith(((MultiDimensionalEdgeFunction<Value>) secondFunction).functions[i]);
				if(newFunctions[i] instanceof AllTop)
					return factory.allTop();
				if(newFunctions[i]!=functions[i])
					changed = true;
			}
			if(changed)
				return new MultiDimensionalEdgeFunction<Value>(factory, newFunctions);
			else
				return this;
		}
		else
			throw new IllegalArgumentException();
	}

	@Override
	public EdgeFunction<Value> joinWith(EdgeFunction<Value> otherFunction) {
		if(otherFunction instanceof AllTop)
			return this;
		else if(otherFunction instanceof MultiDimensionalEdgeFunction) {
			EdgeFunction[] newFunctions = new EdgeFunction[functions.length];
			boolean changed=false;
			for(int i=0; i<functions.length; i++) {
				newFunctions[i] = functions[i].joinWith(((MultiDimensionalEdgeFunction<Value>) otherFunction).functions[i]);
				if(newFunctions[i] instanceof AllTop)
					return factory.allTop();
				if(newFunctions[i] == functions[i])
					changed=true;
			}
			if(changed)
				return new MultiDimensionalEdgeFunction<Value>(factory, newFunctions);
			else
				return this;
		}
		else
			throw new IllegalArgumentException();
	}

	@Override
	public boolean mayReturnTop() {
		for(int i=0; i<functions.length; i++) {
			if(functions[i].mayReturnTop())
				return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "["+Joiner.on(",").join(functions)+"]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((factory == null) ? 0 : factory.hashCode());
		result = prime * result + Arrays.hashCode(functions);
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
		MultiDimensionalEdgeFunction other = (MultiDimensionalEdgeFunction) obj;
		if (factory == null) {
			if (other.factory != null)
				return false;
		} else if (!factory.equals(other.factory))
			return false;
		if (!Arrays.equals(functions, other.functions))
			return false;
		return true;
	}

	public static interface MultiDimensionalValue {
		Object getValue(int dimension);
		
		MultiDimensionalValue create(Object[] values);
	}
}
