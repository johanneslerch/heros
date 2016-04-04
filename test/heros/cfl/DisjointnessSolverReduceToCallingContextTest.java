/*******************************************************************************
 * Copyright (c) 2016 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.cfl;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoAssertionError;

import heros.cfl.DisjointnessSolver.QueryListener;

public class DisjointnessSolverReduceToCallingContextTest {

	
	ProducingTerminal f = new ProducingTerminal("f");
	ConsumingTerminal f̅ = new ConsumingTerminal("f");
	ExclusionTerminal not_f = new ExclusionTerminal("f");
	ProducingTerminal g = new ProducingTerminal("g");
	ConsumingTerminal g̅ = new ConsumingTerminal("g");
	ExclusionTerminal not_g = new ExclusionTerminal("g");
	ProducingTerminal h = new ProducingTerminal("h");
	ConsumingTerminal h̄ = new ConsumingTerminal("h");
	ExclusionTerminal not_h = new ExclusionTerminal("h");
	ProducingTerminal i = new ProducingTerminal("i");

	NonTerminal A = new NonTerminal("A");
	NonTerminal B = new NonTerminal("B");
	NonTerminal C = new NonTerminal("C");
	NonTerminal D = new NonTerminal("D");
	NonTerminal E = new NonTerminal("E");
	NonTerminal U = new NonTerminal("U");
	NonTerminal V = new NonTerminal("V");
	NonTerminal X = new NonTerminal("X");
	NonTerminal Y = new NonTerminal("Y");
	NonTerminal Z = new NonTerminal("Z");
	RegularRule consumeFOnX = new RegularRule(X, f̅);
	private QueryListener listener;
	private Rule challenge;
	private RegularOverApproximizer approximizer;
	private DisjointnessSolver solver = new DisjointnessSolver();
	
	@Test
	public void test1() {
		B.addRule(new RegularRule(B, f̅));
		B.addRule(new RegularRule(C, new ExclusionTerminal("f", "g"), f, f));
		Y.addRule(new ConstantRule(f̅, f̅));
		C.addRule(new ConstantRule(h));
		
		Z.addRule(new RegularRule(C, new ExclusionTerminal("f", "g"), f, f));
		X.addRule(new RegularRule(B, f̅));
		
		solvable(Z, new RegularRule(Y, g));
		unsolvable(X, new RegularRule(Y, g));
	}
	
/* --------------------------------------------------------*/
	
	private void solvable(NonTerminal callingCtx, Rule rule) {
		delayed(callingCtx, rule);
		try {
			Mockito.verify(listener).solved();
		} catch(MockitoAssertionError e) {
			throw new AssertionError(rule+" should be solvable, but was not. Given rule set:\n"+new ToStringRuleVisitor(rule), e);
		}
	}
	
	private void unsolvable(NonTerminal callingCtx, final Rule rule) {
		runSearch(new QueryListener() {
			@Override
			public void solved() {
				fail(rule+" should not be solvable, but was solved. Given rule set:\n"+new ToStringRuleVisitor(rule));
			}
		}, callingCtx, rule);
	}
	
	private void assertUnsolved() {
		try {
			verify(listener, never()).solved();
		} catch(MockitoAssertionError e) {
			throw new AssertionError(challenge+" should not be solvable, but was solved. Given rule set:\n"+new ToStringRuleVisitor(challenge), e);
		}
	}
	
	private void assertSolved() {
		try {
			verify(listener).solved();
		} catch(MockitoAssertionError e) {
			throw new AssertionError(challenge+" should be solvable, but was not. Given rule set:\n"+new ToStringRuleVisitor(challenge), e);
		}
	}
	
	private void delayed(NonTerminal callingCtx, Rule rule) {
		listener = mock(QueryListener.class);
		runSearch(listener, callingCtx, rule);
	}
	
	private void runSearch(QueryListener listener, NonTerminal callingCtx, Rule rule) {
		this.challenge = rule;
		approximizer = new RegularOverApproximizer();
		approximizer.approximate(rule);
		solver.reduceToCallingContext(callingCtx, rule, listener);
	}
}
