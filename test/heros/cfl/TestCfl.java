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
package heros.cfl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoAssertionError;

import fj.data.Option;

public class TestCfl {

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
	RegularRule consumeFOnX = new RegularRule(X, f̅);
	
	
	@Test
	public void regularTrivialSolvable1() {
		// X: f | g
		// solvable X-f ?
		X.addRule(new ConstantRule(f));
		X.addRule(new ConstantRule(g));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularTrivialSolvable2() {
		// X: Y
		// Y: f | g
		// solvable X-f ?
		X.addRule(new RegularRule(Y));
		Y.addRule(new ConstantRule(f));
		Y.addRule(new ConstantRule(g));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularTrivialSolvable3() {
		// X: X | Y
		// Y: Z
		// Z: f
		// solvable X-f ?
		X.addRule(new RegularRule(X));
		X.addRule(new RegularRule(Y));
		Y.addRule(new RegularRule(Z));
		Z.addRule(new ConstantRule(f));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularProduceConsumedTerminals() {
		// X: X -g | Y g
		// Y: Y f | f
		// solvable X-f ?
		X.addRule(new RegularRule(X, g̅));
		X.addRule(new RegularRule(Y, g));
		Y.addRule(new RegularRule(Y, f));
		Y.addRule(new ConstantRule(f));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularUnsolvableLoop() {
		// X: X -g | Y h
		// Y: Y f
		// solvable X-f ?
		X.addRule(new RegularRule(X, g̅));
		X.addRule(new RegularRule(Y, h));
		Y.addRule(new RegularRule(Y, f));
		unsolvable(consumeFOnX);
	}

	@Test
	public void regularReadingLoopSolvable() {
		// X: Xg̅ | Yg
		// Y: f
		X.addRule(new RegularRule(X, g̅));
		X.addRule(new RegularRule(Y, g));
		Y.addRule(new ConstantRule(f));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularReadingTwoFieldsInLoopUnsolvable() {
		// X: Xg̅f̅ | Yg
		// Y: f
		X.addRule(new RegularRule(X, g̅, f̅));
		X.addRule(new RegularRule(Y, g));
		Y.addRule(new ConstantRule(f));
		unsolvable(consumeFOnX);
	}

	@Test
	public void regularReadingTwoFieldsInLoopOnceReducable() {
		// X: Xg̅f̅ | Yg | Zgg
		// Y: f
		// Z: ff
		X.addRule(new RegularRule(X, g̅, f̅));
		X.addRule(new RegularRule(Y, g));
		X.addRule(new RegularRule(Z, g, g));
		Y.addRule(new ConstantRule(f));
		Z.addRule(new ConstantRule(f, f));
		unsolvable(consumeFOnX);
	}
	
	@Test
	public void regularLoopUnsolvable() {
		//X: Xg̅f̅ | Zfgfg
		//Z: i
		X.addRule(new RegularRule(X, g̅, f̅));
		X.addRule(new RegularRule(Z, f, g, f, g));
		Z.addRule(new ConstantRule(i));
		unsolvable(new RegularRule(X, h̄));
	}
	
	@Test
	public void regularLoopUnsolvable2() {
		// X: Yg̅ | Zggg
		// Y: Xg̅ 
		// Z: f
		X.addRule(new RegularRule(Y, g̅));
		X.addRule(new RegularRule(Z, g, g, g));
		Y.addRule(new RegularRule(X, g̅));
		Z.addRule(new ConstantRule(f));
		unsolvable(consumeFOnX);
	}
	
	@Test
	public void regularWritingAndConsumingLoops() {
		// X: Xg̅ | Y
		// Y: Yg | f
		X.addRule(new RegularRule(X, g̅));
		X.addRule(new RegularRule(Y));
		Y.addRule(new RegularRule(Y, g));
		Y.addRule(new ConstantRule(f));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularWritingAndComsumingLoops2() {
		// X: Y
		// Y: Yg̅ | Zg̅
		// Z: Zg | fggg
		X.addRule(new RegularRule(Y));
		Y.addRule(new RegularRule(Z, g̅));
		Y.addRule(new RegularRule(Y, g̅));
		Z.addRule(new RegularRule(Z, g));
		Z.addRule(new ConstantRule(f, g, g, g));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularNonLinear1() {
		// X: Xg̅ | g̅
		// Y: Yg | f
		X.addRule(new RegularRule(X, g̅));
		X.addRule(new ConstantRule(g̅));
		Y.addRule(new RegularRule(Y, g));
		Y.addRule(new ConstantRule(f));
		solvable(new NonLinearRule(new RegularRule(Y), consumeFOnX));
	}
	
	@Test
	public void regularNonLinear2() {
		// X: Xf̅ | f̅
		// Y: Yff | g
		X.addRule(new RegularRule(X, f̅));
		X.addRule(new ConstantRule(f̅));
		Y.addRule(new RegularRule(Y, f, f));
		Y.addRule(new ConstantRule(g));
		solvable(new NonLinearRule(new RegularRule(Y), new RegularRule(X, g̅)));
	}
	
	@Test
	public void regularNonLinear3a() {
		// X: ZY | Y
		// Y: Yg̅ | g̅
		// Z: Zg | fg
		X.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(Y)));
		X.addRule(new RegularRule(Y));
		Y.addRule(new RegularRule(Y, g̅));
		Y.addRule(new ConstantRule(g̅));
		Z.addRule(new RegularRule(Z, g));
		Z.addRule(new ConstantRule(f, g));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularNonLinear3b() {
		// X: Y | ZY
		// Y: Yg̅ | g̅
		// Z: Zg | fg
		X.addRule(new RegularRule(Y));
		X.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(Y)));
		Y.addRule(new RegularRule(Y, g̅));
		Y.addRule(new ConstantRule(g̅));
		Z.addRule(new RegularRule(Z, g));
		Z.addRule(new ConstantRule(f, g));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularNonLinear4() {
		// X: ZY
		// Y: Yg̅ | g̅
		// Z: Zg | fggg
		X.addRule(new NonLinearRule(new RegularRule(Z), new RegularRule(Y)));
		Y.addRule(new RegularRule(Y, g̅));
		Y.addRule(new ConstantRule(g̅));
		Z.addRule(new RegularRule(Z, g));
		Z.addRule(new ConstantRule(f, g, g, g));
		solvable(consumeFOnX);
	}
	
	@Test
	public void contextFree1() {
		// X: fXg̅ | Y
		// Y: Yg | g
		X.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {g̅}));
		X.addRule(new RegularRule(Y));
		Y.addRule(new RegularRule(Y, g));
		Y.addRule(new ConstantRule(g));
		solvable(consumeFOnX);
	}
	
	@Test
	public void contextFree2() {
		// X: fXg̅g̅ | fXg | fg
		X.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {g̅, g̅}));
		X.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {g}));
		X.addRule(new ConstantRule(f, g));
		solvable(consumeFOnX);
	}
	
	@Test
	public void contextFreeSolvableInOverApproximation1() {
		// X: f̅Xg | f̅g
		// Y: f
		X.addRule(new ContextFreeRule(new Terminal[] {f̅}, X, new Terminal[] {g}));
		X.addRule(new ConstantRule(f̅, g));
		Y.addRule(new ConstantRule(f));
		solvable(new NonLinearRule(new RegularRule(Y), new RegularRule(X, g̅, g̅)));
	}
	
	@Test
	public void contextFreeSolvableInOverApproximation2() {
		// X: f̅Xg | f̅g
		// Y: ff
		X.addRule(new ContextFreeRule(new Terminal[] {f̅}, X, new Terminal[] {g}));
		X.addRule(new ConstantRule(f̅, g));
		Y.addRule(new ConstantRule(f, f));
		solvable(new NonLinearRule(new RegularRule(Y), new RegularRule(X, g̅, g̅, g̅)));
	}
	
	@Test
	public void regularMultipleConsumers() {
		// X: Xf | f
		X.addRule(new RegularRule(X, f));
		X.addRule(new ConstantRule(f));
		solvable(new RegularRule(X, f̅, f̅, f̅));
	}
	
	@Test
	public void regularMultipleConsumers2() {
		// X: Yg̅g̅g̅
		// Y: Yg | f
		X.addRule(new RegularRule(Y, g̅, g̅, g̅));
		Y.addRule(new RegularRule(Y, g));
		Y.addRule(new ConstantRule(f));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularExclusionUnsolvable() {
		// X: Y¬f | Xg̅
		// Y: Yf | f
		X.addRule(new RegularRule(Y, not_f));
		X.addRule(new RegularRule(X, g̅));
		Y.addRule(new RegularRule(Y, f));
		Y.addRule(new ConstantRule(f));
		unsolvable(consumeFOnX);
	}
	
	@Test
	public void regularExclusionSolvable() {
		// X: Y¬f | Xg̅
		// Y: Yg | f
		X.addRule(new RegularRule(Y, not_f));
		X.addRule(new RegularRule(X, g̅));
		Y.addRule(new RegularRule(Y, g));
		Y.addRule(new ConstantRule(f));
		solvable(consumeFOnX);
	}
	
	@Test
	public void contextFreeConstantSubstitution() {
		//X: fX | f̅
		X.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[0]));
		X.addRule(new ConstantRule(f̅));
		solvable(consumeFOnX);
	}
	
	@Test
	public void contextFreeConstantSubstitutionUnsolvable() {
		//X: gX | g̅
		X.addRule(new ContextFreeRule(new Terminal[] {g}, X, new Terminal[0]));
		X.addRule(new ConstantRule(g̅));
		unsolvable(consumeFOnX);
	}
	
	@Test
	public void hardPcpInstance() {
		//X: fgggXg̅ | gfXg̅ | fXf̅g̅f̅ | ggXg̅g̅f̅ | fgg | g̅f̅ | f̅
		X.addRule(new ContextFreeRule(new Terminal[] {f, g, g, g}, X, new Terminal[] {g̅}));
		X.addRule(new ContextFreeRule(new Terminal[] {g, f}, X, new Terminal[] {g̅}));
		X.addRule(new ContextFreeRule(new Terminal[] {f}, X, new Terminal[] {f̅, g̅, f̅}));
		X.addRule(new ConstantRule(f, g, g));
		X.addRule(new ConstantRule(g̅, f̅));
		X.addRule(new ConstantRule(f̅));
		solvable(new NonLinearRule(new ConstantRule(h), new RegularRule(X,h̄)));
	}
	
	@Test
	public void nonLinearContextFree() {
		//X: YX | f̅Xg̅ | g
		//Y: Xg̅ | f
		X.addRule(new NonLinearRule(new RegularRule(Y), new RegularRule(X)));
		X.addRule(new ContextFreeRule(new Terminal[] {f̅}, X, new Terminal[] {g̅}));
		X.addRule(new ConstantRule(g));
		Y.addRule(new RegularRule(X, g̅));
		Y.addRule(new ConstantRule(f));
		solvable(consumeFOnX);
	}
	
	@Test
	public void nonApproximatedContextFreeRule() {
		//X: f̅hhYg | ffXg̅
		//Y: Yh̄ | h̄
		X.addRule(new ContextFreeRule(new Terminal[] {f̅, h, h}, Y, new Terminal[] {g}));
		X.addRule(new ContextFreeRule(new Terminal[] {f, f}, X, new Terminal[] {g̅}));
		Y.addRule(new RegularRule(Y, h̄));
		Y.addRule(new ConstantRule(h̄));
		solvable(consumeFOnX);
	}
	
	@Test
	public void lateRule() {
		SearchTreeResultListener listener = delayed(consumeFOnX);
		verify(listener, never()).solved();
		X.addRule(new ConstantRule(f));
		verify(listener).solved();
	}
	
	//TODO more late rule tests
	//TODO tests for removing rules
	
	private void solvable(Rule rule) {
		SearchTreeResultListener listener = delayed(rule);
		try {
			Mockito.verify(listener).solved();
		} catch(MockitoAssertionError e) {
			throw new AssertionError(rule+" should be solvable, but was not. Given rule set:\n"+new ToStringRuleVisitor(rule), e);
		}
	}
	
	private void unsolvable(final Rule rule) {
		runSearch(new SearchTreeResultListener() {
			@Override
			public void solved() {
				fail(rule+" should not be solvable, but was solved. Given rule set:\n"+new ToStringRuleVisitor(rule));
			}
		}, rule);
	}
	
	private SearchTreeResultListener delayed(Rule rule) {
		SearchTreeResultListener listener = mock(SearchTreeResultListener.class);
		runSearch(listener, rule);
		return listener;
	}
	
	private void runSearch(SearchTreeResultListener listener, Rule rule) {
		SearchTreeViewer treeViewer = new SearchTreeViewer();
		new RegularOverApproximizer().approximate(rule);
		SearchTree searchTree = new SearchTree(rule, Option.some(treeViewer), false);
		searchTree.addListener(listener);
		searchTree.search();
		System.out.println(treeViewer);
	}
}
