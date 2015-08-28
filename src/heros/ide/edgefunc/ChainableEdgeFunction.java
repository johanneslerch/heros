package heros.ide.edgefunc;

import heros.ide.edgefunc.AllTop;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.edgefunc.EdgeIdentity;

import java.util.Set;

import com.google.common.base.Function;

public abstract class ChainableEdgeFunction<T extends Joinable<T>> implements EdgeFunction<T> {

	protected final ChainableEdgeFunction<T> chainedFunction;
	protected final AbstractFactory<T> factory;
	private Boolean cachedMayReturnTop;

	public ChainableEdgeFunction(AbstractFactory<T> factory, ChainableEdgeFunction<T> chainedFunction) {
		this.factory = factory;
		this.chainedFunction = chainedFunction;
	}

	public abstract EdgeFunction<T> chain(ChainableEdgeFunction<T> f);

	public EdgeFunction<T> chainedFunction() {
		return chainedFunction == null ? factory.id() : chainedFunction;
	}

	public EdgeFunction<T> chainIfNotNull(ChainableEdgeFunction<T> chainedFunction) {
		if (chainedFunction == null)
			return this;
		else
			return chainedFunction.composeWith(this);
	}

	@Override
	public T computeTarget(T source) {
		if (chainedFunction == null)
			return _computeTarget(source);
		else
			return _computeTarget(chainedFunction.computeTarget(source));
	}

	protected abstract T _computeTarget(T source);

	@Override
	public EdgeFunction<T> composeWith(EdgeFunction<T> secondFunction) {
		if (secondFunction instanceof EdgeIdentity)
			return this;
		if (secondFunction instanceof AllTop)
			return secondFunction;

		Set<EdgeFunction<T>> result = CompositeFunction.foreach(secondFunction,
				new Function<EdgeFunction<T>, EdgeFunction<T>>() {
					@Override
					public EdgeFunction<T> apply(EdgeFunction<T> input) {
						if (input instanceof EdgeIdentity)
							return ChainableEdgeFunction.this;

						ChainableEdgeFunction<T> chainableFunction = (ChainableEdgeFunction<T>) input;
						if (chainableFunction.chainedFunction == null)
							return _composeWith(chainableFunction);

						EdgeFunction<T> composition = composeWith(chainableFunction.chainedFunction);
						return composition.composeWith(chainableFunction.chain(null));
					}
				});

		if (result.isEmpty())
			return factory.allTop();
		else if (result.size() == 1)
			return result.iterator().next();
		else
			return new CompositeFunction<T>(factory, result);
	}
	
	protected abstract boolean mayThisReturnTop();
	
	@Override
	public final boolean mayReturnTop() {
		if(cachedMayReturnTop != null)
			return cachedMayReturnTop;
		
		if(chainedFunction == null) {
			cachedMayReturnTop = mayThisReturnTop();
			return cachedMayReturnTop;
		}
		
		cachedMayReturnTop = mayThisReturnTop() || chainedFunction.mayReturnTop();
		return cachedMayReturnTop;
	}

	protected abstract EdgeFunction<T> _composeWith(ChainableEdgeFunction<T> chainableFunction);

	@Override
	public EdgeFunction<T> joinWith(EdgeFunction<T> otherFunction) {
		if (equals(otherFunction))
			return this;

		if (otherFunction instanceof AllTop)
			return this;

		return new CompositeFunction<T>(factory, this, otherFunction);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chainedFunction == null) ? 0 : chainedFunction.hashCode());
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
		@SuppressWarnings("unchecked")
		ChainableEdgeFunction<T> other = (ChainableEdgeFunction<T>) obj;
		if (chainedFunction == null) {
			if (other.chainedFunction != null)
				return false;
		} else if (!chainedFunction.equals(other.chainedFunction))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (chainedFunction != null)
			return "•" + chainedFunction.toString();
		else
			return "";
	}

	public int depth() {
		if(chainedFunction == null)
			return 1;
		else
			return 1 + chainedFunction.depth();
	}
}
