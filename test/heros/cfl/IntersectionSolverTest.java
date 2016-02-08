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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import heros.cfl.IntersectionSolver.Guard;
import heros.cfl.IntersectionSolver.SubstitutionListener;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.verification.VerificationMode;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class IntersectionSolverTest {

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
		assertSubstitution(new RegularRule(X, f, g), new RegularRule(X, g));
	}

	@Test
	public void substitutionRequiresLoop() {
		X.addRule(new RegularRule(Y, f̅));
		Y.addRule(new RegularRule(Y, g̅));
		Y.addRule(new ConstantRule(f, g, g));
		assertSubstitution(new ConstantRule(), new RegularRule(X));
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
		Z.addRule(new ConstantRule(f, g, g, g));
		assertSubstitution(Lists.<Rule> newArrayList(new ConstantRule()), new RegularRule(X));
	}
	
	@Test
	public void substitutionNonLinear() {
		X.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(Y, f̅)));
		Y.addRule(new RegularRule(Y, g̅));
		Y.addRule(new ConstantRule(g̅));
		Z.addRule(new RegularRule(Z, g));
		Z.addRule(new ConstantRule(f, g, g, g));
		assertSubstitution(Lists.<Rule> newArrayList(new ConstantRule()), new RegularRule(X));
	}
	
	private void assertSubstitution(Rule expectation, Rule actual) {
		assertSubstitution(Lists.newArrayList(expectation), actual);
	}
	
	private void assertSubstitution(List<Rule> expectation, Rule actual) {
		SubstitutionListener listener = mock(SubstitutionListener.class);
		new IntersectionSolver().substitute(actual, listener);
		ArgumentCaptor<Rule> captor = ArgumentCaptor.forClass(Rule.class);
		verify(listener, VerificationModeFactory.atLeastOnce()).newProducingSubstitution(captor.capture(), Mockito.any(Guard.class));
		assertEquals(expectation,captor.getAllValues());		
	}	
}
