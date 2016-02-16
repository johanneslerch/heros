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

import static org.junit.Assert.*;
import heros.solver.Pair;

import org.junit.Ignore;
import org.junit.Test;

import static heros.cfl.TerminalUtil.isBalanced;
import static heros.cfl.TerminalUtil.split;
import static heros.cfl.TerminalUtil.BalanceResult.*;

public class TerminalUtilTest {

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

	@Test
	public void consumerProducerConsumerMismatch() {
		assertEquals(IMBALANCED, isBalanced(f̅, g, f̅));
	}
	
	@Test
	public void consumerProducerConsumerMatch() {
		assertEquals(MORE_CONSUMERS, isBalanced(f̅, g, g̅));
	}
	
	@Test
	public void consumerProducerConsumerProducerConsumerMismatch() {
		assertEquals(IMBALANCED, isBalanced(f̅, g, g̅, h, f̅));
	}
	
	@Test
	public void consumerProducerConsumerProducerConsumerMatch() {
		assertEquals(MORE_CONSUMERS, isBalanced(f̅, g, g̅, h, h̄));
	}
	
	@Test
	public void producerOnly() {
		assertEquals(BALANCED, isBalanced(f, g, h));
	}
	
	@Test
	public void consumerOnly() {
		assertEquals(MORE_CONSUMERS, isBalanced(f̅, g̅));
	}
	
	@Test
	public void producerConsumerMatch() {
		assertEquals(BALANCED, isBalanced(f, g, g̅));
	}
	
	@Test
	public void producerConsumerMismatch() {
		assertEquals(IMBALANCED, isBalanced(f, g̅));
	}
	
	@Test
	public void excludeConsumeMatch() {
		assertEquals(IMBALANCED, isBalanced(not_g, g̅));
	}
	
	@Test
	public void excludeProduce() {
		assertEquals(MORE_CONSUMERS, isBalanced(not_g, f));
	}
	
	@Test
	public void produceExcludeMatch() {
		assertEquals(IMBALANCED, isBalanced(f, not_f));
	}
	
	@Test
	public void excludeProduceConsumeMismatch() {
		assertEquals(IMBALANCED, isBalanced(not_f, f, g̅));
	}
	
	@Test
	public void splitExclusionProduction() {
		prefix(not_f).suffix(g, g, f).combineAndSplit();
	}
	
	@Test
	public void splitConsumeProduction() {
		prefix(g̅).suffix(g, g, f).combineAndSplit();
	}
	
	@Test
	public void splitProduceOnly() {
		prefix().suffix(g, f).combineAndSplit();
	}
	
	@Test
	public void splitConsumeOnly() {
		prefix(g̅).suffix().combineAndSplit();
	}
	
	@Test
	public void splitConsumeExclude() {
		prefix(g̅, not_f).suffix().combineAndSplit();
	}
	
	@Test
	public void splitConsumeExcludeProduce() {
		prefix(g̅, not_f).suffix(g).combineAndSplit();
	}
	
	@Test
	public void splitExcludeProduce() {
		prefix(not_f).suffix(g).combineAndSplit();
	}
	
	private TerminalSplit prefix(Terminal...terminals) {
		return new TerminalSplit(terminals);
	}
	
	private static class TerminalSplit {
		private Terminal[] prefix;
		private Terminal[] suffix;

		public TerminalSplit(Terminal[] prefix) {
			this(prefix, new Terminal[0]);
		}
		
		public TerminalSplit(Terminal[] prefix, Terminal[] suffix) {
			this.prefix = prefix;
			this.suffix = suffix;
		}

		public TerminalSplit suffix(Terminal...terminals) {
			return new TerminalSplit(prefix, terminals);
		}

		public void combineAndSplit() {
			Terminal[] combined = TerminalUtil.append(prefix, suffix);
			Pair<Terminal[], Terminal[]> split = split(combined);
			assertArrayEquals(prefix, split.getO1());
			assertArrayEquals(suffix, split.getO2());
		}
	}
}
