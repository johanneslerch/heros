package heros.ide.edgefunc.fieldsens;

import heros.ide.edgefunc.AbstractFactory;
import heros.ide.edgefunc.ChainableEdgeFunction;
import heros.ide.edgefunc.EdgeFunction;


public class ReadFunction<Field> extends ChainableEdgeFunction<AccessPathBundle<Field>> {

	Field field;

	public ReadFunction(AbstractFactory<AccessPathBundle<Field>> factory, Field field) {
		super(factory, null);
		this.field = field;
	}

	private ReadFunction(AbstractFactory<AccessPathBundle<Field>> factory, Field field, ChainableEdgeFunction<AccessPathBundle<Field>> chainedFunction) {
		super(factory, chainedFunction);
		this.field = field;
	}

	@Override
	protected boolean mayThisReturnTop() {
		return true;
	}
	
	@Override
	protected AccessPathBundle<Field> _computeTarget(AccessPathBundle<Field> source) {
		return source.read(field);
	}

	@Override
	protected EdgeFunction<AccessPathBundle<Field>> _composeWith(ChainableEdgeFunction<AccessPathBundle<Field>> chainableFunction) {
		return chainableFunction.chain(this);
	}

	@Override
	public ChainableEdgeFunction<AccessPathBundle<Field>> chain(ChainableEdgeFunction<AccessPathBundle<Field>> f) {
		return new ReadFunction<Field>(factory, field, f);
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
		ReadFunction other = (ReadFunction) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "read(" + field + ")" + super.toString();
	}
}
