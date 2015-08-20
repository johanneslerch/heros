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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import heros.JoinLattice;
import heros.fieldsens.FactMergeHandler;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.edgefunc.EdgeIdentity;
import heros.ide.edgefunc.fieldsens.AccessPathBundle;
import heros.ide.edgefunc.fieldsens.Factory;
import heros.ide.structs.FactEdgeFnResolverStatementTuple;
import heros.ide.structs.FactEdgeFnResolverTuple;
import heros.utilities.Statement;
import heros.utilities.TestFact;
import heros.utilities.TestMethod;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class ResolverIntegrationTest {

	private Factory<String> factory = new Factory(mock(JoinLattice.class));
	private PerAccessPathMethodAnalyzer<TestFact, Statement, TestMethod, AccessPathBundle<String>> analyzer;
	private TestFact fact;
	private InterestCallback<TestFact, Statement, TestMethod, AccessPathBundle<String>> callback;
	private FactMergeHandler<TestFact> factMergeHandler;
	
	@Before
	public void before() {
		analyzer = mock(PerAccessPathMethodAnalyzer.class);
		fact = new TestFact("value");
		callback = mock(InterestCallback.class);
		factMergeHandler = mock(FactMergeHandler.class);
	}

	@Test
	public void balancedTraversalOnResolveViaCallEdgeResolver() {
		CallEdgeResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> callerResolver = mock(CallEdgeResolver.class);
		CallEdgeResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> calleeResolver = mock(CallEdgeResolver.class);
		
		Statement returnSite = new Statement("returnSite");
		ReturnSiteResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> returnSiteResolver = new ReturnSiteResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>>(
				factMergeHandler, analyzer, returnSite);
		returnSiteResolver.addIncoming(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.read("x"), calleeResolver), 
				callerResolver, factory.read("z"));
		
		verify(analyzer).scheduleEdgeTo(new FactEdgeFnResolverStatementTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(
				fact, EdgeIdentity.<AccessPathBundle<String>>v(), returnSiteResolver, returnSite));
		
		Statement callSite = new Statement("callSite");
		PerAccessPathMethodAnalyzer<TestFact, Statement, TestMethod, AccessPathBundle<String>> callerAnalyzer = mock(PerAccessPathMethodAnalyzer.class);
		when(callerAnalyzer.getCallEdgeResolver()).thenReturn(callerResolver);
		CallEdge<TestFact, Statement, TestMethod, AccessPathBundle<String>> callEdge = new CallEdge<TestFact, Statement, TestMethod, AccessPathBundle<String>>(
				callerAnalyzer, fact, fact,	factory.id(), returnSiteResolver, callSite);
		PerAccessPathMethodAnalyzer<TestFact, Statement, TestMethod, AccessPathBundle<String>> interestedAnalyzer = mock(PerAccessPathMethodAnalyzer.class);
		when(interestedAnalyzer.getConstraint()).thenReturn(factory.read("y"));
		callEdge.registerInterestCallback(interestedAnalyzer);
		
		verify(callerResolver).resolve(eq(factory.read("z").composeWith(factory.read("x")).composeWith(factory.read("y"))), any(InterestCallback.class));
	}
	
	@Test
	public void balancedTraversalOnResolveViaCallEdgeResolver2() {
		CallEdgeResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> callerResolver = mock(CallEdgeResolver.class);
		CallEdgeResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> calleeResolver = mock(CallEdgeResolver.class);
		
		Statement returnSite = new Statement("returnSite");
		ReturnSiteResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> returnSiteResolver = new ReturnSiteResolver<TestFact, Statement, TestMethod, AccessPathBundle<String>>(
				factMergeHandler, analyzer, returnSite);
		returnSiteResolver.addIncoming(new FactEdgeFnResolverTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(fact, factory.prepend("x"), calleeResolver), 
				callerResolver, factory.read("z"));
		
		verify(analyzer).scheduleEdgeTo(new FactEdgeFnResolverStatementTuple<TestFact, Statement, TestMethod, AccessPathBundle<String>>(
				fact, EdgeIdentity.<AccessPathBundle<String>>v(), returnSiteResolver, returnSite));
				
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>> resolver = (Resolver<TestFact, Statement, TestMethod, AccessPathBundle<String>>) invocation.getArguments()[1];
				
				Statement callSite = new Statement("callSite");
				PerAccessPathMethodAnalyzer<TestFact, Statement, TestMethod, AccessPathBundle<String>> callerAnalyzer = mock(PerAccessPathMethodAnalyzer.class);
				when(callerAnalyzer.getCallEdgeResolver()).thenReturn(callerResolver);
				CallEdge<TestFact, Statement, TestMethod, AccessPathBundle<String>> callEdge = new CallEdge<TestFact, Statement, TestMethod, AccessPathBundle<String>>(
						callerAnalyzer, fact, fact,	factory.read("x"), resolver, callSite);
				PerAccessPathMethodAnalyzer<TestFact, Statement, TestMethod, AccessPathBundle<String>> interestedAnalyzer = mock(PerAccessPathMethodAnalyzer.class);
				when(interestedAnalyzer.getConstraint()).thenReturn(factory.read("y"));
				callEdge.registerInterestCallback(interestedAnalyzer);
				
				return null;
			}
		}).when(callback).interest(any(PerAccessPathMethodAnalyzer.class), any(Resolver.class), eq(factory.prepend("x")));
		returnSiteResolver.resolve(factory.read("x"), callback);
		verify(callerResolver).resolve(eq(factory.read("z").composeWith(factory.read("y"))), any(InterestCallback.class));
	}
}
