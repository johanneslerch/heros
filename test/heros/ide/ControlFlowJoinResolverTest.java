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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

public class ControlFlowJoinResolverTest {

	private Factory<String> factory = new Factory(mock(JoinLattice.class));
	private PerAccessPathMethodAnalyzer<TestFact, Statement, TestMethod, AccessPathBundle<String>> analyzer;
	private Statement joinStmt;
	private ControlFlowJoinResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> sut;
	private TestFact fact;
	private InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>> callback;
	private Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> callEdgeResolver;

	@Before
	public void before() {
		analyzer = mock(PerAccessPathMethodAnalyzer.class);
		joinStmt = new Statement("joinStmt");
		sut = new ControlFlowJoinResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>>(mock(FactMergeHandler.class), analyzer, joinStmt);
		fact = new TestFact("value");
		callback = mock(InterestCallback.class);
		callEdgeResolver = mock(CallEdgeResolver.class);
	}

	@Test
	public void emptyIncomingFact() {
		sut.addIncoming(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), callEdgeResolver));
		verify(analyzer).processFlowFromJoinStmt(eq(new FactEdgeFnResolverStatementTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(
				fact, factory.id(), sut, joinStmt)));
		assertTrue(sut.isResolvedUnbalanced());
	}

	@Test
	public void resolveViaIncomingFact() {
		sut.resolve(factory.read("a"), callback);
		sut.addIncoming(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.prepend("a"), callEdgeResolver));
		verify(callback).interest(eq(analyzer), argThat(new ResolverArgumentMatcher(factory.read("a"))), eq(factory.id()));
	}

	@Test
	public void registerCallbackAtIncomingResolver() {
		Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = mock(Resolver.class);
		sut.addIncoming(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), resolver));
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
				argCallback.interest(analyzer, nestedResolver, factory.prepend("a"));
				return null;
			}
		}).when(resolver).resolve(eq(factory.read("a")), any(InterestCallback.class));
		
		sut.addIncoming(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.id(), resolver));
		sut.resolve(factory.read("a"), callback);
		
		verify(callback).interest(eq(analyzer), eq(nestedResolver), eq(factory.prepend("a")));
	}
	
	
	private class ResolverArgumentMatcher extends
			ArgumentMatcher<Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>>> {

		private EdgeFunction<AccessPathBundle<String>> edgeFn;

		public ResolverArgumentMatcher(EdgeFunction<AccessPathBundle<String>> edgeFn) {
			this.edgeFn = edgeFn;
		}

		@Override
		public boolean matches(Object argument) {
			ControlFlowJoinResolver resolver = (ControlFlowJoinResolver) argument;
			return resolver.isResolvedUnbalanced() && resolver.getResolvedFunction().equals(edgeFn) && resolver.getJoinStmt().equals(joinStmt);
		}
	}
}
