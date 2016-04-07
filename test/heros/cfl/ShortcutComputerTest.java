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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import heros.cfl.ShortcutComputer.NonTerminalContext;
import heros.cfl.ShortcutComputer.ContextListener;
import heros.cfl.ShortcutComputer.Edge;
import heros.solver.Pair;

import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ShortcutComputerTest {

	ShortcutComputer solver = new ShortcutComputer();
	
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
	NonTerminal W = new NonTerminal("W");
	NonTerminal X = new NonTerminal("X");
	NonTerminal Y = new NonTerminal("Y");
	NonTerminal Z = new NonTerminal("Z");
	
	@Test
	public void substitutionTrivial() {
		X.addRule(new ConstantRule(f));
		assertSubstitution(new ConstantRule(f), X);
	}
	
	@Test
	public void substitution() {
		X.addRule(new RegularRule(Y, g̅));
		Y.addRule(new RegularRule(Z, g));
		Z.addRule(new RegularRule(X, f));
		assertSubstitution(new RegularRule(X, f), X);
	}
	
	@Test
	public void terminateProducingLoop() {
		X.addRule(new RegularRule(X, f));
		assertSubstitution(new RegularRule(X, f), X);
	}

	@Test
	public void substitutionRequiresLoop() {
		X.addRule(new RegularRule(Y, f̅));
		Y.addRule(new RegularRule(Y, g̅));
		Y.addRule(new ConstantRule(h, f, g, g));
		assertSubstitution(new ConstantRule(h), X);
	}

	@Test
	public void substitutionContextFree() {
		X.addRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g̅}));
		Y.addRule(new ContextFreeRule(new Terminal[] {h}, Z, new Terminal[] {g}));
		Z.addRule(new RegularRule(X, f));
		assertSubstitution(new ContextFreeRule(new Terminal[] {f, h}, X, new Terminal[] {f}), X);
	}
	
	@Test
	public void substitutionLinearized() {
		X.addRule(new RegularRule(Y, f̅));
		Y.addRule(new RegularRule(Y, g̅));
		Y.addRule(new RegularRule(Z, g̅));
		Z.addRule(new RegularRule(Z, g));
		Z.addRule(new ConstantRule(h, f, g, g, g));
		assertSubstitution(new ConstantRule(h), X);
	}
	
	@Test
	public void substitutionNonLinear() {
		X.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(Y, f̅)));
		Y.addRule(new RegularRule(Y, g̅));
		Y.addRule(new ConstantRule(g̅));
		Z.addRule(new RegularRule(Z, g));
		Z.addRule(new ConstantRule(h, f, g, g, g));
		assertSubstitution(new ConstantRule(h), X);
	}
	
	@Test
	public void substitutionNonLinearRightProducing() {
		X.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(Y)));
		Y.addRule(new RegularRule(X, f));
		assertSubstitution(new NonLinearRule(new RegularRule(Z), new RegularRule(X, f)), X);
	}
	
	@Test
	public void substitutionHiddenIdentityRule() {
		X.addRule(new RegularRule(X, g̅));
		X.addRule(new RegularRule(X, g̅, g));

		NonTerminal intermediateNonTerminal = solver.getIntermediateNonTerminal(X, new RegularRule(X, g̅));		
		assertSubstitution(new RegularRule(intermediateNonTerminal, g), X);
	}
	
	@Test
	public void substitutionHiddenIdentityRule2() {
		X.addRule(new RegularRule(Y, g̅));
		Y.addRule(new RegularRule(Z, g));
		Z.addRule(new RegularRule(Y, g̅));
		assertSubstitution(Sets.<Rule> newHashSet(), X);
	}

	@Test
	public void substitutionEmptyInNonLinearRule1() {
		X.addRule(new NonLinearRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[0]), new RegularRule(Y)));
		Y.addRule(new ConstantRule());
		assertSubstitution(new ConstantRule(f), X);
	}
	
	@Test
	public void substitutionEmptyInNonLinearRule2() {
		X.addRule(new NonLinearRule(new RegularRule(Y), new RegularRule(Y)));
		Y.addRule(new ConstantRule());
		Y.addRule(new ConstantRule(f));
		assertSubstitution(Sets.<Rule> newHashSet(new ConstantRule(f), new RegularRule(Y, f)), X);
	}
	
	@Test
	public void substitutionEmptyInNonLinearRule3() {
		B.addRule(new ConstantRule());
		A.addRule(new NonLinearRule(new RegularRule(D), new RegularRule(B)));
		D.addRule(new ConstantRule(f));
		C.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(B)));
		
		assertSubstitution(new ConstantRule(f), C);
	}
	
	@Test
	public void substitutionHiddenIdentityRule3() {
		X.addRule(new RegularRule(X, g̅));
		X.addRule(new RegularRule(X, g̅, g));
		X.addRule(new RegularRule(X, g̅, g̅, g, g));
		
		NonTerminal intermediateNonTerminal1 = solver.getIntermediateNonTerminal(X, new RegularRule(X, g̅));
		NonTerminal intermediateNonTerminal2 = solver.getIntermediateNonTerminal(X, new RegularRule(X, g̅, g̅));
		assertSubstitution(Sets.<Rule> newHashSet(
				new RegularRule(intermediateNonTerminal1, g), 
				new RegularRule(intermediateNonTerminal2, g, g),
				new RegularRule(intermediateNonTerminal2, g)), X);
	}
	
	@Test
	public void substitutionSelfSatisfyingLoop() {
		X.addRule(new RegularRule(Y, f̅,f̅,f̅,f̅,f̅));
		Y.addRule(new ConstantRule(f));
		Y.addRule(new RegularRule(Y, f̅, f, f));
		
		NonTerminal intermediateNonTerminal = solver.getIntermediateNonTerminal(Y, new RegularRule(Y, f̅));
		assertSubstitution(new RegularRule(intermediateNonTerminal, f), X);
	}
	
	@Test
	public void complexNonLinear() {
		Z.addRule(new RegularRule(X, f̅));
		X.addRule(new RegularRule(Y, g̅));
		Y.addRule(new ConstantRule());
		D.addRule(new RegularRule(C, not_h));
		C.addRule(new RegularRule(B, g̅));
		B.addRule(new RegularRule(A, g, g));
		A.addRule(new RegularRule(E, h, f));
		
		U.addRule(new NonLinearRule(new RegularRule(D), new RegularRule(Z)));
		
		assertSubstitution(Sets.<Rule> newHashSet(), Z);
		assertSubstitution(new RegularRule(A, g), D);
		assertSubstitution(new RegularRule(E, h), U);
	}

	@Test
	public void nestedNonLinear1() {
		X.addRule(new RegularRule(Y, f̅, f̅));
		X.addRule(new RegularRule(X, f̅));
		Y.addRule(new ConstantRule(g̅));
		C.addRule(new RegularRule(U, g));
		U.addRule(new ConstantRule(f));
		A.addRule(new RegularRule(B, f));
		B.addRule(new ConstantRule(h, h, f));
		
		D.addRule(new NonLinearRule(new RegularRule(C), new RegularRule(X)));
		E.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(D)));
		V.addRule(new RegularRule(E, h̄));
		
		assertSubstitution(new ConstantRule(h), V);
	}
	
	@Test
	public void nestedNonLinear2() {
		X.addRule(new RegularRule(Y, f̅, f̅));
		Y.addRule(new ConstantRule(g̅));
		C.addRule(new RegularRule(U, g));
		U.addRule(new ConstantRule(f));
		A.addRule(new RegularRule(B, f));
		B.addRule(new ConstantRule(h));
		
		D.addRule(new NonLinearRule(new RegularRule(C), new RegularRule(X)));
		E.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(D)));
		
		assertSubstitution(new ConstantRule(h), E);
	}
	
	@Test
	public void nestedNonLinear3() {
		X.addRule(new ConstantRule(f̅));
		D.addRule(new ConstantRule(g̅));
		A.addRule(new ConstantRule(h, f, g));
		
		C.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(D)));
		E.addRule(new NonLinearRule(new RegularRule(D), new RegularRule(X)));
		U.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(E)));
		V.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(X)));

		assertSubstitution(new ConstantRule(h, f), C);
		assertSubstitution(Sets.<Rule> newHashSet(), E);
		assertSubstitution(new ConstantRule(h), U);
		assertSubstitution(Sets.<Rule> newHashSet(), V);
	}
	
	@Test
	public void nestedNonLinear4() {
		Y.addRule(new ConstantRule(f̅));
		X.addRule(new RegularRule(Z, g̅));
		Z.addRule(new ConstantRule(h̄));
		A.addRule(new ConstantRule(h));
		A.addRule(new ConstantRule(f, f, g));
		
		E.addRule(new NonLinearRule(new RegularRule(X), new RegularRule(Y)));
		D.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(E)));
		assertSubstitution(Sets.<Rule>newHashSet(), D);
	}
	
	@Test
	public void nestedNonLinear5a() {
		A.addRule(new RegularRule(B, g̅));
		B.addRule(new ConstantRule());
		
		Z.addRule(new RegularRule(Z, g));
		D.addRule(new RegularRule(Z, g));
		E.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(A, f)));
		
		Y.addRule(new NonLinearRule(new RegularRule(E), new RegularRule(B, f̅)));
		assertSubstitution(new RegularRule(Z, g), Y);
	}
	
	@Test
	public void nestedNonLinear5b() {
		A.addRule(new RegularRule(B, g̅));
		B.addRule(new ConstantRule());
		
		Z.addRule(new RegularRule(Z, g));
		D.addRule(new RegularRule(Z, g));
		E.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(A, f)));
		
		Y.addRule(new NonLinearRule(new RegularRule(E), new RegularRule(B)));
		X.addRule(new RegularRule(Y, f̅));
		assertSubstitution(new NonLinearRule(new RegularRule(Z), new RegularRule(A, f)), Y);
		assertSubstitution(new RegularRule(Z, g), X);
	}
	
	@Test
	public void nestedNonLinear6a() {
		A.addRule(new NonLinearRule(new RegularRule(B), new RegularRule(C, f̅)));
		C.addRule(new ConstantRule(g̅));
		B.addRule(new NonLinearRule(new RegularRule(D), new RegularRule(E)));
		E.addRule(new ConstantRule(h̄));
		D.addRule(new RegularRule(D, h));
		D.addRule(new RegularRule(D, g));
		D.addRule(new ConstantRule(f, f));
		assertSubstitution(new ConstantRule(f), A);
	}
	
	@Test
	public void nestedNonLinear6b() {
		A.addRule(new NonLinearRule(new NonLinearRule(new RegularRule(D), new RegularRule(E)), new RegularRule(C, f̅)));
		C.addRule(new ConstantRule(g̅));
		E.addRule(new ConstantRule(h̄));
		D.addRule(new RegularRule(D, h));
		D.addRule(new RegularRule(D, g));
		D.addRule(new ConstantRule(f, f));
		assertSubstitution(new ConstantRule(f), A);
	}
	
	@Test
	public void nestedNonLinear6c() {
		A.addRule(new NonLinearRule(new RegularRule(D), new NonLinearRule(new RegularRule(E), new RegularRule(C, f̅))));
		C.addRule(new ConstantRule(g̅));
		E.addRule(new ConstantRule(h̄));
		D.addRule(new RegularRule(D, h));
		D.addRule(new RegularRule(D, g));
		D.addRule(new ConstantRule(f, f));
		assertSubstitution(new ConstantRule(f), A);
	}
	
	@Test
	public void nestedNonLinear7() {
		B.addRule(new ConstantRule());
		E.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(A, f)));
		Y.addRule(new NonLinearRule(new RegularRule(E), new RegularRule(B)));
		assertSubstitution(new NonLinearRule(new RegularRule(Z), new RegularRule(A, f)), Y);
		assertConditionalSubstitution(Optional.<Rule> of(new RegularRule(A, f)), new RegularRule(A, f), B);
	}
	
	@Test
	public void nestedNonLinear8() {
		A.addRule(new NonLinearRule(new RegularRule(B), new NonLinearRule(new RegularRule(C, f), new RegularRule(D, f̅))));
		D.addRule(new ConstantRule());
		C.addRule(new ConstantRule(h̄));
		B.addRule(new ConstantRule(h, h));
		assertSubstitution(new ConstantRule(h), A);
	}
	
	@Test
	public void nestedNonLinear9() {
		A.addRule(new ConstantRule());
		B.addRule(new RegularRule(A, g̅, not_f));
		C.addRule(new ConstantRule());
		C.addRule(new RegularRule(C, g));
		C.addRule(new RegularRule(C, f));
		
		
		V.addRule(new NonLinearRule(new RegularRule(C), new RegularRule(A, g̅)));
		X.addRule(new NonLinearRule(new RegularRule(C), new RegularRule(A, g̅, not_f)));
		Y.addRule(new NonLinearRule(new RegularRule(C), new RegularRule(B, f̅)));
		
		assertSubstitution(Sets.<Rule>newHashSet(new RegularRule(C, g), new RegularRule(C, f)), V);
		assertSubstitution(new RegularRule(C, g), X);
		assertSubstitution(Sets.<Rule>newHashSet(), Y);
	}
	
//	@Test
//	public void nestedNonLinear10() {
////		C → V<f̅>
//		C.addRule(new RegularRule(V, f̅));
////		V → V¬<f> | V<f> | V<f̅> | ZB | Z<g>		=SP
//		V.addRule(new RegularRule(V, not_f));
//		V.addRule(new RegularRule(V, f));
//		V.addRule(new RegularRule(V, f̅));
//		V.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(B)));
//		V.addRule(new RegularRule(Z, g));
////		Z → 0
//		Z.addRule(new ConstantRule(new ProducingTerminal("0")));
////		B → AD<f> | AD<f̅> | A¬<f> | A<f> | A | AD<f̅>¬<f> | AD<f̅><f> | AD
//		B.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(D, f)));
//		B.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(D, f̅)));
//		B.addRule(new RegularRule(A, not_f));
//		B.addRule(new RegularRule(A, f));
//		B.addRule(new RegularRule(A));
//		B.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(D, f̅, not_f)));
//		B.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(D, f̅, f)));
//		B.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(D)));
////		A → <g> | <<g>W> | <g><f>
//		A.addRule(new ConstantRule(g));
//		A.addRule(new ContextFreeRule(new Terminal[] {g}, W, new Terminal[0]));
//		A.addRule(new ConstantRule(g,f));
////		D → ε | <f> | ¬<f> | D<f> | D<f̅> | D<f̅>¬<f> | D<f̅><f>
//		D.addRule(new ConstantRule());
//		D.addRule(new ConstantRule(f));
//		D.addRule(new ConstantRule(not_f));
//		D.addRule(new RegularRule(D, f));
//		D.addRule(new RegularRule(D, f̅));
//		D.addRule(new RegularRule(D, f̅, not_f));
//		D.addRule(new RegularRule(D, f̅, f));
////		W → E | E¬<f><f> | E<f><f> | E¬<f> | E<f> | E<f̅>   = {RS E}
//		W.addRule(new RegularRule(E));
//		W.addRule(new RegularRule(E, not_f, f));
//		W.addRule(new RegularRule(E, f, f));
//		W.addRule(new RegularRule(E, f));
//		W.addRule(new RegularRule(E, f̅));
////		E → ε | E¬<f> | E<f>
//		E.addRule(new ConstantRule());
//		E.addRule(new RegularRule(E, not_f));
//		E.addRule(new RegularRule(E, f));
//		
//		X.addRule(new NonLinearRule(new RegularRule(C), new RegularRule(W)));
//		assertSubstitution(Sets.<Rule>newHashSet(), X);
//	}
//	
//	@Test
//	public void nestedNonLinear11() {
////		V → V¬<f> | V<f> | V<f̅> | ZB | Z<g>		=SP
//		V.addRule(new RegularRule(V, not_f));
//		V.addRule(new RegularRule(V, f));
//		V.addRule(new RegularRule(V, f̅));
//		V.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(B)));
////		Z → 0
//		Z.addRule(new ConstantRule());
////		B → AD<f> | AD<f̅> | A¬<f> | A<f> | A | AD<f̅>¬<f> | AD<f̅><f> | AD
//		B.addRule(new NonLinearRule(new RegularRule(A), new RegularRule(D)));
////		A → <g> | <<g>W> | <g><f>
//		A.addRule(new ContextFreeRule(new Terminal[] {g}, E, new Terminal[0]));
////		D → ε | <f> | ¬<f> | D<f> | D<f̅> | D<f̅>¬<f> | D<f̅><f>
//		D.addRule(new ConstantRule());
//		D.addRule(new RegularRule(D, f));
//		D.addRule(new RegularRule(D, f̅));
////		E → ε | E¬<f> | E<f>
//		E.addRule(new ConstantRule());
//		E.addRule(new RegularRule(E, f));
//		
//		X.addRule(new NonLinearRule(new RegularRule(V), new RegularRule(E)));
//		assertSubstitution(Sets.<Rule>newHashSet(), X);
//	}
	
	private void assertSubstitution(Rule expectation, NonTerminal actual) {
		assertSubstitution(Sets.newHashSet(expectation), actual);
	}
	
	private void assertSubstitution(Set<Rule> expectation, NonTerminal actual) {
		ContextListener listener = mock(ContextListener.class);
		NonTerminalContext context = solver.resolve(actual);
		context.addListener(listener);
		
		ArgumentCaptor<Edge> captor = ArgumentCaptor.forClass(Edge.class);
		verify(listener, VerificationModeFactory.atLeast(0)).newIncomingEdge(captor.capture());
		Iterable<Rule> transform = Iterables.transform(Iterables.filter(captor.getAllValues(), new Predicate<Edge>() {
			@Override
			public boolean apply(Edge input) {
				return !input.getCondition().isPresent();
			}
		}), new Function<Edge, Rule>() {
			@Override
			public Rule apply(Edge input) {
				return input.getRule();
			}
		});
		assertEquals(expectation, Sets.newHashSet(transform));
	}	

	private void assertConditionalSubstitution(Optional<Rule> condition, Rule expectation, NonTerminal actual) {
		assertConditionalSubstitution(Sets.<Pair<Optional<Rule>, Rule>> newHashSet(new Pair<Optional<Rule>, Rule>(condition, expectation)), actual);
	}
	
	private void assertConditionalSubstitution(Set<Pair<Optional<Rule>, Rule>> expectation, NonTerminal actual) {
		ContextListener listener = mock(ContextListener.class);
		NonTerminalContext context = solver.resolve(actual);
		context.addListener(listener);
		
		ArgumentCaptor<Edge> captor = ArgumentCaptor.forClass(Edge.class);
		verify(listener, VerificationModeFactory.atLeast(0)).newIncomingEdge(captor.capture());
		Iterable<Pair<Optional<Rule>, Rule>> transform = Iterables.transform(captor.getAllValues(), new Function<Edge, Pair<Optional<Rule>, Rule>>() {
			@Override
			public Pair<Optional<Rule>, Rule> apply(Edge input) {
				return new Pair<Optional<Rule>, Rule>(input.getCondition(), input.getRule());
			}
		});
		assertEquals(expectation, Sets.newHashSet(transform));
	}	
}
