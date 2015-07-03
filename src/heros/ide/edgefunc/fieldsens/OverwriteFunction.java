package heros.ide.edgefunc.fieldsens;

import heros.ide.edgefunc.EdgeFunction;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;


public class OverwriteFunction<Field> extends ChainableEdgeFunction<Field> {

	private final Set<Field> fields;

	@SuppressWarnings("unchecked")
	public OverwriteFunction(Factory<Field> factory, Field field) {
		super(factory, null);
		this.fields = Sets.newHashSet(field);
	}

	OverwriteFunction(Factory<Field> factory, Set<Field> fields, ChainableEdgeFunction<Field> chainedFunction) {
		super(factory, chainedFunction);
		this.fields = fields;
	}

	@Override
	protected boolean mayThisReturnTop() {
		//if this does not have a preceding function it may return top, e.g., a prepend with the same field may precede in the future
		//o.w. the succeeding function (e.g., read of overwritten field) may return top, but not this overwrite
		//this definition is important to propagate overwrite functions at initial seeds
		return chainedFunction == null;
	}
	
	@Override
	protected AccessPathBundle<Field> _computeTarget(AccessPathBundle<Field> source) {
		return source.overwrite(fields);
	}

	@Override
	protected EdgeFunction<AccessPathBundle<Field>> _composeWith(ChainableEdgeFunction<Field> chainableFunction) {
		if (chainableFunction instanceof OverwriteFunction) {
			OverwriteFunction<Field> overwriteFunction = (OverwriteFunction<Field>) chainableFunction;
			HashSet<Field> newFields = Sets.newHashSet(fields);
			newFields.addAll(overwriteFunction.fields);
			return new OverwriteFunction<Field>(factory, newFields, chainedFunction);
		}
		if (chainableFunction instanceof ReadFunction) {
			if (containsField(((ReadFunction<Field>) chainableFunction).field))
				return factory.allTop();
			else
				return chainableFunction.chainIfNotNull(chainedFunction);
		}

		return chainableFunction.chain(this);
	}

	@Override
	public ChainableEdgeFunction<Field> chain(ChainableEdgeFunction<Field> f) {
		return new OverwriteFunction<Field>(factory, fields, f);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
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
		OverwriteFunction other = (OverwriteFunction) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}

	public boolean containsField(Field field) {
		for (Field f : fields)
			if (f.equals(field))
				return true;
		return false;
	}

	@Override
	public String toString() {
		return "overwrite(" + Joiner.on(",").join(fields) + ")" + super.toString();
	}
}
