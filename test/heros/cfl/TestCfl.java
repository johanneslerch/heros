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

public class TestCfl {

	@Test
	public void regularTrivialSolvable1() {
		// X: f | g
		// solvable X-f ?
		NonTerminal x = new NonTerminal("X");
		x.addRule(new RegularRule(new ProducingTerminal("f")));
		x.addRule(new RegularRule(new ProducingTerminal("g")));
		solvable(x, new ConsumingTerminal("f"));
	}
	
	@Test
	public void regularTrivialSolvable2() {
		// X: Y
		// Y: f | g
		// solvable X-f ?
		NonTerminal x = new NonTerminal("X");
		NonTerminal y = new NonTerminal("Y");
		x.addRule(new RegularRule(y));
		y.addRule(new RegularRule(new ProducingTerminal("f")));
		y.addRule(new RegularRule(new ProducingTerminal("g")));
		solvable(x, new ConsumingTerminal("f"));
	}
	
	@Test
	public void regularTrivialSolvable3() {
		// X: X | Y
		// Y: Z
		// Z: f
		// solvable X-f ?
		NonTerminal x = new NonTerminal("X");
		NonTerminal y = new NonTerminal("Y");
		NonTerminal z = new NonTerminal("Z");
		x.addRule(new RegularRule(x));
		x.addRule(new RegularRule(y));
		y.addRule(new RegularRule(z));
		z.addRule(new RegularRule(new ProducingTerminal("f")));
		solvable(x, new ConsumingTerminal("f"));
	}
	
	@Test
	public void regularProduceConsumedTerminals() {
		// X: X -g | Y g
		// Y: Y f
		// solvable X-f ?
		NonTerminal x = new NonTerminal("X");
		NonTerminal y = new NonTerminal("Y");
		x.addRule(new RegularRule(x, new ConsumingTerminal("g")));
		x.addRule(new RegularRule(y, new ProducingTerminal("g")));
		y.addRule(new RegularRule(y, new ProducingTerminal("f")));
		solvable(x, new ConsumingTerminal("f"));
	}
	
	@Test
	public void regularUnsolvableLoop() {
		// X: X -g | Y h
		// Y: Y f
		// solvable X-f ?
		NonTerminal x = new NonTerminal("X");
		NonTerminal y = new NonTerminal("Y");
		x.addRule(new RegularRule(x, new ConsumingTerminal("g")));
		x.addRule(new RegularRule(y, new ProducingTerminal("h")));
		y.addRule(new RegularRule(y, new ProducingTerminal("f")));
		unsolvable(x, new ConsumingTerminal("f"));
	}

	private void solvable(NonTerminal x, ConsumingTerminal consumingTerminal) {
		assertResult(Solvable, x, consumingTerminal);
	}
	
	private void unsolvable(NonTerminal x, ConsumingTerminal consumingTerminal) {
		assertResult(NotSolvable, x, consumingTerminal);
	}
	
	private void unknown(NonTerminal x, ConsumingTerminal consumingTerminal) {
		assertResult(Unknown, x, consumingTerminal);
	}

	private void assertResult(SolverResult expected, NonTerminal x, ConsumingTerminal consumingTerminal) {
		Solver solver = new Solver(x.getRules(), consumingTerminal);
		SolverResult actual = solver.solve();
		System.out.println(solver.explain());
		if(!expected.equals(actual)) {
			fail(x.toString()+consumingTerminal+" should be "+expected+", but was "+actual+". Given the rule set:\n"+x.printRules());
		}
	}
}
