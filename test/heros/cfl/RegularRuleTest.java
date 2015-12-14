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

import org.junit.Test;

public class RegularRuleTest {

	private RegularRule sut;

	@Test
	public void oneMatch() {
		sut = new RegularRule(new ProducingTerminal("a"), new ConsumingTerminal("a"));
		assertTrue(sut.isPossible());
	}
	
	@Test
	public void twoMatches() {
		sut = new RegularRule(new ProducingTerminal("a"), new ProducingTerminal("b"), new ConsumingTerminal("b"), new ConsumingTerminal("a"));
		assertTrue(sut.isPossible());
	}
	
	@Test
	public void oneMismatch() {
		sut = new RegularRule(new ProducingTerminal("a"), new ConsumingTerminal("b"));
		assertFalse(sut.isPossible());
	}
	
	@Test
	public void twoMismatchFirst() {
		sut = new RegularRule(new ProducingTerminal("a"), new ProducingTerminal("b"), new ConsumingTerminal("b"), new ConsumingTerminal("c"));
		assertFalse(sut.isPossible());
	}
	
	@Test
	public void twoMismatchSecond() {
		sut = new RegularRule(new ProducingTerminal("a"), new ProducingTerminal("b"), new ConsumingTerminal("c"), new ConsumingTerminal("a"));
		assertFalse(sut.isPossible());
	}
	
	@Test
	public void wrongOrder() {
		sut = new RegularRule(new ProducingTerminal("a"), new ProducingTerminal("b"), new ConsumingTerminal("a"), new ConsumingTerminal("b"));
		assertFalse(sut.isPossible());
	}
	
	@Test
	public void moreConsumers() {
		sut = new RegularRule(new ProducingTerminal("a"), new ConsumingTerminal("a"), new ConsumingTerminal("b"));
		assertTrue(sut.isPossible());
	}
	
	@Test
	public void moreProducers() {
		sut = new RegularRule(new ProducingTerminal("a"), new ProducingTerminal("b"), new ConsumingTerminal("b"));
		assertTrue(sut.isPossible());
	}
	
	@Test
	public void appendMatch() {
		sut = new RegularRule(new ProducingTerminal("a"));
		Rule actual = sut.append(new ConsumingTerminal("a"));
		assertEquals(new RegularRule(), actual);
	}
	
	@Test
	public void appendMoreConsumers() {
		sut = new RegularRule(new ProducingTerminal("a"));
		Rule actual = sut.append(new ConsumingTerminal("a"), new ConsumingTerminal("b"));
		assertEquals(new RegularRule(new ConsumingTerminal("b")), actual);
	}
	
	@Test
	public void appendMoreProducers() {
		sut = new RegularRule(new ProducingTerminal("a"), new ProducingTerminal("b"));
		Rule actual = sut.append(new ConsumingTerminal("b"));
		assertEquals(new RegularRule(new ProducingTerminal("a")), actual);
	}
	
	@Test
	public void appendMismatch() {
		sut = new RegularRule(new ProducingTerminal("a"));
		Rule actual = sut.append(new ConsumingTerminal("b"));
		assertEquals(new RegularRule(new ProducingTerminal("a"), new ConsumingTerminal("b")), actual);
	}
}
