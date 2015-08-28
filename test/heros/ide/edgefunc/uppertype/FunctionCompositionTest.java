/*******************************************************************************
 * Copyright (c) 2015 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.ide.edgefunc.uppertype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import heros.JoinLattice;
import heros.ide.edgefunc.ChainableEdgeFunction;
import heros.ide.edgefunc.EdgeFunction;

import org.junit.Test;

public class FunctionCompositionTest {

	private Factory<TestType> factory = new Factory<TestType>(mock(JoinLattice.class));
	
	private TestType superType = new TestType();
	private TestType type = new TestType(superType);
	private TestType subType = new TestType(type);
	private TestType otherType = new TestType();
	
	
	@Test(expected=IllegalStateException.class)
	public void anyBeforeAny() {
		factory.any().composeWith(factory.any());
	}
	
	@Test(expected=IllegalStateException.class)
	public void anyBeforeAnyOrType() {
		factory.any().composeWith(factory.anyOrType(type));
	}
	
	@Test
	public void anyBeforeEmpty() {
		EdgeFunction<TestType> actual = factory.any().composeWith(factory.empty());
		assertEquals(factory.empty(), actual);
	}
	
	@Test
	public void anyBeforePop() {
		EdgeFunction<TestType> actual = factory.any().composeWith(factory.pop());
		assertEquals(factory.any(), actual);
	}
	
	@Test
	public void anyBeforePush() {
		assertNoSimplification(factory.any(), factory.push());
	}
	
	@Test
	public void anyBeforeType() {
		EdgeFunction<TestType> actual = factory.any().composeWith(factory.type(type));
		assertEquals(factory.anyOrType(type), actual);
	}
	
	@Test
	public void anyBeforeUpperBound() {
		assertEquals(factory.anyOrUpperBound(type), factory.any().composeWith(factory.upperBound(type)));
	}
	
	@Test(expected=IllegalStateException.class)
	public void anyOrTypeBeforeAny() {
		factory.anyOrType(type).composeWith(factory.any());
	}
	
	@Test
	public void anyOrTypeBeforeEmpty() {
		EdgeFunction<TestType> actual = factory.anyOrType(type).composeWith(factory.empty());
		assertEquals(factory.empty(), actual);
	}
	
	@Test
	public void anyOrTypeBeforePop() {
		EdgeFunction<TestType> actual = factory.anyOrType(type).composeWith(factory.pop());
		assertEquals(factory.any(), actual);
	}
	
	@Test
	public void anyOrTypeBeforePush() {
		assertNoSimplification(factory.anyOrType(type), factory.push());
	}
	
	@Test
	public void anyOrTypeBeforeTypeMatch() {
		EdgeFunction<TestType> actual = factory.anyOrType(type).composeWith(factory.type(type));
		assertEquals(factory.anyOrType(type), actual);
	}
	
	@Test
	public void anyOrTypeBeforeTypeMismatch() {
		EdgeFunction<TestType> actual = factory.anyOrType(type).composeWith(factory.type(otherType));
		assertEquals(factory.allTop(), actual);
	}
	
	@Test
	public void anyOrTypeBeforeUpperBoundMatch() {
		EdgeFunction<TestType> actual = factory.anyOrType(type).composeWith(factory.upperBound(superType));
		assertEquals(factory.anyOrType(type), actual);
	}
	
	@Test
	public void anyOrTypeBeforeUpperBoundMismatch() {
		EdgeFunction<TestType> actual = factory.anyOrType(type).composeWith(factory.upperBound(subType));
		assertEquals(factory.allTop(), actual);
	}
	
	@Test(expected=IllegalStateException.class)
	public void anyOrUpperBoundBeforeAny() {
		factory.anyOrUpperBound(type).composeWith(factory.any());
	}
	
	@Test
	public void anyOrUpperBoundBeforeEmpty() {
		EdgeFunction<TestType> actual = factory.anyOrUpperBound(type).composeWith(factory.empty());
		assertEquals(factory.empty(), actual);
	}
	
	@Test
	public void anyOrUpperBoundBeforePop() {
		EdgeFunction<TestType> actual = factory.anyOrUpperBound(type).composeWith(factory.pop());
		assertEquals(factory.any(), actual);
	}
	
	@Test
	public void anyOrUpperBoundBeforePush() {
		assertNoSimplification(factory.anyOrUpperBound(type), factory.push());
	}
	
	@Test
	public void anyOrUpperBoundBeforeTypeMatch() {
		EdgeFunction<TestType> actual = factory.anyOrUpperBound(type).composeWith(factory.type(subType));
		assertEquals(factory.anyOrType(subType), actual);
	}
	
	@Test
	public void anyOrUpperBoundBeforeTypeMismatch() {
		EdgeFunction<TestType> actual = factory.anyOrUpperBound(type).composeWith(factory.type(superType));
		assertEquals(factory.allTop(), actual);
	}
	
	@Test
	public void anyOrUpperBoundBeforeUpperBoundMatch() {
		assertEquals(factory.anyOrUpperBound(type), factory.anyOrUpperBound(type).composeWith(factory.upperBound(superType)));
		assertEquals(factory.anyOrUpperBound(subType), factory.anyOrUpperBound(type).composeWith(factory.upperBound(subType)));
	}
	
	@Test
	public void anyOrUpperBoundBeforeUpperBoundMismatch() {
		EdgeFunction<TestType> actual = factory.anyOrUpperBound(type).composeWith(factory.upperBound(otherType));
		assertEquals(factory.allTop(), actual);
	}
	
	@Test
	public void emptyBeforeAny() {
		assertNoSimplificationKeepOrigin(factory.empty(), factory.any());
	}
	
	@Test
	public void emptyBeforeAnyOrType() {
		assertNoSimplificationKeepOrigin(factory.empty(), factory.anyOrType(type));
	}
	
	@Test
	public void emptyBeforeEmpty() {
		EdgeFunction<TestType> actual = factory.empty().composeWith(factory.empty());
		assertEquals(factory.empty(), actual);
	}
	
	@Test
	public void emptyBeforePop() {
		EdgeFunction<TestType> actual = factory.empty().composeWith(factory.pop());
		assertEquals(factory.allTop(), actual);
	}
	
	@Test
	public void emptyBeforePush() {
		assertNoSimplificationKeepOrigin(factory.empty(), factory.push());
	}
	
	@Test
	public void emptyBeforeType() {
		assertNoSimplificationKeepOrigin(factory.empty(), factory.type(type));
	}
	
	@Test
	public void emptyBeforeUpperBound() {
		assertNoSimplification(factory.empty(), factory.upperBound(type));
	}
	
	@Test
	public void initBeforeEmpty() {
		assertEquals(factory.init(), factory.init().composeWith(factory.empty()));
	}
	
	@Test
	public void initBeforePop() {
		assertEquals(factory.allTop(), factory.init().composeWith(factory.pop()));
	}
	
	@Test
	public void pushBeforeEmpty() {
		assertEquals(factory.allTop(), factory.push().composeWith(factory.empty()));
	}
	
	@Test
	public void pushBeforePop() {
		assertEquals(factory.id(), factory.push().composeWith(factory.pop()));
	}
	
	@Test
	public void typeBeforeAny() {
		assertEquals(factory.anyOrType(type), factory.type(type).composeWith(factory.any()));
	}
	
	@Test
	public void typeBeforeAnyOrTypeMatch() {
		assertEquals(factory.anyOrType(type), factory.type(type).composeWith(factory.anyOrType(type)));
	}
	
	@Test
	public void typeBeforeAnyOrTypeMismatch() {
		assertEquals(factory.allTop(), factory.type(type).composeWith(factory.anyOrType(otherType)));
	}
	
	@Test
	public void typeBeforeEmpty() {
		assertNoSimplificationKeepOrigin(factory.type(type), factory.empty());
	}
	
	@Test
	public void typeBeforePop() {
		assertNoSimplificationKeepOrigin(factory.type(type), factory.pop());
	}
	
	@Test
	public void typeBeforePush() {
		assertNoSimplificationKeepOrigin(factory.type(type), factory.push());
	}
	
	@Test
	public void typeBeforeTypeMatch() {
		assertEquals(factory.type(type), factory.type(type).composeWith(factory.type(type)));
	}

	@Test
	public void typeBeforeTypeMismatch() {
		assertEquals(factory.allTop(), factory.type(type).composeWith(factory.type(otherType)));
	}
	
	@Test
	public void typeBeforeUpperBoundMatch() {
		assertEquals(factory.type(type), factory.type(type).composeWith(factory.upperBound(superType)));
	}
	
	@Test
	public void typeBeforeUpperBoundMismatch() {
		assertEquals(factory.allTop(), factory.type(type).composeWith(factory.upperBound(subType)));
	}

	@Test
	public void upperBoundBeforeEmpty() {
		assertNoSimplificationKeepOrigin(factory.upperBound(type), factory.empty());
	}
	
	@Test
	public void upperBoundBeforePop() {
		assertNoSimplificationKeepOrigin(factory.upperBound(type), factory.pop());
	}
	
	@Test
	public void upperBoundBeforePush() {
		assertNoSimplificationKeepOrigin(factory.upperBound(type), factory.push());
	}
	
	@Test
	public void upperBoundBeforeTypeMatch() {
		assertEquals(factory.type(subType), factory.upperBound(type).composeWith(factory.type(subType)));
	}
	
	@Test
	public void upperBoundBeforeTypeMismatch() {
		assertEquals(factory.allTop(), factory.upperBound(type).composeWith(factory.type(superType)));
	}
	
	@Test
	public void upperBoundBeforeUpperBoundMatch() {
		assertEquals(factory.upperBound(subType), factory.upperBound(type).composeWith(factory.upperBound(subType)));
		assertEquals(factory.upperBound(type), factory.upperBound(type).composeWith(factory.upperBound(superType)));
	}
	
	@Test
	public void upperBoundBeforeUpperBoundMismatch() {
		assertEquals(factory.allTop(), factory.upperBound(type).composeWith(factory.upperBound(otherType)));
	}
	
	@Test
	public void pushUpperBoundPop() {
		assertEquals(factory.id(), factory.push().composeWith(factory.upperBound(type)).composeWith(factory.pop()));
	}
	
	@Test
	public void pushTypePop() {
		assertEquals(factory.id(), factory.push().composeWith(factory.type(type)).composeWith(factory.pop()));
	}
	
	@Test
	public void anyUpperBoundPop() {
		assertEquals(factory.any(), factory.any().composeWith(factory.upperBound(type)).composeWith(factory.pop()));
	}
	
	@Test
	public void anyTypePop() {
		assertEquals(factory.any(), factory.any().composeWith(factory.type(type)).composeWith(factory.pop()));
	}
	
	private static void assertNoSimplification(EdgeFunction<TestType> first, EdgeFunction<TestType> second) {
		ChainableEdgeFunction<TestType> firstChainable = (ChainableEdgeFunction) first;
		ChainableEdgeFunction<TestType> secondChainable = (ChainableEdgeFunction) second;
		EdgeFunction<TestType> actual = firstChainable.composeWith(secondChainable);
		
		assertTrue(actual instanceof ChainableEdgeFunction);
		ChainableEdgeFunction<TestType> chainedActual = (ChainableEdgeFunction<TestType>) actual;
		assertEquals(second.getClass(), chainedActual.getClass());
		assertTrue(chainedActual.chainedFunction() instanceof ChainableEdgeFunction);
	}
	
	private static void assertNoSimplificationKeepOrigin(EdgeFunction<TestType> first, EdgeFunction<TestType> second) {
		ChainableEdgeFunction<TestType> firstChainable = (ChainableEdgeFunction) first;
		ChainableEdgeFunction<TestType> secondChainable = (ChainableEdgeFunction) second;
		ChainableEdgeFunction<TestType> origin = mock(ChainableEdgeFunction.class);
		EdgeFunction<TestType> actual = firstChainable.chain(origin).composeWith(secondChainable);
		
		assertTrue(actual instanceof ChainableEdgeFunction);
		ChainableEdgeFunction<TestType> chainedActual = (ChainableEdgeFunction<TestType>) actual;
		assertEquals(second.getClass(), chainedActual.getClass());
		assertTrue(chainedActual.chainedFunction() instanceof ChainableEdgeFunction);
		ChainableEdgeFunction<TestType> nested = (ChainableEdgeFunction)chainedActual.chainedFunction();
		assertEquals(first.getClass(), nested.getClass());
		assertEquals(origin, nested.chainedFunction());
	}
	
	private static class TestType implements Type<TestType> {

		private TestType superType;
		
		public TestType() {
			this.superType = null;
		}
		
		public TestType(TestType superType) {
			this.superType = superType;
		}

		@Override
		public TestType join(TestType j) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public TestType meet(TestType m) {
			{
				TestType current = this;
				do {
					if(current == m)
						return this;
					current = current.superType;
				}while(current!=null);
			}
			
			{
				TestType current = m;
				do {
					if(current == this)
						return m;
					current = current.superType;
				}while(current!=null);
			}
			
			return new TestType();
		}
		
		@Override
		public String toString() {
			return "Type"+System.identityHashCode(this);
		}
	}
}
