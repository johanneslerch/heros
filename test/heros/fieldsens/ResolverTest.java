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
package heros.fieldsens;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import heros.fieldsens.structs.WrappedFact;
import heros.utilities.Statement;
import heros.utilities.TestFact;
import heros.utilities.TestMethod;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

public class ResolverTest {

	private PerAccessPathMethodAnalyzer<String, TestFact, Statement, TestMethod> analyzer;
	private Statement joinStmt;
	private ControlFlowJoinResolver<String, TestFact, Statement, TestMethod> sut;
	private TestFact fact;
	private InterestCallback<String, TestFact, Statement, TestMethod> callback;
	private Resolver<String, TestFact, Statement, TestMethod> callEdgeResolver;

	@Before
	public void before() {
		analyzer = mock(PerAccessPathMethodAnalyzer.class);
		joinStmt = new Statement("joinStmt");
		sut = new ControlFlowJoinResolver<String, TestFact, Statement, TestMethod>(mock(FactMergeHandler.class), joinStmt, fact,
				new TestDebugger<String, TestFact, Statement, TestMethod>(), mock(ContextLogger.class), fact, new TestMethod("foo"));
		fact = new TestFact("value");
		callback = mock(InterestCallback.class);
		callEdgeResolver = mock(CallEdgeResolver.class);
	}
	
	@Test
	public void returnExistingResolver() {
		AccessPath<String> root = AccessPath.<String>empty();
		ResolverTemplate<String, TestFact, Statement, TestMethod, WrappedFact<String, TestFact, Statement, TestMethod>> xRes = 
				sut.getOrCreateNestedResolver(root.appendExcludedFieldReference("x"));
		ResolverTemplate<String, TestFact, Statement, TestMethod, WrappedFact<String, TestFact, Statement, TestMethod>> x2Res = 
				sut.getOrCreateNestedResolver(root.appendExcludedFieldReference("x"));
		
		assertEquals(xRes, x2Res);
	}
	
	@Test
	public void concreteResolverAfterExclusions() {
		AccessPath<String> root = AccessPath.<String>empty();
		ResolverTemplate<String, TestFact, Statement, TestMethod, WrappedFact<String, TestFact, Statement, TestMethod>> notXRes = 
				sut.getOrCreateNestedResolver(root.appendExcludedFieldReference("x"));
		ResolverTemplate<String, TestFact, Statement, TestMethod, WrappedFact<String, TestFact, Statement, TestMethod>> yRes = sut.getOrCreateNestedResolver(root.append("y"));
		ResolverTemplate<String, TestFact, Statement, TestMethod, WrappedFact<String, TestFact, Statement, TestMethod>> notXThenYRes = notXRes.getOrCreateNestedResolver(root.append("y"));
		
		assertEquals(yRes, notXThenYRes);
	}
	
	@Test
	public void diamondShapedExclusions() {
		AccessPath<String> root = AccessPath.<String>empty();
		
		ResolverTemplate<String, TestFact, Statement, TestMethod, WrappedFact<String, TestFact, Statement, TestMethod>> notXRes = 
				sut.getOrCreateNestedResolver(root.appendExcludedFieldReference("x"));
		ResolverTemplate<String, TestFact, Statement, TestMethod, WrappedFact<String, TestFact, Statement, TestMethod>> notYRes = 
				sut.getOrCreateNestedResolver(root.appendExcludedFieldReference("Y"));
		
		ResolverTemplate<String, TestFact, Statement, TestMethod, WrappedFact<String, TestFact, Statement, TestMethod>> notXYRes = 
				notXRes.getOrCreateNestedResolver(root.appendExcludedFieldReference("x", "y"));
		ResolverTemplate<String, TestFact, Statement, TestMethod, WrappedFact<String, TestFact, Statement, TestMethod>> notYXRes = 
				notYRes.getOrCreateNestedResolver(root.appendExcludedFieldReference("x", "y"));
		
		assertEquals(notXYRes, notYXRes);
	}
}
