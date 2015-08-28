package heros.ide.edgefunc.fieldsens;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import heros.JoinLattice;
import heros.ide.edgefunc.CompositeFunction;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.edgefunc.Joinable;

import org.junit.Test;

@SuppressWarnings("unchecked")
public class FunctionJoinTest {

	private String f = "f";
	private String g = "g";
	private Factory<String> factory = new Factory<String>(mock(JoinLattice.class));
	private EdgeFunction<AccessPathBundle<String>> id = factory.id();
	private EdgeFunction<AccessPathBundle<String>> actual = null;
	private CompositeFunction<AccessPathBundle<String>> expected = null;

	private CompositeFunction<AccessPathBundle<String>> composite(EdgeFunction<AccessPathBundle<String>>... functions) {
		return new CompositeFunction<AccessPathBundle<String>>(factory, functions);
	}

	@Test
	public void mergeIdentities() {
		actual = factory.prepend(f).joinWith(factory.prepend(f));
		assertEquals(factory.prepend(f), actual);
	}

	@Test
	public void keepDifferentFunctions() {
		actual = factory.prepend(f).joinWith(factory.prepend(g));
		expected = composite(factory.prepend(f), factory.prepend(g));
		assertEquals(expected, actual);
	}

	@Test
	public void prependToComposite() {
		EdgeFunction<AccessPathBundle<String>> composite = factory.prepend(f).joinWith(factory.prepend(g));
		actual = composite.composeWith(factory.prepend(f));
		expected = composite(factory.prepend(f).composeWith(factory.prepend(f)),
				factory.prepend(g).composeWith(factory.prepend(f)));
		assertEquals(expected, actual);
	}

	@Test
	public void removeCompositeForSingleEntry() {
		EdgeFunction<AccessPathBundle<String>> composite = factory.prepend(f).joinWith(factory.prepend(g));
		actual = composite.composeWith(factory.read(f));
		assertEquals(id, actual);
	}

	@Test
	public void removeAllTopFunctionsInComposite() {
		EdgeFunction<AccessPathBundle<String>> composite = factory.prepend(f).joinWith(factory.prepend(g)).joinWith(
				factory.prepend(g).composeWith(factory.prepend(f)));
		actual = composite.composeWith(factory.read(f));
		expected = composite(id, factory.prepend(g));
		assertEquals(expected, actual);
	}

	@Test
	public void mergeMultipleComposites() {
		EdgeFunction<AccessPathBundle<String>> composite1 = factory.prepend(f).joinWith(factory.prepend(g));
		EdgeFunction<AccessPathBundle<String>> composite2 = factory.read(f).joinWith(factory.read(g));
		actual = composite1.joinWith(composite2);
		expected = composite(factory.prepend(f), factory.prepend(g), factory.read(f), factory.read(g));
		assertEquals(expected, actual);
	}

	@Test
	public void composeWithComposite() {
		EdgeFunction<AccessPathBundle<String>> composite = factory.read(f).joinWith(factory.read(g));
		actual = factory.prepend(f).composeWith(composite);
		assertEquals(id, actual);
	}
}
