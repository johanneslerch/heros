package heros.ide.edgefunc.fieldsens;

import java.util.Set;



public interface AccessPathBundle<Field> {

	AccessPathBundle<Field> prepend(Field field);
	
	AccessPathBundle<Field> overwrite(Set<Field> field);
	
	AccessPathBundle<Field> read(Field field);
	
	AccessPathBundle<Field> join(AccessPathBundle<Field> bundle);
}
