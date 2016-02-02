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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

public class ConstantRuleTest {

	private ConstantRule sut;
	private Rule actual;
	private ProducingTerminal f = new ProducingTerminal("f");
	private ProducingTerminal g = new ProducingTerminal("g");
	private ConsumingTerminal f̅ = new ConsumingTerminal("f");
	private ConsumingTerminal g̅ = new ConsumingTerminal("g");
	private ProducingTerminal h = new ProducingTerminal("h");
	private ConsumingTerminal h̄ = new ConsumingTerminal("h");
	private ExclusionTerminal not_f = new ExclusionTerminal("f");
	private ExclusionTerminal not_g = new ExclusionTerminal("g");
	private ExclusionTerminal not_fg = new ExclusionTerminal("f", "g");
	
	@Test
	public void oneMatch() {
		sut = new ConstantRule(f, f̅);
		assertTrue(sut.isPossible());
	}
	
	@Test
	public void twoMatches() {
		sut = new ConstantRule(f, g, g̅, f̅);
		assertTrue(sut.isPossible());
	}
	
	@Test
	public void oneMismatch() {
		sut = new ConstantRule(f, g̅);
		assertFalse(sut.isPossible());
	}
	
	@Test
	public void twoMismatchFirst() {
		sut = new ConstantRule(f, g, g̅, h̄);
		assertFalse(sut.isPossible());
	}
	
	@Test
	public void twoMismatchSecond() {
		sut = new ConstantRule(f, g, h̄, f̅);
		assertFalse(sut.isPossible());
	}
	
	@Test
	public void wrongOrder() {
		sut = new ConstantRule(f, g, f̅, g̅);
		assertFalse(sut.isPossible());
	}
	
	@Test
	public void moreConsumers() {
		sut = new ConstantRule(f, f̅, g̅);
		assertFalse(sut.isPossible());
	}
	
	@Test
	public void moreProducers() {
		sut = new ConstantRule(f, g, g̅);
		assertTrue(sut.isPossible());
	}
	
	@Test
	public void consumerProducerConsumer() {
		sut = new ConstantRule(f̅, h, f̅);
		assertFalse(sut.isPossible());
		assertFalse(new RegularRule(new NonTerminal("X"), sut).isPossible());
	}
	
	@Test
	public void appendMatch() {
		sut = new ConstantRule(f);
		actual = sut.append(f̅);
		assertEquals(new ConstantRule(), actual);
	}
	
	@Test
	public void ConstantRule() {
		sut = new ConstantRule(f);
		actual = sut.append(f̅, g̅);
		assertEquals(new ConstantRule(g̅), actual);
	}
	
	@Test
	public void appendMoreProducers() {
		sut = new ConstantRule(f, g);
		actual = sut.append(g̅);
		assertEquals(new ConstantRule(f), actual);
	}
	
	@Test
	public void appendMismatch() {
		sut = new ConstantRule(f);
		actual = sut.append(g̅);
		assertEquals(new ConstantRule(f, g̅), actual);
	}
	
	@Test
	public void appendExlusionAndConsumingMatch() {
		sut = new ConstantRule(not_f);
		actual = sut.append(f̅);
		assertFalse(actual.isPossible());
	}
	
	@Test
	public void appendExclusionAndConsumingMismatch() {
		sut = new ConstantRule(not_f);
		actual = sut.append(g̅);
		assertEquals(new ConstantRule(g̅), actual);
		assertEquals(TerminalUtil.BalanceResult.MORE_CONSUMERS, TerminalUtil.isBalanced(actual.getTerminals()));
	}
	
	@Test
	public void appendProducingAndExclusionMismatch() {
		sut = new ConstantRule(f);
		actual = sut.append(not_g);
		assertEquals(new ConstantRule(f), actual);
	}
	
	@Test
	public void appendProducingAndExclusionMatch() {
		sut = new ConstantRule(f);
		actual = sut.append(not_f);
		assertFalse(actual.isPossible());
	}
	
	@Test
	public void appendExclusionAndExclusion() {
		sut = new ConstantRule(not_f);
		actual = sut.append(not_g);
		assertEquals(new ConstantRule(not_fg), actual);
		assertEquals(TerminalUtil.BalanceResult.MORE_CONSUMERS, TerminalUtil.isBalanced(actual.getTerminals()));
	}
	
	@Test
	public void appendConsumingAndExclusion() {
		sut = new ConstantRule(f̅);
		actual = sut.append(not_g);
		assertEquals(new ConstantRule(f̅, not_g), actual);
		assertEquals(TerminalUtil.BalanceResult.MORE_CONSUMERS, TerminalUtil.isBalanced(actual.getTerminals()));
	}
	
	@Test
	public void appendExclusionAndProducing() {
		sut = new ConstantRule(not_g);
		actual = sut.append(f);
		assertEquals(new ConstantRule(not_g, f), actual);
	}
	
	@Test
	public void appendProducingExcludingProducing() {
		sut = new ConstantRule(f);
		actual = sut.append(not_g, g);
		assertEquals(new ConstantRule(f, g), actual);
	}
	
	@Test
	public void appendConsumingExclusionAndExclusionProducing() {
		sut = new ConstantRule(f̅, not_g);
		actual = sut.append(not_f, g);
		assertEquals(new ConstantRule(f̅, not_fg, g), actual);
	}
	
	@Test
	public void appendConsumerExclusionProduce() {
		sut = new ConstantRule(f̅);
		actual = sut.append(not_f, g);
		assertEquals(new ConstantRule(f̅, not_f, g), actual);
	}
}
