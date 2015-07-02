package heros.ide.edgefunc.fieldsens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import heros.JoinLattice;
import heros.ide.edgefunc.EdgeFunction;

import org.junit.Test;

import com.google.common.collect.Sets;


public class FunctionCompositionTest {

	private String f = "f";
	private String g = "g";
	private Factory<String> factory = new Factory<String>(mock(JoinLattice.class));
	private EdgeFunction<AccessPathBundle<String>> id = factory.id();
	private EdgeFunction<AccessPathBundle<String>> actual = null;
	private EdgeFunction<AccessPathBundle<String>> expected = null;
	
	@Test
	public void prependIdentity() {
		actual = factory.prepend(f).composeWith(id);
		assertEquals(factory.prepend(f), actual);
	}

	@Test
	public void overwriteIdentity() {
		actual = factory.overwrite(f).composeWith(id);
		assertEquals(factory.overwrite(f), actual);
	}

	@Test
	public void readIdentity() {
		actual = factory.read(f).composeWith(id);
		assertEquals(factory.read(f), actual);
	}

	@Test
	public void readAfterRead() {
		actual = factory.read(f).composeWith(factory.read(g));
		assertEquals(factory.read(f), ((ReadFunction) actual).chainedFunction());
	}

	@Test
	public void readAfterPrependMatch() {
		actual = factory.prepend(f).composeWith(factory.read(f));
		assertEquals(id, actual);
	}

	@Test
	public void readAfterPrependMismatch() {
		assertAllTop(factory.prepend(f).composeWith(factory.read(g)));
	}

	@Test
	public void readAfterOverwriteMatch() {
		actual = factory.overwrite(f).composeWith(factory.read(f));
		assertAllTop(actual);
	}

	@Test
	public void readAfterOverwriteMismatch() {
		actual = factory.overwrite(f).composeWith(factory.read(g));
		assertEquals(factory.read(g), actual);
	}

	@Test
	public void readAfterPrependPrepend() {
		actual = factory.prepend(f).composeWith(factory.prepend(g)).composeWith(factory.read(g));
		assertEquals(factory.prepend(f), actual);
	}

	@Test
	public void prependAfterPrepend() {
		actual = factory.prepend(f).composeWith(factory.prepend(g));
		assertEquals(factory.prepend(f), ((PrependFunction) actual).chainedFunction());
	}

	@Test
	public void prependAfterRead() {
		actual = factory.read(f).composeWith(factory.prepend(f));
		assertTrue(actual instanceof PrependFunction);
		assertEquals(factory.read(f), ((PrependFunction) actual).chainedFunction());
	}

	@Test
	public void prependAfterOverwrite() {
		actual = factory.overwrite(f).composeWith(factory.prepend(f));
		assertTrue(actual instanceof PrependFunction);
		assertEquals(factory.overwrite(f), ((PrependFunction) actual).chainedFunction());
	}

	@Test
	public void overwriteAfterPrependMatch() {
		actual = factory.prepend(f).composeWith(factory.overwrite(f));
		assertAllTop(actual);
	}

	@Test
	public void overwriteAfterPrependMismatch() {
		actual = factory.prepend(f).composeWith(factory.overwrite(g));
		assertEquals(factory.prepend(f), actual);
	}

	@Test
	public void overwriteAfterOverwrite() {
		actual = factory.overwrite(f).composeWith(factory.overwrite(g));
		assertEquals(new OverwriteFunction<String>(factory, Sets.newHashSet(f, g), null), actual);
	}

	@Test
	public void overwriteAfterRead() {
		actual = factory.read(f).composeWith(factory.overwrite(f));
		assertTrue(actual instanceof OverwriteFunction);
		assertEquals(factory.read(f), ((OverwriteFunction) actual).chainedFunction());
	}

	@Test
	public void prependTop() {
		assertAllTop(factory.prepend(f).composeWith(factory.allTop()));
	}

	@Test
	public void overwriteTop() {
		assertAllTop(factory.overwrite(f).composeWith(factory.allTop()));
	}

	@Test
	public void readTop() {
		assertAllTop(factory.read(f).composeWith(factory.allTop()));
	}

	@Test
	public void prependBeforeReadReadMatch() {
		actual = factory.prepend(g).composeWith(factory.read(g).composeWith(factory.read(f)));
		assertEquals(factory.read(f), actual);
	}

	@Test
	public void prependPrependBeforeReadReadMatch() {
		actual = factory.prepend(f).composeWith(factory.prepend(g)).composeWith(
				factory.read(g).composeWith(factory.read(f)));
		assertEquals(id, actual);
	}

	@Test
	public void prependBeforePrependPrepend() {
		actual = factory.prepend(f).composeWith(factory.prepend(g).composeWith(factory.prepend(f)));
		expected  = factory.prepend(f).composeWith(factory.prepend(g)).composeWith(factory.prepend(f));
		assertEquals(expected, actual);
	}

	@Test
	public void overwriteBeforePrependRead() {
		actual = factory.overwrite(f).composeWith(factory.read(f).composeWith(factory.prepend(g)));
		assertAllTop(actual);
	}

	@Test
	public void readBeforeReadRead() {
		actual = factory.read(f).composeWith(factory.read(g).composeWith(factory.read(f)));
		expected = factory.read(f).composeWith(factory.read(g)).composeWith(factory.read(f));
		assertEquals(expected, actual);
	}

	private void assertAllTop(EdgeFunction<AccessPathBundle<String>> f) {
		assertEquals(f.toString(), factory.allTop(), f);
	}
}
