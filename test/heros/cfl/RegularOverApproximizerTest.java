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

	NonTerminal U = new NonTerminal("U");
	NonTerminal V = new NonTerminal("V");
	NonTerminal W = new NonTerminal("W");
	NonTerminal X = new NonTerminal("X");
	NonTerminal Y = new NonTerminal("Y");
	NonTerminal Z = new NonTerminal("Z");
	
	NonTerminal Uprime = approximizer.createNonTerminalPrime(U);
	NonTerminal Vprime = approximizer.createNonTerminalPrime(V);
	NonTerminal Wprime = approximizer.createNonTerminalPrime(W);
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
	
	
	@Test
	public void delayedConstantRuleInApproximatedScc() {
		X.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {g}));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(X, new ConstantRule(h));
		
		assertRules(X, new RegularRule(X, g), new RegularRule(Xprime, h));
		assertRules(Xprime, ε, new RegularRule(Xprime, f));
	}
	
	@Test
	public void delayedConstantRuleNotInApproximatedScc() {
		X.addRule(new RegularRule(X, g));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(X, new ConstantRule(h));
		
		assertRules(X, new RegularRule(X, g), new ConstantRule(h));
	}

	@Test
	public void delayedRegularRuleInApproximatedScc() {
		X.addRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g}));
		Y.addRule(new RegularRule(X));
		Y.addRule(new ConstantRule(g));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Y, new RegularRule(X, i));
		
		assertRules(X, new RegularRule(Y, g));
		assertRules(Y, new RegularRule(X), new RegularRule(Yprime, g), new RegularRule(X, i));
		assertRules(Xprime, ε, new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Xprime, f));
	}
	
	@Test
	public void delayedRegularRuleNotInApproximatedScc() {
		X.addRule(new RegularRule(Y, f));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Y, new RegularRule(X, g));
		assertRules(X, new RegularRule(Y, f));
		assertRules(Y, new RegularRule(X, g));
	}
	
	@Test
	public void delayedRegularRuleCreatingScc() {
		X.addRule(new ContextFreeRule(new Terminal[] {g}, Y, new Terminal[] {f}));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Y, new RegularRule(X, h));
		assertRules(X, new RegularRule(Y, f));
		assertRules(Y, new RegularRule(X, h));
		assertRules(Xprime, ε, new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Xprime, g));
	}
	
	@Test
	public void delayedRegularRuleConnectingTwoApproximatedSccs() {
		W.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {g}));
		W.addRule(new ContextFreeRule(new Terminal[] {i}, Z, new Terminal[] {k}));
		X.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {i}));
		X.addRule(new ContextFreeRule(new Terminal[] {j}, Y, new Terminal[] {k}));
		Y.addRule(new ContextFreeRule(new Terminal[] {g}, Z, new Terminal[] {f}));
		Z.addRule(new ContextFreeRule(new Terminal[] {h}, Y, new Terminal[] {k}));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Y, new RegularRule(X, g));
		
		assertRules(W, new RegularRule(X, g), new RegularRule(Z, k));
		assertRules(X, new RegularRule(W, i), new RegularRule(Y, k));
		assertRules(Y, new RegularRule(X, g), new RegularRule(Z, f));
		assertRules(Z, new RegularRule(Y, k));
		assertRules(Wprime, ε, new RegularRule(Xprime, h));
		assertRules(Xprime, ε, new RegularRule(Wprime, f), new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Xprime, j), new RegularRule(Zprime, h));
		assertRules(Zprime, ε, new RegularRule(Yprime, g), new RegularRule(Wprime, i));
	}
	
	@Test
	public void delayedRegularRuleOutOfApproximatedScc() {
		X.addRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g}));
		Y.addRule(new RegularRule(X));
		Y.addRule(new ConstantRule(g));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Y, new RegularRule(Z));
		
		assertRules(X, new RegularRule(Y, g));
		assertRules(Y, new RegularRule(X), new RegularRule(Yprime, g), new NonLinearRule(new RegularRule(Yprime), new RegularRule(Z)));
		assertRules(Xprime, ε, new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Xprime, f));
	}
	
	@Test
	public void delayedRegularRuleExtendingApproximatedSccOutwards() {
		W.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {g}));
		X.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {i}));
		Y.addRule(new ContextFreeRule(new Terminal[] {j}, Z, new Terminal[] {k}));
		Z.addRule(new ContextFreeRule(new Terminal[] {i}, W, new Terminal[] {g}));
		approximizer.approximate(new RegularRule(W));
		approximizer.addRule(X, new RegularRule(Y, f));
		
		assertRules(W, new RegularRule(X, g));
		assertRules(X, new RegularRule(W, i), new RegularRule(Y, f));
		assertRules(Y, new RegularRule(Z, k));
		assertRules(Z, new RegularRule(W, g));		
		assertRules(Xprime, ε, new RegularRule(Wprime, f));
		assertRules(Yprime, ε, new RegularRule(Xprime));
		assertRules(Wprime, ε, new RegularRule(Xprime, h), new RegularRule(Zprime, i));
		assertRules(Zprime, ε, new RegularRule(Yprime, j));
	}
	
	@Test
	public void delayedRegularRuleExtendingApproximatedSccOutwards2() {
		W.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {g}));
		X.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {i}));
		Y.addRule(new RegularRule(X, f));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(X, new RegularRule(Y, k));
		
		assertRules(W, new RegularRule(X, g));
		assertRules(X, new RegularRule(W, i), new RegularRule(Y, k));
		assertRules(Y, new RegularRule(X, f));
		assertRules(Wprime, ε, new RegularRule(Xprime, h));
		assertRules(Xprime, ε, new RegularRule(Wprime, f), new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Xprime));
	}
	
	@Test
	public void delayedRegularRuleExtendingApproximatedSccInwards() {
		W.addRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g}));
		Y.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {i}));
		W.addRule(new ContextFreeRule(new Terminal[] {j}, Z, new Terminal[] {k}));
		Z.addRule(new ContextFreeRule(new Terminal[] {i}, X, new Terminal[] {g}));
		approximizer.approximate(new RegularRule(W));
		approximizer.addRule(X, new RegularRule(Y, f));
		
		assertRules(W, new RegularRule(Y, g), new RegularRule(Z, k));
		assertRules(X, new RegularRule(Y, f));
		assertRules(Y, new RegularRule(W, i));
		assertRules(Z, new RegularRule(X, g));		
		assertRules(Xprime, ε, new RegularRule(Zprime, i));
		assertRules(Yprime, ε, new RegularRule(Xprime), new RegularRule(Wprime, f));
		assertRules(Wprime, ε, new RegularRule(Yprime, h));
		assertRules(Zprime, ε, new RegularRule(Wprime, j));
	}
	
	@Test
	public void delayedContextFreeRuleConnectingTwoApproximatedSccs() {
		W.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {g}));
		X.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {i}));
		X.addRule(new ContextFreeRule(new Terminal[] {j}, Y, new Terminal[] {k}));
		Y.addRule(new ContextFreeRule(new Terminal[] {g}, Z, new Terminal[] {f}));
		Z.addRule(new ContextFreeRule(new Terminal[] {h}, Y, new Terminal[] {k}));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Y, new ContextFreeRule(new Terminal[] {j}, X, new Terminal[] {g}));
		
		assertRules(W, new RegularRule(X, g));
		assertRules(X, new RegularRule(W, i), new RegularRule(Y, k));
		assertRules(Y, new RegularRule(X, g), new RegularRule(Z, f));
		assertRules(Z, new RegularRule(Y, k));
	}
	
	@Test
	public void delayedContextFreeRuleInApproximatedScc() {
		X.addRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g}));
		Y.addRule(new RegularRule(X));
		Y.addRule(new ConstantRule(g));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Y, new ContextFreeRule(new Terminal[] {h}, X, new Terminal[] {i}));
		
		assertRules(X, new RegularRule(Y, g));
		assertRules(Y, new RegularRule(X), new RegularRule(Yprime, g), new RegularRule(X, i));
		assertRules(Xprime, ε, new RegularRule(Yprime), new RegularRule(Yprime, h));
		assertRules(Yprime, ε, new RegularRule(Xprime, f));
	}
	
	@Test
	public void delayedRuleIntoApproximatedScc() {
		X.addRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g}));
		Y.addRule(new RegularRule(X));
		Y.addRule(new ConstantRule(g));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Z, new RegularRule(X));
		
		assertRules(X, new RegularRule(Y, g));
		assertRules(Y, new RegularRule(X), new RegularRule(Yprime, g));
		assertRules(Xprime, ε, new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Xprime, f));
		assertRules(Z, new RegularRule(X));
	}
	
	@Test
	public void delayedRuleNotLeftLinearInScc() {
		X.addRule(new RegularRule(Y, f));
		Y.addRule(new RegularRule(X, h));
		Y.addRule(new ConstantRule(g));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(X, new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g}));
		
		assertRules(X, new RegularRule(Y, f), new RegularRule(Y, g));
		assertRules(Y, new RegularRule(X, h), new RegularRule(Yprime, g));
		assertRules(Xprime, ε, new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Xprime, f), new RegularRule(Xprime));
	}
	
	@Test
	public void delayedRuleExtendingApproximatedSccInwards() {
		X.addRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g}));
		Y.addRule(new RegularRule(X));
		Y.addRule(new ConstantRule(g));
		Y.addRule(new ContextFreeRule(new Terminal[] {k}, Z, new Terminal[] {h}));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Z, new ContextFreeRule(new Terminal[]{j}, X, new Terminal[] {i}));
		
		assertRules(X, new RegularRule(Y, g));
		assertRules(Xprime, ε, new RegularRule(Yprime), new RegularRule(Zprime, j));
		assertRules(Y, new RegularRule(X), new RegularRule(Yprime, g), new RegularRule(Z, h));
		assertRules(Yprime, ε, new RegularRule(Xprime, f));
		assertRules(Z, new RegularRule(X, i));
		assertRules(Zprime, ε, new RegularRule(Yprime, k));
	}
	
	@Test
	public void delayedContextFreeRuleExtendingApproximatedSccOutwards() {
		W.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {g}));
		X.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {i}));
		Y.addRule(new ContextFreeRule(new Terminal[] {j}, Z, new Terminal[] {k}));
		Z.addRule(new ContextFreeRule(new Terminal[] {i}, W, new Terminal[] {g}));
		approximizer.approximate(new RegularRule(W));
		approximizer.addRule(X, new ContextFreeRule(new Terminal[] {j}, Y, new Terminal[] {f}));
		
		assertRules(W, new RegularRule(X, g));
		assertRules(X, new RegularRule(W, i), new RegularRule(Y, f));
		assertRules(Y, new RegularRule(Z, k));
		assertRules(Z, new RegularRule(W, g));		
		assertRules(Xprime, ε, new RegularRule(Wprime, f));
		assertRules(Yprime, ε, new RegularRule(Xprime, j));
		assertRules(Wprime, ε, new RegularRule(Xprime, h), new RegularRule(Zprime, i));
		assertRules(Zprime, ε, new RegularRule(Yprime, j));
	}
	
	@Test
	public void delayedContextFreeRuleNotInApproximatedScc() {
		X.addRule(new RegularRule(Y, f));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Y, new ContextFreeRule(new Terminal[] {f}, Z, new Terminal[] {g}));
		assertRules(X, new RegularRule(Y, f));
		assertRules(Y, new ContextFreeRule(new Terminal[] {f}, Z, new Terminal[] {g}));
	}
	
	@Test
	public void delayedContextFreeRuleOutOfApproximatedScc() {
		X.addRule(new RegularRule(Y, f));
		Y.addRule(new ContextFreeRule(new Terminal[] {i}, X, new Terminal[] {g}));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Y, new ContextFreeRule(new Terminal[] {f}, Z, new Terminal[] {g}));
		assertRules(X, new RegularRule(Y, f));
		assertRules(Y, new RegularRule(X, g), new NonLinearRule(new RegularRule(Yprime, f), new RegularRule(Z, g)));
		assertRules(Xprime, ε, new RegularRule(Yprime, i));
		assertRules(Yprime, ε, new RegularRule(Xprime));
	}
	
	@Test
	public void delayedNonLinearRuleNoScc() {
		X.addRule(new RegularRule(Y));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Y, new NonLinearRule(new RegularRule(W, f), new RegularRule(Z, g)));
		assertRules(X, new RegularRule(Y));
		assertRules(Y, new NonLinearRule(new RegularRule(W, f), new RegularRule(Z, g)));
	}
	
	@Test
	public void delayedNonLinearRuleInScc() {
		X.addRule(new RegularRule(W, f));
		W.addRule(new ContextFreeRule(new Terminal[] {g}, X, new Terminal[] {i}));
		W.addRule(new ContextFreeRule(new Terminal[] {h}, Y, new Terminal[] {j}));
		Y.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {j}));
		Y.addRule(new ContextFreeRule(new Terminal[] {h}, Z, new Terminal[] {j}));
		Z.addRule(new RegularRule(Y, k));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(X, new NonLinearRule(new RegularRule(Y, f), new RegularRule(Z, g)));
		
		assertRules(W, new RegularRule(X, i), new RegularRule(Y, j));
		assertRules(X, new RegularRule(W, f), new RegularRule(Z, g));
		assertRules(Y, new RegularRule(W, j), new RegularRule(Z, j));
		assertRules(Z, new RegularRule(Y, k));
		assertRules(Wprime, ε, new RegularRule(Xprime), new RegularRule(Yprime, h));
		assertRules(Xprime, ε, new RegularRule(Wprime, g));
		assertRules(Yprime, ε, new RegularRule(Wprime, h), new RegularRule(Zprime), new RegularRule(Xprime));
		assertRules(Zprime, ε, new RegularRule(Yprime, h), new RegularRule(Y, f));
	}
	
	@Test
	public void delayedNonLinearRulePartiallyInScc() {
		X.addRule(new RegularRule(W, f));
		W.addRule(new ContextFreeRule(new Terminal[] {g}, X, new Terminal[] {i}));
		W.addRule(new ContextFreeRule(new Terminal[] {h}, Y, new Terminal[] {j}));
		Y.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {j}));
		Y.addRule(new ContextFreeRule(new Terminal[] {h}, Z, new Terminal[] {j}));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(X, new NonLinearRule(new RegularRule(Y, f), new RegularRule(Z, g)));
		
		assertRules(W, new RegularRule(X, i), new RegularRule(Y, j));
		assertRules(X, new RegularRule(W, f), new NonLinearRule(new RegularRule(Y, f), new RegularRule(Z, g)));
		assertRules(Y, new RegularRule(W, j), new NonLinearRule(new RegularRule(Yprime), new ContextFreeRule(new Terminal[] {h}, Z, new Terminal[] {j})));
		assertRules(Wprime, ε, new RegularRule(Xprime), new RegularRule(Yprime, h));
		assertRules(Xprime, ε, new RegularRule(Wprime, g));
		assertRules(Yprime, ε, new RegularRule(Wprime, h), new RegularRule(Xprime));
	}
	
	@Test
	public void delayedNonLinearRuleExtendingOutwards() {
		W.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {g}));
		X.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {i}));
		Y.addRule(new RegularRule(Z, j));
		Z.addRule(new RegularRule(W, k));
		approximizer.approximate(new RegularRule(W));
		approximizer.addRule(X, new NonLinearRule(new RegularRule(Y, f), new RegularRule(Z, g)));
		
		assertRules(W, new RegularRule(X, g));
		assertRules(X, new RegularRule(W, i), new RegularRule(Z, g));
		assertRules(Y, new RegularRule(Z, j));
		assertRules(Z, new RegularRule(W, k));
		assertRules(Wprime, ε, new RegularRule(Xprime, h), new RegularRule(Zprime));
		assertRules(Xprime, ε, new RegularRule(Wprime, f));
		assertRules(Yprime, ε, new RegularRule(Xprime));
		assertRules(Zprime, ε, new RegularRule(Yprime), new RegularRule(Y, f));
	}
	
	@Test
	public void delayedNonLinearRuleExtendingOutwardsPartially() {
		W.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {g}));
		X.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {i}));
		Y.addRule(new RegularRule(X, h));
		Y.addRule(new RegularRule(Z, j));
		approximizer.approximate(new RegularRule(W));
		approximizer.addRule(X, new NonLinearRule(new RegularRule(Y, f), new RegularRule(Z, g)));
		
		assertRules(W, new RegularRule(X, g));
		assertRules(X, new RegularRule(W, i), new NonLinearRule(new RegularRule(Y, f), new RegularRule(Z, g)));
		assertRules(Y, new RegularRule(X, h), new NonLinearRule(new RegularRule(Yprime), new RegularRule(Z, j)));
		assertRules(Wprime, ε, new RegularRule(Xprime, h));
		assertRules(Xprime, ε, new RegularRule(Wprime, f), new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Xprime));
	}
	
	@Test
	public void delayedNonLinearRuleExtendingInwards() {
		W.addRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g}));
		Y.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {i}));
		Y.addRule(new RegularRule(X, j));
		Z.addRule(new RegularRule(W, k));
		approximizer.approximate(new RegularRule(W));
		approximizer.addRule(X, new NonLinearRule(new RegularRule(Y, f), new RegularRule(Z, g)));
		
		assertRules(W, new RegularRule(Y, g));
		assertRules(X, new RegularRule(Z, g));
		assertRules(Y, new RegularRule(W, i), new RegularRule(X, j));
		assertRules(Z, new RegularRule(W, k));
		assertRules(Wprime, ε, new RegularRule(Yprime, h), new RegularRule(Zprime));
		assertRules(Xprime, ε, new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Wprime, f), new RegularRule(Xprime));
		assertRules(Zprime, ε, new RegularRule(Y, f));
	}
	
	@Test
	public void delayedNonLinearRuleExtendingInwardsPartially() {
		W.addRule(new ContextFreeRule(new Terminal[] {f}, Y, new Terminal[] {g}));
		Y.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {i}));
		Y.addRule(new RegularRule(X, j));
		approximizer.approximate(new RegularRule(W));
		approximizer.addRule(X, new NonLinearRule(new RegularRule(Y, f), new RegularRule(Z, g)));
	
		assertRules(W, new RegularRule(Y, g));
		assertRules(X, new NonLinearRule(new RegularRule(Y, f), new RegularRule(Z, g)));
		assertRules(Y, new RegularRule(W, i), new RegularRule(X, j));
		assertRules(Wprime, ε, new RegularRule(Yprime, h));
		assertRules(Xprime, ε, new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Wprime, f), new RegularRule(Xprime));
	}
	
	@Test
	public void delayedNonLinearRuleAllInDifferentSccsNotConnecting() {
		X.addRule(new ContextFreeRule(new Terminal[] {f}, U, new Terminal[] {g}));
		U.addRule(new ContextFreeRule(new Terminal[] {h}, X, new Terminal[] {i}));
		Y.addRule(new ContextFreeRule(new Terminal[] {f}, V, new Terminal[] {g}));
		V.addRule(new ContextFreeRule(new Terminal[] {h}, Y, new Terminal[] {i}));
		Z.addRule(new ContextFreeRule(new Terminal[] {f}, W, new Terminal[] {g}));
		W.addRule(new ContextFreeRule(new Terminal[] {h}, Z, new Terminal[] {i}));
		approximizer.approximate(new NonLinearRule(new NonLinearRule(new RegularRule(X), new RegularRule(Y)), new RegularRule(Z)));
		approximizer.addRule(X, new NonLinearRule(new RegularRule(Y, f), new RegularRule(Z, g)));
		
		assertRules(U, new RegularRule(X, i));
		assertRules(X, new RegularRule(U, g), new NonLinearRule(new RegularRule(Xprime), new NonLinearRule(new RegularRule(Y,f), new RegularRule(Z,g))));
		assertRules(Uprime, ε, new RegularRule(Xprime, f));
		assertRules(Xprime, ε, new RegularRule(Uprime, h));
		
		assertRules(V, new RegularRule(Y, i));
		assertRules(Y, new RegularRule(V, g));
		assertRules(Vprime, ε, new RegularRule(Yprime, f));
		assertRules(Yprime, ε, new RegularRule(Vprime, h));
		
		assertRules(W, new RegularRule(Z, i));
		assertRules(Z, new RegularRule(W, g));
		assertRules(Wprime, ε, new RegularRule(Zprime, f));
		assertRules(Zprime, ε, new RegularRule(Wprime, h));
	}
	
	@Test
	public void delayedNonLinearRuleAllInDifferentSccsConnecting() {
		X.addRule(new ContextFreeRule(new Terminal[] {f}, U, new Terminal[] {g}));
		U.addRule(new ContextFreeRule(new Terminal[] {h}, X, new Terminal[] {i}));
		Y.addRule(new ContextFreeRule(new Terminal[] {f}, V, new Terminal[] {g}));
		Y.addRule(new RegularRule(X, k));
		V.addRule(new ContextFreeRule(new Terminal[] {h}, Y, new Terminal[] {i}));
		Z.addRule(new ContextFreeRule(new Terminal[] {f}, W, new Terminal[] {g}));
		Z.addRule(new RegularRule(X, j));
		W.addRule(new ContextFreeRule(new Terminal[] {h}, Z, new Terminal[] {i}));
		approximizer.approximate(new NonLinearRule(new NonLinearRule(new RegularRule(X), new RegularRule(Y)), new RegularRule(Z)));
		approximizer.addRule(X, new NonLinearRule(new RegularRule(Y, f), new RegularRule(Z, g)));
		
		assertRules(U, new RegularRule(X, i));
		assertRules(X, new RegularRule(U, g), new RegularRule(Z, g));
		assertRules(Uprime, ε, new RegularRule(Xprime, f));
		assertRules(Xprime, ε, new RegularRule(Uprime, h), new RegularRule(Yprime), new RegularRule(Zprime));
		
		assertRules(V, new RegularRule(Y, i));
		assertRules(Y, new RegularRule(V, g), new RegularRule(X, k));
		assertRules(Vprime, ε, new RegularRule(Yprime, f));
		assertRules(Yprime, ε, new RegularRule(Vprime, h), new RegularRule(Xprime));
		
		assertRules(W, new RegularRule(Z, i));
		assertRules(Z, new RegularRule(W, g), new RegularRule(X, j));
		assertRules(Wprime, ε, new RegularRule(Zprime, f));
		assertRules(Zprime, ε, new RegularRule(Wprime, h), new RegularRule(Y, f));
	}
	
	private void assertRules(NonTerminal nt, Rule... rules) {
		assertEquals("Expected rules "+Arrays.toString(rules)+", but got "+nt.getRules()+" for "+nt+".", rules.length, nt.getRules().size());
		for(Rule r : rules) 
			assertTrue(nt +" is missing rule '"+r+"', but has: "+nt.getRules(), nt.getRules().contains(r));
	}
}