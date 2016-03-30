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
import heros.solver.Pair;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class RegularOverApproximizerTest {

	RegularOverApproximizer approximizer = new RegularOverApproximizer();
	
	ProducingTerminal f = new ProducingTerminal("f");
	ProducingTerminal g = new ProducingTerminal("g");
	ProducingTerminal h = new ProducingTerminal("h");
	ProducingTerminal i = new ProducingTerminal("i");
	ProducingTerminal j = new ProducingTerminal("j");
	ProducingTerminal k = new ProducingTerminal("k");

	NonTerminal T = new NonTerminal("T");
	NonTerminal U = new NonTerminal("U");
	NonTerminal V = new NonTerminal("V");
	NonTerminal W = new NonTerminal("W");
	NonTerminal X = new NonTerminal("X");
	NonTerminal Y = new NonTerminal("Y");
	NonTerminal Z = new NonTerminal("Z");
	
	NonTerminal Tprime = approximizer.prime.get(T);
	NonTerminal Uprime = approximizer.prime.get(U);
	NonTerminal Vprime = approximizer.prime.get(V);
	NonTerminal Wprime = approximizer.prime.get(W);
	NonTerminal Xprime = approximizer.prime.get(X);
	NonTerminal Yprime = approximizer.prime.get(Y);
	NonTerminal Zprime = approximizer.prime.get(Z);
	
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
		assertRules(X, new NonLinearRule(new RegularRule(Xprime, f), new RegularRule(Y, new Terminal[] {g})),
				new RegularRule(X, h));
		assertRules(Xprime, ε, new RegularRule(Xprime, k));
		assertRules(Y, new RegularRule(Y, i), new ConstantRule(j));
	}
	
	@Test
	public void reproducableNonLinearRule() {
		X.addRule(new ConstantRule(h, h, h));
		Y.addRule(new ConstantRule(g));
		Y.addRule(new ConstantRule(f));
		X.addRule(new NonLinearRule(new RegularRule(Y), new RegularRule(X)));
		approximizer.approximate(new RegularRule(X));
		assertRules(X, new RegularRule(Xprime, h, h, h));
		assertRules(Xprime, ε, new NonLinearRule(new RegularRule(Xprime), new RegularRule(Y)));
		assertRules(Y, new ConstantRule(g), new ConstantRule(f));
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
		assertRules(Y, new RegularRule(W, j), new NonLinearRule(new RegularRule(Yprime, h), new RegularRule(Z, new Terminal[] {j})));
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
		assertRules(X, new RegularRule(U, g), new NonLinearRule( new NonLinearRule(new RegularRule(Xprime), new RegularRule(Y,f)), new RegularRule(Z,g)));
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
	
	@Test
	public void delayedNonLeftLinearRuleAddedToScc() {
		X.addRule(new RegularRule(Y));
		X.addRule(new ConstantRule());
		Y.addRule(new RegularRule(X));
		Y.addRule(new ConstantRule());
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(X, new ContextFreeRule(new Terminal[] {f}, X, new Terminal[0]));
		
		assertRules(X, new RegularRule(Y), new RegularRule(Xprime));
		assertRules(Xprime, ε, new RegularRule(Yprime), new RegularRule(Xprime, f));
		assertRules(Y, new RegularRule(X), new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Xprime));
	}
	
	@Test
	public void delayedRegularClosingSccAffectingSourceSccWithoutNonLeftLinearRules() {
		X.addRule(new RegularRule(W));
		W.addRule(new RegularRule(X));
		
		Y.addRule(new RegularRule(Z));
		Z.addRule(new RegularRule(U));
		U.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[0]));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(X, new RegularRule(Y));
		
		assertRules(X, new RegularRule(Y), new RegularRule(W));
		assertRules(Y, new RegularRule(Z));
		assertRules(Z, new RegularRule(U));
		assertRules(U, new RegularRule(X));
		assertRules(W, new RegularRule(X));
		assertRules(Xprime, ε, new RegularRule(Wprime), new RegularRule(Uprime, f));
		assertRules(Yprime, ε, new RegularRule(Xprime));
		assertRules(Zprime, ε, new RegularRule(Yprime));
		assertRules(Uprime, ε, new RegularRule(Zprime));
		assertRules(Wprime, ε, new RegularRule(Xprime));
	}
	
	@Test
	public void delayedNonLinearInExistingApproximatedScc() {
		X.addRule(new RegularRule(Y));
		X.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(Y)));
		Y.addRule(new RegularRule(Z));
		V.addRule(new RegularRule(X));
		Z.addRule(new RegularRule(W));
		W.addRule(new RegularRule(Z));
		U.addRule(new RegularRule(T));
		T.addRule(new RegularRule(Z));
		approximizer.approximate(new RegularRule(X));
		approximizer.addRule(Z, new ContextFreeRule(new Terminal[] {f}, V, new Terminal[0]));
		approximizer.addRule(X, new NonLinearRule(new RegularRule(U), new RegularRule(Z)));
	}
	
	@Test
	public void nonLinearSccButOnlyLeftLinear() {
		W.addRule(new ConstantRule(k));
		W.addRule(new NonLinearRule(new RegularRule(W), new ContextFreeRule(new Terminal[] {f}, X, new Terminal[0])));
		
		assertRules(W, new ConstantRule(k), new NonLinearRule(new RegularRule(W), new ContextFreeRule(new Terminal[] {f}, X, new Terminal[0])));
	}
	
	@Test
	public void delayedNonLinearSccButOnlyLeftLinear() {
		approximizer.addRule(W, new ConstantRule(k));
		approximizer.addRule(W, new NonLinearRule(new RegularRule(W), new ContextFreeRule(new Terminal[] {f}, X, new Terminal[0])));
		
		assertRules(W, new ConstantRule(k), new NonLinearRule(new RegularRule(W), new ContextFreeRule(new Terminal[] {f}, X, new Terminal[0])));
	}
	
	@Test
	public void contextFreeRule() {
		U.addRule(new ContextFreeRule(new Terminal[] {k}, Z, new Terminal[0]));
		U.addRule(new ContextFreeRule(new Terminal[] {f}, U, new Terminal[0]));
		approximizer.approximate(new RegularRule(U));
		
		assertRules(U, new NonLinearRule(new RegularRule(Uprime, k), new RegularRule(Z)));
		assertRules(Uprime, ε, new RegularRule(Uprime, f));
	}

	@Test
	public void nonLinearRuleNesting() {
		approximizer.addRule(X, new ConstantRule(f));
		approximizer.addRule(X, new ContextFreeRule(new Terminal[] {g}, X, new Terminal[0]));
		approximizer.addRule(X, new NonLinearRule(new RegularRule(V, j), new RegularRule(U)));
		assertRules(X, new RegularRule(Xprime, f), 
				new NonLinearRule(new NonLinearRule(new RegularRule(Xprime), new RegularRule(V, j)), new RegularRule(U)));
	}
	
	@Test
	public void complexNonLinearRule() {
		// Y → <hVf> | <hWh>
		// V → (Wg,((<kYh>,<kUf>),(((<jXj>,(V,Y)),(Y,(<hY>,<gW>))),<iY>)))
		// W → j
		Y.addRule(new ContextFreeRule(new Terminal[] {h}, V, new Terminal[] {f}));
		Y.addRule(new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {h}));
		V.addRule(new NonLinearRule(
				new RegularRule(W, g),
				new NonLinearRule(
						new NonLinearRule(
								new ContextFreeRule(new Terminal[] {k}, Y, new Terminal[] {h}),
								new ContextFreeRule(new Terminal[] {k}, U, new Terminal[] {f})),
						new NonLinearRule(
								new NonLinearRule(
										new NonLinearRule(
												new ContextFreeRule(new Terminal[] {j}, X, new Terminal[] {j}),
												new NonLinearRule(new RegularRule(V), new RegularRule(Y))),
										new NonLinearRule(
												new RegularRule(Y),
												new NonLinearRule(
														new ContextFreeRule(new Terminal[] {h}, Y, new Terminal[] {}),
														new ContextFreeRule(new Terminal[] {g}, W, new Terminal[] {})))),
								new ContextFreeRule(new Terminal[] {i}, Y, new Terminal[] {})))));			
		W.addRule(new ConstantRule(j));
		approximizer.approximate(new RegularRule(Y));
		assertRules(Y, new RegularRule(V, f), new NonLinearRule(new RegularRule(Yprime, h), new RegularRule(W, h)));
		assertRules(V, new RegularRule(Y));
		assertRules(Yprime, ε, 
				// YgWi
				new NonLinearRule(new RegularRule(Y, g), new RegularRule(W, i)),
				// Yh
				new RegularRule(Y, h),
				new RegularRule(Y),
				new RegularRule(V),
				// V'Wgk
				new NonLinearRule(new RegularRule(Vprime), new RegularRule(W, g, k)));
		assertRules(Vprime, ε, new RegularRule(Yprime, h),
				// YhkUfjXj
				new NonLinearRule(new NonLinearRule(new RegularRule(Y, h, k), new RegularRule(U, f, j)), 
						new RegularRule(X, j)));
		assertRules(Wprime, ε);
	}
	
	@Test
	public void delayedComplexNonLinearRule() {
		// Y → <hVf> | <hWh>
		// V → (Wg,((<kYh>,<kUf>),(((<jXj>,(V,Y)),(Y,(<hY>,<gW>))),<iY>))) | ε
		// W → j
		approximizer.addRule(V, new ConstantRule());
		approximizer.addRule(Y, new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {h}));
		approximizer.addRule(V, new NonLinearRule(
				new RegularRule(W, g),
				new NonLinearRule(
						new NonLinearRule(
								new ContextFreeRule(new Terminal[] {k}, Y, new Terminal[] {h}),
								new ContextFreeRule(new Terminal[] {k}, U, new Terminal[] {f})),
						new NonLinearRule(
								new NonLinearRule(
										new NonLinearRule(
												new ContextFreeRule(new Terminal[] {j}, X, new Terminal[] {j}),
												new NonLinearRule(new RegularRule(V), new RegularRule(Y))),
										new NonLinearRule(
												new RegularRule(Y),
												new NonLinearRule(
														new ContextFreeRule(new Terminal[] {h}, Y, new Terminal[] {}),
														new ContextFreeRule(new Terminal[] {g}, W, new Terminal[] {})))),
								new ContextFreeRule(new Terminal[] {i}, Y, new Terminal[] {})))));			
		
		assertRules(Y, new ContextFreeRule(new Terminal[] {h}, W, new Terminal[] {h}));
		assertRules(V,  new RegularRule(Vprime), 
				// VYYhYgWiY
				new NonLinearRule(
					new NonLinearRule(
							new NonLinearRule(
									new NonLinearRule(new RegularRule(V), new RegularRule(Y)),
									new RegularRule(Y, h)),
							new RegularRule(Y)),
					new NonLinearRule(
							new ContextFreeRule(new Terminal[] {g}, W, new Terminal[] {i}),
							new RegularRule(Y)))
				);
		assertRules(Yprime, ε);
		assertRules(Vprime, ε,
				// V'WgkYhkUfjXj
				new NonLinearRule(
						new NonLinearRule(
								new NonLinearRule(new RegularRule(Vprime), new RegularRule(W, g, k)),
								new RegularRule(Y, h)),
						new NonLinearRule(
								new ContextFreeRule(new Terminal[] {k}, U, new Terminal[] {f, j}),
								new RegularRule(X, j))));
		assertRules(Wprime, ε);
		
		approximizer.addRule(Y, new ContextFreeRule(new Terminal[] {h}, V, new Terminal[] {f}));
		approximizer.addRule(W, new ConstantRule(j));
		
		assertRules(Y, new RegularRule(V, f), new NonLinearRule(new RegularRule(Yprime, h), new RegularRule(W, h)));
		assertRules(V,  new RegularRule(Vprime), new RegularRule(Y));
		assertRules(Vprime, ε, new RegularRule(Yprime, h),
				// YhkUfjXj
				new NonLinearRule(new NonLinearRule(new RegularRule(Y, h, k), new RegularRule(U, f, j)), 
						new RegularRule(X, j)));
		assertRules(Yprime, ε, 
				new NonLinearRule(new RegularRule(Y, g), new RegularRule(W, i)),
				new RegularRule(Y, h),
				new RegularRule(Y),
				new RegularRule(V),
				new NonLinearRule(new RegularRule(Vprime), new RegularRule(W, g, k)));
		assertRules(Wprime, ε);
	}
	
	@Test
	public void delayedComplexNonLinearRule2() {
		// V → Y
		// Y → ((Y,(Zh,(Z,((Y,Zh),<jZk>)))),((Vg,<gX>),Yj)) | ε
		// X → Zj
		approximizer.addRule(Y, new ConstantRule());
		approximizer.addRule(Y, new NonLinearRule(
				new NonLinearRule(
					new RegularRule(Y),
					new NonLinearRule(
						new RegularRule(Z, h),
						new NonLinearRule(
							new RegularRule(Z),
							new NonLinearRule(
								new NonLinearRule(new RegularRule(Y), new RegularRule(Z, h)),
								new ContextFreeRule(new Terminal[] {j}, Z, new Terminal[] {k}))))),
				new NonLinearRule(
					new NonLinearRule(new RegularRule(V, g), new ContextFreeRule(new Terminal[] {g}, X, new Terminal[0])),
					new RegularRule(Y, j))));
		
		
		approximizer.addRule(V, new RegularRule(Y));
		approximizer.addRule(X, new RegularRule(Z, j));
		
		assertRules(V, new RegularRule(Y));
		assertRules(Y, new RegularRule(Y, j), new RegularRule(Yprime));
		assertRules(Yprime, ε, new RegularRule(Vprime),	new NonLinearRule(new RegularRule(V, g, g), new RegularRule(X)),
				// YZhZ
				new NonLinearRule(new NonLinearRule(new RegularRule(Y), new RegularRule(Z, h)), new RegularRule(Z)));
		assertRules(Vprime, ε,
				// YZhjZk
				new NonLinearRule(new NonLinearRule(new RegularRule(Y), new RegularRule(Z, h, j)),
						new RegularRule(Z, k))
				);
	}
	
	@Test
	public void delayedNonLinearRuleThatIsLeftLinear() {
		// U → <jXf>
		// X → k | (U,Zf)
		U.addRule(new ContextFreeRule(new Terminal[] {j}, X, new Terminal[] {f}));
		X.addRule(new ConstantRule(k));
		approximizer.approximate(new RegularRule(U));
		approximizer.addRule(X, new NonLinearRule(new RegularRule(U), new RegularRule(Z, f)));
		
		assertRules(U, new RegularRule(X, f));
		assertRules(X, new RegularRule(Xprime, k), new NonLinearRule(new RegularRule(U), new RegularRule(Z, f)));
		assertRules(Uprime, ε, new RegularRule(Xprime));
		assertRules(Xprime, ε, new RegularRule(Uprime, j));
	}
	
	@Test
	public void delayedComplexNonLinearRule3() {
		// W → <kW> | (Uj,(<kVi>,X))
		// U → Uf
		// X → i | Ui
		approximizer.addRule(X, new ConstantRule(i));
		approximizer.addRule(X, new RegularRule(U, i));
		approximizer.addRule(U, new RegularRule(U, f));
		approximizer.addRule(W, new ContextFreeRule(new Terminal[] {k}, W, new Terminal[0]));
		approximizer.addRule(W, new NonLinearRule(new RegularRule(U, j),
				new NonLinearRule(new ContextFreeRule(new Terminal[] {k}, V, new Terminal[] {i}),
						new RegularRule(X))));
		
		assertRules(X, new ConstantRule(i), new RegularRule(U, i));
		assertRules(U, new RegularRule(U, f));
		assertRules(W, 
				new NonLinearRule(
					new RegularRule(U, j),
					new NonLinearRule(
							new ContextFreeRule(new Terminal[]{k}, V, new Terminal[]{i}),
							new RegularRule(X))));
		assertRules(Wprime, ε, new RegularRule(Wprime, k));
	}
	
	@Test
	public void permutationTest() {
		PermutationTest permutation = new PermutationTest(Lists.newArrayList(U, V, W, X, Y, Z), 
				Lists.newArrayList(Uprime, Vprime, Wprime, Xprime, Yprime, Zprime),
				Lists.<Terminal>newArrayList(f,g,h,i,j,k));
		for(int i=0; i<1000; i++) {
			permutation.clean();
			List<Pair<NonTerminal, Rule>> rules = permutation.setup(5);
			int permIndex = 0;
			for(List<Pair<NonTerminal, Rule>> perm : Collections2.permutations(rules)) {
				if(permIndex++ > 10)
					break;
				
				List<Pair<NonTerminal, Rule>> currentRuleSet = Lists.newLinkedList();
				approximizer = new RegularOverApproximizer(approximizer.prime);
				permutation.clean();
				for(Pair<NonTerminal, Rule> r : perm) {
					//test incremental:
					try {
						approximizer.addRule(r.getO1(), r.getO2());
						permutation.collectActual();
					}
					catch (IllegalStateException e) {
						throw new AssertionError("Exception while adding to \n"
								+permutation.toString(currentRuleSet)
								+"the rule: "+r.getO1() +" -> "+r.getO2(), e);
					}
					//clean and prepare
					currentRuleSet.add(r);
					permutation.clean();
					for(Pair<NonTerminal, Rule> p : currentRuleSet)
						p.getO1().addRule(p.getO2());
					
					//get expectation
					approximizer = new RegularOverApproximizer(approximizer.prime);
					approximizer.approximate(permutation.getNonTerminals());
					permutation.collectExpectation();

					permutation.compareAndAssert(currentRuleSet);
				}
			}
		}
	}
	
	private void assertRules(NonTerminal nt, Rule... rules) {
		assertEquals("Expected rules "+Arrays.toString(rules)+", but got "+nt.getRules()+" for "+nt+".", rules.length, nt.getRules().size());
		for(Rule r : rules) 
			assertTrue(nt +" is missing rule '"+r+"', but has: "+nt.getRules(), nt.getRules().contains(r));
	}
}
