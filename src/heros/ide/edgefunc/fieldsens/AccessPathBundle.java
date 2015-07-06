package heros.ide.edgefunc.fieldsens;

import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import fj.data.List;

public class AccessPathBundle<FieldAccess> {

	private Set<List<FieldAccess>> paths = Sets.newHashSet();

	public AccessPathBundle() {
		paths.add(List.<FieldAccess> nil());
	}

	private AccessPathBundle(Set<List<FieldAccess>> paths) {
		this.paths = paths;
	}

	public Set<List<FieldAccess>> getPaths() {
		return paths;
	}

	public AccessPathBundle<FieldAccess> join(AccessPathBundle<FieldAccess> right) {
		Set<List<FieldAccess>> result = Sets.newHashSet(paths);
		result.addAll(right.paths);
		return new AccessPathBundle<FieldAccess>(result);
	}

	public AccessPathBundle<FieldAccess> prepend(FieldAccess field) {
		Set<List<FieldAccess>> result = Sets.newHashSet();
		for (List<FieldAccess> path : paths) {
			result.add(path.cons(field));
		}
		return new AccessPathBundle<FieldAccess>(result);
	}

	public AccessPathBundle<FieldAccess> read(FieldAccess field) {
		Set<List<FieldAccess>> result = Sets.newHashSet();
		for (List<FieldAccess> path : paths) {
			if (path.isNotEmpty() && path.head().equals(field))
				result.add(path.tail());
		}
		return result.isEmpty() ? null : new AccessPathBundle<FieldAccess>(result);
	}

	public AccessPathBundle<FieldAccess> overwrite(Set<FieldAccess> fields) {
		Set<List<FieldAccess>> result = Sets.newHashSet();
		for (List<FieldAccess> path : paths) {
			if (path.isEmpty() || !fields.contains(path.head()))
				result.add(path);
		}
		return result.isEmpty() ? null : new AccessPathBundle<FieldAccess>(result);
	}

	@Override
	public String toString() {
		return "{" + Joiner.on(", ").join(Iterables.transform(paths, new Function<List<FieldAccess>, String>() {
			@Override
			public String apply(List<FieldAccess> input) {
				return Joiner.on(".").join(input);
			}
		})) + "}";
	}
	
}
