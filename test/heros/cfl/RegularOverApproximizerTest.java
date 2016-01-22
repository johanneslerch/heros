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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class RegularOverApproximizerTest {

	RegularOverApproximizer approximizer = new RegularOverApproximizer();
	
	ProducingTerminal f = new ProducingTerminal("f");
	ProducingTerminal g = new ProducingTerminal("g");
	ProducingTerminal h = new ProducingTerminal("h");
	ProducingTerminal i = new ProducingTerminal("i");
	ProducingTerminal j = new ProducingTerminal("j");
	ProducingTerminal k = new ProducingTerminal("k");

	NonTerminal X = new NonTerminal("X");
	NonTerminal Y = new NonTerminal("Y");
	NonTerminal Z = new NonTerminal("Z");
	
	NonTerminal Xprime = approximizer.createNonTerminalPrime(X);
	NonTerminal Yprime = approximizer.createNonTerminalPrime(Y);
	NonTerminal Zprime = approximizer.createNonTerminalPrime(Z);
	
	Rule ε = new ConstantRule();
	
	@Test
	public void palindrome() {
		X.addRule(new ContextFreeRule(new Terminal[]{f}, X, new Terminal[]{f}));
		X.addRule(new ContextFreeRule(new Terminal[]{g}, X, new Terminal[]{g}));
		X.addRule(ε);
		approximizer.approximate(new RegularRule(X));
		assertRules(X, new RegularRule(X, f), new RegularRule(X, g), new RegularRule(Xprime));
		assertRules(Xprime, ε, new RegularRule(Xprime, f), new RegularRule(Xprime, g));
	}
	
	@Test
	public void singleContextFreeRule() {
		X.addRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g}));
		Y.addRule(new RegularRule(X, h));
		Y.addRule(new ConstantRule(i));
		approximizer.approximate(new RegularRule(X));
		assertRules(X, new RegularRule(Y, g));
		assertRules(Y, new RegularRule(X, h), new RegularRule(Yprime, i));
		assertRules(Yprime, ε, new RegularRule(Xprime, f));
		assertRules(Xprime, ε, new RegularRule(Yprime));
	}
	
	@Test
	public void singleContextFreeRuleWithNonTerminalNotInScc() {
		X.addRule(new ContextFreeRule(new Terminal[]{f}, X, new Terminal[] {g}));
		X.addRule(new RegularRule(Y));
		Y.addRule(new RegularRule(Y, h));
		Y.addRule(new ConstantRule(i));
		approximizer.approximate(new RegularRule(X));
		assertRules(X, new RegularRule(X, g), new NonLinearRule(new RegularRule(Xprime), new RegularRule(Y)));
		assertRules(Xprime, ε, new RegularRule(Xprime, f));
		assertRules(Y, new RegularRule(Y, h), new ConstantRule(i));
	}
	
	@Test
	public void hardPcpInstance() {
		//X: fgggXg̅ | gfXg̅ | fXf̅g̅f̅ | ggXg̅g̅f̅ | fgg | g̅f̅ | f̅
		X.addRule(new ContextFreeRule(new Terminal[] {f, g, g, g}, X, new Terminal[] {i}));
		X.addRule(new ContextFreeRule(new Terminal[] {g, f}, X, new Terminal[] {i}));
		X.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {h, i, h}));
		X.addRule(new ConstantRule(f, g, g));
		X.addRule(new ConstantRule(i, h));
		X.addRule(new ConstantRule(h));
		approximizer.approximate(new RegularRule(X));
		assertRules(X, new RegularRule(X, i), new RegularRule(X, h, i, h), new RegularRule(Xprime, f,g,g), new RegularRule(Xprime, i,h), new RegularRule(Xprime, h));
		assertRules(Xprime, ε, new RegularRule(Xprime, f,g,g,g), new RegularRule(Xprime, g,f), new RegularRule(Xprime, f));
	}
	
	@Test
	public void nonLinear() {
		X.addRule(new NonLinearRule(new RegularRule(Y), new RegularRule(X)));
		X.addRule(new ContextFreeRule(new Terminal[] {h}, X, new Terminal[] {i}));
		X.addRule(new ConstantRule(g));
		Y.addRule(new RegularRule(X, i));
		Y.addRule(new ConstantRule(f));
		approximizer.approximate(new RegularRule(X));
		assertRules(X, new RegularRule(X, i), new RegularRule(Xprime, g));
		assertRules(Y, new RegularRule(X, i), new RegularRule(Yprime, f));
		assertRules(Xprime, ε, new RegularRule(Y), new RegularRule(Xprime, h), new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Xprime));
	}
	
	@Test
	public void multipleContextFreeRules() {
		Y.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {g}));
		Y.addRule(ε);
		X.addRule(new ContextFreeRule(new Terminal[] {j}, Z, new Terminal[] {k}));
		X.addRule(new ContextFreeRule(new Terminal[] {h}, Y, new Terminal[] {i}));
		Z.addRule(new RegularRule(X));
		approximizer.approximate(new RegularRule(Y));
		assertRules(Y, new RegularRule(X, g), new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Xprime, h));
		assertRules(X, new RegularRule(Z, k), new RegularRule(Y, i));
		assertRules(Xprime, ε, new RegularRule(Yprime, f), new RegularRule(Zprime));
		assertRules(Z, new RegularRule(X));
		assertRules(Zprime, ε, new RegularRule(Xprime, j));
	}
	
	@Test
	public void nonLinearContextFree() {
		X.addRule(new NonLinearRule(new RegularRule(Y), new RegularRule(X)));
		X.addRule(new ContextFreeRule(new Terminal[] {h}, X, new Terminal[] {i}));
		X.addRule(new ConstantRule(g));
		Y.addRule(new RegularRule(X, i));
		Y.addRule(new ConstantRule(f));
		approximizer.approximate(new RegularRule(Y));
		assertRules(X, new RegularRule(X, i), new RegularRule(Xprime, g));
		assertRules(Xprime, ε, new RegularRule(Xprime, h), new RegularRule(Y), new RegularRule(Yprime));
		assertRules(Y, new RegularRule(X, i), new RegularRule(Yprime, f));
		assertRules(Yprime, ε, new RegularRule(Xprime));
	}
	
	@Test
	public void nonApproximatedContextFreeRule() {
		X.addRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g}));
		X.addRule(new ContextFreeRule(new Terminal[] {k}, X, new Terminal[] {h}));
		Y.addRule(new RegularRule(Y, i));
		Y.addRule(new ConstantRule(j));
		approximizer.approximate(new RegularRule(X));
		assertRules(X, new NonLinearRule(new RegularRule(Xprime), new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g})),
				new RegularRule(X, h));
		assertRules(Xprime, ε, new RegularRule(Xprime, k));
		assertRules(Y, new RegularRule(Y, i), new ConstantRule(j));
	}
	
	private void assertRules(NonTerminal nt, Rule... rules) {
		assertEquals("Expected rules "+Arrays.toString(rules)+", but got "+nt.getRules()+" for "+nt+".", rules.length, nt.getRules().size());
		for(Rule r : rules) 
			assertTrue(nt +" is missing rule '"+r+"', but has: "+nt.getRules(), nt.getRules().contains(r));
	}
}
