package heros.ide.edgefunc;

import heros.edgefunc.EdgeIdentity;
import heros.ide.edgefunc.AllTop;
import heros.ide.edgefunc.EdgeFunction;

import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public class CompositeFunction<T extends Joinable<T>> implements EdgeFunction<T> {

	private final Set<EdgeFunction<T>> functions;
	private final AbstractFactory<T> factory;

	@SafeVarargs
	public CompositeFunction(AbstractFactory<T> factory, EdgeFunction<T>... functions) {
		this.factory = factory;
		this.functions = Sets.newHashSet();
		for (EdgeFunction<T> function : functions) {
			if (function instanceof CompositeFunction) {
				for (EdgeFunction<T> compFun : ((CompositeFunction<T>) function).functions) {
					this.functions.add(compFun);
				}
			} else
				this.functions.add(function);
		}
	}

	public CompositeFunction(AbstractFactory<T> factory, Set<EdgeFunction<T>> functions) {
		this.factory = factory;
		this.functions = functions;
	}

	@Override
	public T computeTarget(T source) {
		T result = null;
		for (EdgeFunction<T> f : functions) {
			T res = f.computeTarget(source);
			if (result == null)
				result = res;
			else if (res != null)
				result = result.join(res);
		}
		return result;
	}

	@Override
	public EdgeFunction<T> joinWith(EdgeFunction<T> otherFunction) {
		Set<EdgeFunction<T>> set = Sets.newHashSet(functions);
		if (otherFunction instanceof CompositeFunction) {
			set.addAll(((CompositeFunction<T>) otherFunction).functions);
		} else
			set.add(otherFunction);
		return new CompositeFunction<T>(factory, set);
	}

	@Override
	public EdgeFunction<T> composeWith(EdgeFunction<T> secondFunction) {
		if (secondFunction instanceof EdgeIdentity)
			return this;
		if (secondFunction instanceof AllTop)
			return secondFunction;

		Set<EdgeFunction<T>> resultSet = Sets.newHashSet();

		for (EdgeFunction<T> function : functions) {
			for (EdgeFunction<T> targetFunction : getTargetFunctions(secondFunction)) {
				EdgeFunction<T> result = function.composeWith(targetFunction);
				if (!(result instanceof AllTop)) {
					if (result instanceof CompositeFunction)
						resultSet.addAll(((CompositeFunction<T>) result).functions);
					else
						resultSet.add(result);
				}
			}
		}
		if (resultSet.isEmpty())
			return factory.allTop();
		else if (resultSet.size() == 1)
			return resultSet.iterator().next();
		else
			return new CompositeFunction<T>(factory, resultSet);
	}

	@SuppressWarnings("unchecked")
	private Set<EdgeFunction<T>> getTargetFunctions(EdgeFunction<T> function) {
		if (function instanceof CompositeFunction) {
			return ((CompositeFunction<T>) function).functions;
		} else
			return Sets.newHashSet(function);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((functions == null) ? 0 : functions.hashCode());
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
		CompositeFunction other = (CompositeFunction) obj;
		if (functions == null) {
			if (other.functions != null)
				return false;
		} else if (!functions.equals(other.functions))
			return false;
		return true;
	}

	public static <T extends Joinable<T>> Set<EdgeFunction<T>> foreach(EdgeFunction<T> edgeFunction,
			Function<EdgeFunction<T>, EdgeFunction<T>> f) {
		Set<EdgeFunction<T>> result = Sets.newHashSet();
		if (edgeFunction instanceof CompositeFunction) {
			for (EdgeFunction<T> edgeF : ((CompositeFunction<T>) edgeFunction).functions) {
				EdgeFunction<T> apply = f.apply(edgeF);
				if (!(apply instanceof AllTop))
					result.add(apply);
			}
		} else {
			EdgeFunction<T> apply = f.apply(edgeFunction);
			if (!(apply instanceof AllTop))
				result.add(apply);
		}
		return result;
	}

	@Override
	public String toString() {
		return "{" + Joiner.on(",").join(functions) + "}";
	}

	@Override
	public boolean mayReturnTop() {
		for (EdgeFunction<T> edgeFunction : functions) {
			if(edgeFunction.mayReturnTop())
				return true;
		}
		return false;
	}
}
