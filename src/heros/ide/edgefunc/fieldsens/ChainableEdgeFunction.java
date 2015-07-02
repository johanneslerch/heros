package heros.ide.edgefunc.fieldsens;

import heros.ide.edgefunc.AllTop;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.edgefunc.EdgeIdentity;

import java.util.Set;

import com.google.common.base.Function;

public abstract class ChainableEdgeFunction<Field> implements EdgeFunction<AccessPathBundle<Field>> {

	protected final ChainableEdgeFunction<Field> chainedFunction;
	protected final Factory<Field> factory;
	private boolean thisMayReturnTop;
	private Boolean mayReturnTop;

	public ChainableEdgeFunction(Factory<Field> factory, boolean thisMayReturnTop, ChainableEdgeFunction<Field> chainedFunction) {
		this.factory = factory;
		this.thisMayReturnTop = thisMayReturnTop;
		this.chainedFunction = chainedFunction;
	}

	public abstract EdgeFunction<AccessPathBundle<Field>> chain(ChainableEdgeFunction<Field> f);

	public EdgeFunction<AccessPathBundle<Field>> chainedFunction() {
		return chainedFunction == null ? EdgeIdentity.<AccessPathBundle<Field>> v() : chainedFunction;
	}

	protected EdgeFunction<AccessPathBundle<Field>> chainIfNotNull(ChainableEdgeFunction<Field> chainedFunction) {
		if (chainedFunction == null)
			return this;
		else
			return chainedFunction.composeWith(this);
	}

	@Override
	public AccessPathBundle<Field> computeTarget(AccessPathBundle<Field> source) {
		if (chainedFunction == null)
			return _computeTarget(source);
		else
			return _computeTarget(chainedFunction.computeTarget(source));
	}

	protected abstract AccessPathBundle<Field> _computeTarget(AccessPathBundle<Field> source);

	@Override
	public EdgeFunction<AccessPathBundle<Field>> composeWith(EdgeFunction<AccessPathBundle<Field>> secondFunction) {
		if (secondFunction instanceof EdgeIdentity)
			return this;
		if (secondFunction instanceof AllTop)
			return secondFunction;

		Set<EdgeFunction<AccessPathBundle<Field>>> result = CompositeFunction.foreach(secondFunction,
				new Function<EdgeFunction<AccessPathBundle<Field>>, EdgeFunction<AccessPathBundle<Field>>>() {
					@Override
					public EdgeFunction<AccessPathBundle<Field>> apply(EdgeFunction<AccessPathBundle<Field>> input) {
						if (input instanceof EdgeIdentity)
							return ChainableEdgeFunction.this;

						ChainableEdgeFunction<Field> chainableFunction = (ChainableEdgeFunction<Field>) input;
						if (chainableFunction.chainedFunction == null)
							return _composeWith(chainableFunction);

						EdgeFunction<AccessPathBundle<Field>> composition = composeWith(chainableFunction.chainedFunction);
						return composition.composeWith(chainableFunction.chain(null));
					}
				});

		if (result.isEmpty())
			return factory.allTop();
		else if (result.size() == 1)
			return result.iterator().next();
		else
			return new CompositeFunction<Field>(factory, result);
	}
	
	@Override
	public final boolean mayReturnTop() {
		if(mayReturnTop != null)
			return mayReturnTop;
		
		if(chainedFunction == null)
			return thisMayReturnTop;
		
		return thisMayReturnTop && chainedFunction.mayReturnTop();
	}

	protected abstract EdgeFunction<AccessPathBundle<Field>> _composeWith(ChainableEdgeFunction<Field> chainableFunction);

	@Override
	public EdgeFunction<AccessPathBundle<Field>> joinWith(EdgeFunction<AccessPathBundle<Field>> otherFunction) {
		if (equals(otherFunction))
			return this;

		if (otherFunction instanceof AllTop)
			return this;

		return new CompositeFunction<Field>(factory, this, otherFunction);
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
		ChainableEdgeFunction<Field> other = (ChainableEdgeFunction<Field>) obj;
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
}
