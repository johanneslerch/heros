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
package heros.ide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.List;

import heros.JoinLattice;
import heros.fieldsens.FactMergeHandler;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.edgefunc.fieldsens.AccessPathBundle;
import heros.ide.edgefunc.fieldsens.Factory;
import heros.ide.structs.FactEdgeFnResolverTuple;
import heros.ide.structs.FactEdgeFnResolverStatementTuple;
import heros.utilities.Statement;
import heros.utilities.TestFact;
import heros.utilities.TestMethod;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Lists;

public class ReturnSiteResolverTest {

	private Factory<String> factory = new Factory(mock(JoinLattice.class));
	private PerAccessPathMethodAnalyzer<TestFact, Statement, TestMethod, AccessPathBundle<String>> analyzer;
	private Statement returnSite;
	private ReturnSiteResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> sut;
	private TestFact fact;
	private InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>> callback;
	private Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> callEdgeResolver;

	@Before
	public void before() {
		analyzer = mock(PerAccessPathMethodAnalyzer.class);
		returnSite = new Statement("returnSite");
		sut = new ReturnSiteResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>>(mock(FactMergeHandler.class), analyzer, returnSite);
		fact = new TestFact("value");
		callback = mock(InterestCallback.class);
		callEdgeResolver = mock(CallEdgeResolver.class);
	}

	@Test
	public void emptyIncomingFact() {
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), callEdgeResolver),
				callEdgeResolver, factory.id());
		verify(analyzer).scheduleEdgeTo(eq(new FactEdgeFnResolverStatementTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), sut, returnSite)));
		assertTrue(sut.isResolvedUnbalanced());
	}

	@Test
	public void resolveViaIncomingFact() {
		sut.resolve(factory.read("a"), callback);
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.prepend("a"), callEdgeResolver), 
				callEdgeResolver, factory.id());
		verify(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(factory.read("a"))), eq(factory.id()));
	}

	@Test
	public void registerCallbackAtIncomingResolver() {
		Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = mock(Resolver.class);
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), resolver), 
				callEdgeResolver, factory.id());
		sut.resolve(factory.read("a"), callback);
		verify(resolver).resolve(eq(factory.read("a")), any(InterestCallback.class));
	}
	
	@Test
	public void resolveViaIncomingResolver() {
		Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = mock(Resolver.class);
		final Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> nestedResolver = mock(Resolver.class);
		Mockito.doAnswer(new Answer(){
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>> argCallback = 
						(InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>>) invocation.getArguments()[1];
				argCallback.interest(analyzer, nestedResolver, factory.read("x"));
				return null;
			}
		}).when(resolver).resolve(eq(factory.read("a")), any(InterestCallback.class));
		
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), resolver), 
				callEdgeResolver, factory.id());
		sut.resolve(factory.read("a"), callback);
		
		verify(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(factory.read("a"))), eq(factory.id()));
	}
	
	@Test
	public void resolveViaLateInterestAtIncomingResolver() {
		Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = mock(Resolver.class);
		final Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> nestedResolver = mock(Resolver.class);
		final List<InterestCallback> callbacks = Lists.newLinkedList();
		
		Mockito.doAnswer(new Answer(){
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>> argCallback = 
						(InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>>) invocation.getArguments()[1];
				callbacks.add(argCallback);
				return null;
			}
		}).when(resolver).resolve(eq(factory.read("a")), any(InterestCallback.class));
		
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), resolver), 
				callEdgeResolver, factory.id());
		sut.resolve(factory.read("a"), callback);
		
		verify(callback, never()).interest(any(PerAccessPathMethodAnalyzer.class), any(Resolver.class), any(EdgeFunction.class));
		
		assertEquals(1, callbacks.size());
		Resolver transitiveResolver = mock(Resolver.class);
		callbacks.get(0).interest(analyzer, transitiveResolver, factory.prepend("a"));
		verify(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(factory.read("a"))), eq(factory.id()));
	}
	
	@Test
	public void resolveViaDelta() {
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), callEdgeResolver), 
				callEdgeResolver, factory.prepend("a"));
		sut.resolve(factory.read("a"), callback);
		verify(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(factory.read("a"))), eq(factory.id()));
	}
	
	@Test
	public void resolveViaDeltaTwice() {
		final InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>> innerCallback = mock(InterestCallback.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				ReturnSiteResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = (ReturnSiteResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>>) invocation.getArguments()[1];
				resolver.resolve(factory.read("a").composeWith(factory.read("b")), innerCallback);
				return null;
			}
		}).when(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(factory.read("a"))), eq(factory.id()));
		
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), callEdgeResolver), 
				callEdgeResolver, factory.prepend("b").composeWith(factory.prepend("a")));
		sut.resolve(factory.read("a"), callback);
		
		verify(innerCallback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(factory.read("a").composeWith(factory.read("b")))), 
				eq(factory.id()));
	}
	
	@Test
	public void resolveViaDeltaAndThenViaCallSite() {
		final InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>> innerCallback = mock(InterestCallback.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				ReturnSiteResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = (ReturnSiteResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>>) invocation.getArguments()[1];
				resolver.resolve(factory.read("a").composeWith(factory.read("b")), innerCallback);
				return null;
			}
		}).when(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(factory.read("a"))), eq(factory.id()));
		
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), callEdgeResolver), 
				callEdgeResolver, factory.prepend("a"));
		sut.resolve(factory.read("a"), callback);
		verify(innerCallback).continueBalancedTraversal(factory.prepend("a"));
	}

	@Test
	public void resolveViaCallEdgeResolverAtCallSite() {
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), callEdgeResolver), 
				callEdgeResolver, factory.id());
		sut.resolve(factory.read("a"), callback);
		verify(callback).continueBalancedTraversal(factory.id());
	}
	
	@Test
	public void resolveViaResolverAtCallSite() {
		Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = mock(Resolver.class);
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), callEdgeResolver), 
				resolver, factory.id());
		sut.resolve(factory.read("a"), callback);
		verify(resolver).resolve(eq(factory.read("a")), any(InterestCallback.class));
	}
	
	@Test
	public void resolveViaResolverAtCallSiteTwice() {
		Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = mock(Resolver.class);
		final Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> nestedResolver = mock(Resolver.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback innerCallback = (InterestCallback) invocation.getArguments()[1];
				innerCallback.interest(analyzer, nestedResolver, factory.prepend("a"));
				return null;
			}
		}).when(resolver).resolve(eq(factory.read("a")), any(InterestCallback.class));
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback innerCallback = (InterestCallback) invocation.getArguments()[1];
				innerCallback.interest(analyzer, nestedResolver, factory.prepend("a").composeWith(factory.prepend("b")));
				return null;
			}
		}).when(nestedResolver).resolve(eq(factory.read("b")), any(InterestCallback.class));
		
		final InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>> secondCallback = mock(InterestCallback.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = (Resolver) invocation.getArguments()[1];
				resolver.resolve(factory.read("b"), secondCallback);
				return null;
			}
			
		}).when(callback).interest(eq(analyzer), eq(nestedResolver), eq(factory.prepend("a")));
		
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), callEdgeResolver), 
				resolver, factory.id());
		sut.resolve(factory.read("a"), callback);
		
		verify(secondCallback).interest(eq(analyzer), eq(nestedResolver),
				eq(factory.prepend("a").composeWith(factory.prepend("b"))));
	}
	
	@Test
	public void resolveAsEmptyViaIncomingResolver() {
		Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = mock(Resolver.class);		
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback innerCallback = (InterestCallback) invocation.getArguments()[1];
				innerCallback.continueBalancedTraversal(factory.id());
				return null;
			}
		}).when(resolver).resolve(eq(factory.read("a")), any(InterestCallback.class));

		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), resolver), 
				callEdgeResolver, factory.overwrite("a"));
		sut.resolve(factory.read("a"), callback);
		
		verify(callback, never()).continueBalancedTraversal(any(EdgeFunction.class));
		verify(callback, never()).interest(any(PerAccessPathMethodAnalyzer.class), any(Resolver.class), any(EdgeFunction.class));
	}
	
	@Test
	public void resolveViaCallSiteResolver() {
		Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = mock(Resolver.class);
		
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), callEdgeResolver), 
				resolver, factory.id());
		sut.resolve(factory.read("a"), callback);
		
		verify(resolver).resolve(eq(factory.read("a")), any(InterestCallback.class));
	}
	
	@Test
	public void incomingZeroCallEdgeResolver() {
		Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = mock(Resolver.class);
		ZeroCallEdgeResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> zeroResolver = mock(ZeroCallEdgeResolver.class); 
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(
				fact, factory.id(), zeroResolver), resolver, factory.id());
				
		sut.resolve(factory.read("a"), callback);
		
		verify(resolver, never()).resolve(any(EdgeFunction.class), any(InterestCallback.class));
		verify(callback, never()).interest(any(PerAccessPathMethodAnalyzer.class), any(Resolver.class), any(EdgeFunction.class));
		verify(callback, never()).continueBalancedTraversal(any(EdgeFunction.class));
	}
	
	@Test
	public void keepIncEdgeOnContinueBalancedTraversal() {
		Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = mock(Resolver.class);
		final InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>> secondCallback = mock(InterestCallback.class);
		sut.addIncomingWithoutCheck(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(
				fact, factory.prepend("y"), resolver), callEdgeResolver, factory.read("z"));
		
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = (Resolver) invocation.getArguments()[1];
				resolver.resolve(factory.read("y").composeWith(factory.read("x")), secondCallback);
				return null;
			}
		}).when(callback).interest(eq(analyzer), 
				argThat(new ReturnSiteResolverArgumentMatcher(factory.read("y"))), eq(factory.id()));
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>> callback = 
						(InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>>) invocation.getArguments()[1];
				callback.continueBalancedTraversal(factory.read("u"));
				return null;
			}
		}).when(resolver).resolve(eq(factory.read("x")), any(InterestCallback.class));
		
		sut.resolve(factory.read("y"), callback);
		verify(secondCallback).continueBalancedTraversal(eq(factory.read("z").composeWith(factory.read("u")).composeWith(factory.prepend("y"))));
	}
	
	private class ReturnSiteResolverArgumentMatcher extends
			ArgumentMatcher<ReturnSiteResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>>> {

		private EdgeFunction<AccessPathBundle<String>> edgeFn;

		public ReturnSiteResolverArgumentMatcher(EdgeFunction<AccessPathBundle<String>> edgeFn) {
			this.edgeFn = edgeFn;
		}

		@Override
		public boolean matches(Object argument) {
			ReturnSiteResolver resolver = (ReturnSiteResolver) argument;
			return resolver.isResolvedUnbalanced() && resolver.getResolvedFunction().equals(edgeFn) && resolver.getReturnSite().equals(returnSite);
		}
	}
}
