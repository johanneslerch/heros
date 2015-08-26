package heros.ide.edgefunc.fieldsens;

import heros.ide.edgefunc.EdgeFunction;

public class PrependFunction<Field> extends ChainableEdgeFunction<Field> {

	public final Field field;

	public PrependFunction(Factory<Field> factory, Field field) {
		super(factory, null);
		this.field = field;
	}

	private PrependFunction(Factory<Field> factory, Field field, ChainableEdgeFunction<Field> chainedFunction) {
		super(factory, chainedFunction);
		this.field = field;
	}
	
	@Override
	protected boolean mayThisReturnTop() {
		return false;
	}

	@Override
	protected AccessPathBundle<Field> _computeTarget(AccessPathBundle<Field> source) {
		return source.prepend(field);
	}

	@Override
	protected EdgeFunction<AccessPathBundle<Field>> _composeWith(ChainableEdgeFunction<Field> chainableFunction) {
		if (chainableFunction instanceof ReadFunction) {
			if (((ReadFunction<Field>) chainableFunction).field.equals(field))
				return chainedFunction();
			else
				return factory.allTop();
		}
		if (chainableFunction instanceof OverwriteFunction) {
			if (((OverwriteFunction<Field>) chainableFunction).containsField(field))
				return factory.allTop();
			else
				return this;
		}
		if(chainableFunction instanceof EnsureEmptyFunction)
			return factory.allTop();

		return chainableFunction.chain(this);
	}

	@Override
	public ChainableEdgeFunction<Field> chain(ChainableEdgeFunction<Field> f) {
		return new PrependFunction<Field>(factory, field, f);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrependFunction other = (PrependFunction) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "prepend(" + field + ")" + super.toString();
	}
}
