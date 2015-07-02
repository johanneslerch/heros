package heros.ide.edgefunc.fieldsens;

import heros.edgefunc.EdgeIdentity;
import heros.ide.edgefunc.AllTop;
import heros.ide.edgefunc.EdgeFunction;

import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public class CompositeFunction<Field> implements EdgeFunction<AccessPathBundle<Field>> {

	private final Set<EdgeFunction<AccessPathBundle<Field>>> functions;
	private final Factory<Field> factory;

	@SafeVarargs
	public CompositeFunction(Factory<Field> factory, EdgeFunction<AccessPathBundle<Field>>... functions) {
		this.factory = factory;
		this.functions = Sets.newHashSet();
		for (EdgeFunction<AccessPathBundle<Field>> function : functions) {
			if (function instanceof CompositeFunction) {
				for (EdgeFunction<AccessPathBundle<Field>> compFun : ((CompositeFunction<Field>) function).functions) {
					this.functions.add(compFun);
				}
			} else
				this.functions.add(function);
		}
	}

	public CompositeFunction(Factory<Field> factory, Set<EdgeFunction<AccessPathBundle<Field>>> functions) {
		this.factory = factory;
		this.functions = functions;
	}

	@Override
	public AccessPathBundle<Field> computeTarget(AccessPathBundle<Field> source) {
		AccessPathBundle<Field> result = null;
		for (EdgeFunction<AccessPathBundle<Field>> f : functions) {
			AccessPathBundle<Field> res = f.computeTarget(source);
			if (result == null)
				result = res;
			else if (res != null)
				result = result.join(res);
		}
		return result;
	}

	@Override
	public EdgeFunction<AccessPathBundle<Field>> joinWith(EdgeFunction<AccessPathBundle<Field>> otherFunction) {
		Set<EdgeFunction<AccessPathBundle<Field>>> set = Sets.newHashSet(functions);
		if (otherFunction instanceof CompositeFunction) {
			set.addAll(((CompositeFunction<Field>) otherFunction).functions);
		} else
			set.add(otherFunction);
		return new CompositeFunction<Field>(factory, set);
	}

	@Override
	public EdgeFunction<AccessPathBundle<Field>> composeWith(EdgeFunction<AccessPathBundle<Field>> secondFunction) {
		if (secondFunction instanceof EdgeIdentity)
			return this;
		if (secondFunction instanceof AllTop)
			return secondFunction;

		Set<EdgeFunction<AccessPathBundle<Field>>> resultSet = Sets.newHashSet();

		for (EdgeFunction<AccessPathBundle<Field>> function : functions) {
			for (EdgeFunction<AccessPathBundle<Field>> targetFunction : getTargetFunctions(secondFunction)) {
				EdgeFunction<AccessPathBundle<Field>> result = function.composeWith(targetFunction);
				if (!(result instanceof AllTop)) {
					if (result instanceof CompositeFunction)
						resultSet.addAll(((CompositeFunction<Field>) result).functions);
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
			return new CompositeFunction<Field>(factory, resultSet);
	}

	@SuppressWarnings("unchecked")
	private Set<EdgeFunction<AccessPathBundle<Field>>> getTargetFunctions(EdgeFunction<AccessPathBundle<Field>> function) {
		if (function instanceof CompositeFunction) {
			return ((CompositeFunction<Field>) function).functions;
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

	public static <Field> Set<EdgeFunction<AccessPathBundle<Field>>> foreach(EdgeFunction<AccessPathBundle<Field>> edgeFunction,
			Function<EdgeFunction<AccessPathBundle<Field>>, EdgeFunction<AccessPathBundle<Field>>> f) {
		Set<EdgeFunction<AccessPathBundle<Field>>> result = Sets.newHashSet();
		if (edgeFunction instanceof CompositeFunction) {
			for (EdgeFunction<AccessPathBundle<Field>> edgeF : ((CompositeFunction<Field>) edgeFunction).functions) {
				EdgeFunction<AccessPathBundle<Field>> apply = f.apply(edgeF);
				if (!(apply instanceof AllTop))
					result.add(apply);
			}
		} else {
			EdgeFunction<AccessPathBundle<Field>> apply = f.apply(edgeFunction);
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
		for (EdgeFunction<AccessPathBundle<Field>> edgeFunction : functions) {
			if(edgeFunction.mayReturnTop())
				return true;
		}
		return false;
	}
}
