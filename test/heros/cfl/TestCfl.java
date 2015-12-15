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

import static org.junit.Assert.fail;
import static heros.cfl.SolverResult.*;

import org.junit.Test;

import fj.data.Option;

public class TestCfl {

	NonTerminal x = new NonTerminal("X");
	NonTerminal y = new NonTerminal("Y");
	NonTerminal z = new NonTerminal("Z");
	RegularRule consumeFOnX = new RegularRule(x, new ConsumingTerminal("f"));
	
	@Test
	public void regularTrivialSolvable1() {
		// X: f | g
		// solvable X-f ?
		x.addRule(new RegularRule(new ProducingTerminal("f")));
		x.addRule(new RegularRule(new ProducingTerminal("g")));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularTrivialSolvable2() {
		// X: Y
		// Y: f | g
		// solvable X-f ?
		x.addRule(new RegularRule(y));
		y.addRule(new RegularRule(new ProducingTerminal("f")));
		y.addRule(new RegularRule(new ProducingTerminal("g")));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularTrivialSolvable3() {
		// X: X | Y
		// Y: Z
		// Z: f
		// solvable X-f ?
		x.addRule(new RegularRule(x));
		x.addRule(new RegularRule(y));
		y.addRule(new RegularRule(z));
		z.addRule(new RegularRule(new ProducingTerminal("f")));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularProduceConsumedTerminals() {
		// X: X -g | Y g
		// Y: Y f
		// solvable X-f ?
		x.addRule(new RegularRule(x, new ConsumingTerminal("g")));
		x.addRule(new RegularRule(y, new ProducingTerminal("g")));
		y.addRule(new RegularRule(y, new ProducingTerminal("f")));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularUnsolvableLoop() {
		// X: X -g | Y h
		// Y: Y f
		// solvable X-f ?
		x.addRule(new RegularRule(x, new ConsumingTerminal("g")));
		x.addRule(new RegularRule(y, new ProducingTerminal("h")));
		y.addRule(new RegularRule(y, new ProducingTerminal("f")));
		unsolvable(consumeFOnX);
	}
	//   ̅
	@Test
	public void regularReadingLoopSolvable() {
		// X: Xg̅ | Yg
		// Y: f
		x.addRule(new RegularRule(x, new ConsumingTerminal("g")));
		x.addRule(new RegularRule(y, new ProducingTerminal("g")));
		y.addRule(new RegularRule(new ProducingTerminal("f")));
		solvable(consumeFOnX);
	}
	
	@Test
	public void regularReadingTwoFieldsInLoopUnsolvable() {
		// X: Xg̅f̅ | Yg
		// Y: f
		x.addRule(new RegularRule(x, new ConsumingTerminal("g"), new ConsumingTerminal("f")));
		x.addRule(new RegularRule(y, new ProducingTerminal("g")));
		y.addRule(new RegularRule(new ProducingTerminal("f")));
		unsolvable(consumeFOnX);
	}

	private void solvable(RegularRule rule) {
		assertResult(Solvable, rule);
	}
	
	private void unsolvable(RegularRule rule) {
		assertResult(NotSolvable, rule);
	}
	
	private void unknown(RegularRule rule) {
		assertResult(Unknown, rule);
	}

	private void assertResult(SolverResult expected, RegularRule rule) {
		SearchTreeViewer treeViewer = new SearchTreeViewer();
		SearchTree searchTree = new SearchTree(rule, Option.some(treeViewer));
		SolverResult actual = searchTree.search();
		
		System.out.println(treeViewer);
		if(!expected.equals(actual)) {
			fail(rule+" should be "+expected+", but was "+actual+". Given the rule set:\n"+rule.accept(new ToStringRuleVisitor(rule)));
		}
	}
}
