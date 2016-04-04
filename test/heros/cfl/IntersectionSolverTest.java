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
import heros.cfl.IntersectionSolver.Guard;
import heros.cfl.IntersectionSolver.SubstitutionListener;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class IntersectionSolverTest {

	IntersectionSolver intersectionSolver = new IntersectionSolver();
	
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
	
	NonTerminal X = new NonTerminal("X");
	NonTerminal Y = new NonTerminal("Y");
	NonTerminal Z = new NonTerminal("Z");
	
	@Test
	public void substitutionTrivial() {
		X.addRule(new ConstantRule(f));
		assertSubstitution(new ConstantRule(f), new RegularRule(X));
	}
	
	@Test
	public void substitution() {
		X.addRule(new RegularRule(Y, g̅));
		Y.addRule(new RegularRule(Z, g));
		Z.addRule(new RegularRule(X, f));
		assertSubstitution(new RegularRule(X, f), new RegularRule(X));
	}
	
	@Test
	public void terminateProducingLoop() {
		X.addRule(new RegularRule(X, f));
		assertSubstitution(new RegularRule(X, f), new RegularRule(X));
	}

	@Test
	public void substitutionRequiresLoop() {
		X.addRule(new RegularRule(Y, f̅));
		Y.addRule(new RegularRule(Y, g̅));
		Y.addRule(new ConstantRule(h, f, g, g));
		assertSubstitution(new ConstantRule(h), new RegularRule(X));
	}

	@Test
	public void substitutionContextFree() {
		X.addRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g̅}));
		Y.addRule(new ContextFreeRule(new Terminal[] {h}, Z, new Terminal[] {g}));
		Z.addRule(new RegularRule(X, f));
		assertSubstitution(new ContextFreeRule(new Terminal[] {f, h}, X, new Terminal[] {f}), new RegularRule(X));
	}
	
	@Test
	public void substitutionLinearized() {
		X.addRule(new RegularRule(Y, f̅));
		Y.addRule(new RegularRule(Y, g̅));
		Y.addRule(new RegularRule(Z, g̅));
		Z.addRule(new RegularRule(Z, g));
		Z.addRule(new ConstantRule(h, f, g, g, g));
		assertSubstitution(new ConstantRule(h), new RegularRule(X));
	}
	
	@Test
	public void substitutionNonLinear() {
		X.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(Y, f̅)));
		Y.addRule(new RegularRule(Y, g̅));
		Y.addRule(new ConstantRule(g̅));
		Z.addRule(new RegularRule(Z, g));
		Z.addRule(new ConstantRule(h, f, g, g, g));
		assertSubstitution(new ConstantRule(h), new RegularRule(X));
	}
	
	@Test
	public void substitutionNonLinearRightProducing() {
		X.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(Y)));
		Y.addRule(new RegularRule(X, f));
		assertSubstitution(new NonLinearRule(new RegularRule(Z), new RegularRule(X, f)), new RegularRule(X));
	}
	
	@Test
	public void substitutionHiddenIdentityRule() {
		X.addRule(new RegularRule(X, g̅));
		X.addRule(new RegularRule(X, g̅, g));

		NonTerminal intermediateNonTerminal = intersectionSolver.getIntermediateNonTerminal(X, new RegularRule(X, g̅));		
		assertSubstitution(new RegularRule(intermediateNonTerminal, g), new RegularRule(X));
	}
	
	@Test
	public void substitutionHiddenIdentityRule2() {
		X.addRule(new RegularRule(Y, g̅));
		Y.addRule(new RegularRule(Z, g));
		Z.addRule(new RegularRule(Y, g̅));
		assertSubstitution(Sets.<Rule> newHashSet(), new RegularRule(X));
	}

	@Test
	public void substitutionEmptyInNonLinearRule() {
		X.addRule(new NonLinearRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[0]), new RegularRule(Y)));
		Y.addRule(new ConstantRule());
		assertSubstitution(new ConstantRule(f), new RegularRule(X));
	}
	
	@Test
	public void substitutionHiddenIdentityRule3() {
		X.addRule(new RegularRule(X, g̅));
		X.addRule(new RegularRule(X, g̅, g));
		X.addRule(new RegularRule(X, g̅, g̅, g, g));
		
		NonTerminal intermediateNonTerminal1 = intersectionSolver.getIntermediateNonTerminal(X, new RegularRule(X, g̅));
		NonTerminal intermediateNonTerminal2 = intersectionSolver.getIntermediateNonTerminal(X, new RegularRule(X, g̅, g̅));
		assertSubstitution(Sets.<Rule> newHashSet(
				new RegularRule(intermediateNonTerminal1, g), 
				new RegularRule(intermediateNonTerminal2, g, g),
				new RegularRule(intermediateNonTerminal2, g)), new RegularRule(X));
	}
	
	@Test
	public void substitutionSelfSatisfyingLoop() {
		X.addRule(new RegularRule(Y, f̅,f̅,f̅,f̅,f̅));
		Y.addRule(new ConstantRule(f));
		Y.addRule(new RegularRule(Y, f̅, f, f));
		
		NonTerminal intermediateNonTerminal = intersectionSolver.getIntermediateNonTerminal(Y, new RegularRule(Y, f̅));
		assertSubstitution(new RegularRule(intermediateNonTerminal, f), new RegularRule(X));
	}

	
	
	private void assertSubstitution(Rule expectation, Rule actual) {
		assertSubstitution(Sets.newHashSet(expectation), actual);
	}
	
	private void assertSubstitution(Set<Rule> expectation, Rule actual) {
		SubstitutionListener listener = mock(SubstitutionListener.class);
		intersectionSolver.substitute(actual, listener);
		if(expectation.isEmpty())
			verify(listener, never()).newProducingSubstitution(any(Rule.class), any(Guard.class));
		else {
			ArgumentCaptor<Rule> captor = ArgumentCaptor.forClass(Rule.class);
			verify(listener, VerificationModeFactory.atLeastOnce()).newProducingSubstitution(captor.capture(), any(Guard.class));
			assertEquals(expectation,Sets.newHashSet(captor.getAllValues()));
		}
	}	
}
