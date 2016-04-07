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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.base.Optional;

public class SuffixCheckerTest {

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

	NonTerminal A = new NonTerminal("A");
	NonTerminal B = new NonTerminal("B");
	NonTerminal C = new NonTerminal("C");

	@Test
	public void equal() {
		rule(new RegularRule(A)).withoutPrefix().hasSuffix(new RegularRule(A));
	}

	@Test
	public void constantSuffix() {
		rule(new ConstantRule(f, g)).withPrefix(new ConstantRule(f)).hasSuffix(new ConstantRule(g));
	}
	
	@Test
	public void regularSuffix() {
		rule(new RegularRule(A, f)).withPrefix(new RegularRule(A)).hasSuffix(new ConstantRule(f));
	}
	
	@Test
	public void nonLinearNotSuffixOfRegular() {
		rule(new RegularRule(A,f)).hasNoSuffix(new NonLinearRule(new RegularRule(B), new RegularRule(A,f)));
	}

	@Test
	public void nonLinearEqual() {
		rule(new NonLinearRule(new RegularRule(A), new NonLinearRule(new RegularRule(B), new RegularRule(C)))).withoutPrefix().hasSuffix(
				new NonLinearRule(new NonLinearRule(new RegularRule(A), new RegularRule(B)), new RegularRule(C)));
	}

	@Test
	public void nonLinearWithRegularSuffix() {
		rule(new NonLinearRule(new RegularRule(A, g), new RegularRule(B, f))).withPrefix(new RegularRule(A, g)).hasSuffix(new RegularRule(B, f));
	}

	@Test
	public void nonLinearWithContextFreeSuffix() {
		rule(new NonLinearRule(new RegularRule(A, g), new RegularRule(B, f))).withPrefix(new RegularRule(A)).hasSuffix(
				new ContextFreeRule(new Terminal[] { g }, B, new Terminal[] { f }));
	}

	@Test
	public void mismatch() {
		rule(new RegularRule(B, g)).hasNoSuffix(new ConstantRule(f));
	}

	private static RuleMatcher rule(Rule rule) {
		return new RuleMatcher(rule);
	}

	private static class RuleMatcher {

		private Rule rule;

		public RuleMatcher(Rule rule) {
			this.rule = rule;
		}

		public SuffixMatcher withoutPrefix() {
			return new SuffixMatcher(Optional.<Rule> absent());
		}

		public SuffixMatcher withPrefix(Rule prefix) {
			return new SuffixMatcher(Optional.of(prefix));
		}

		public void hasNoSuffix(Rule suffix) {
			assertFalse(rule + " should not have suffix " + suffix, new SuffixChecker(rule, suffix).isSuffix());
		}

		private class SuffixMatcher {

			private Optional<Rule> prefix;

			public SuffixMatcher(Optional<Rule> prefix) {
				this.prefix = prefix;
			}

			public void hasSuffix(Rule suffix) {
				SuffixChecker checker = new SuffixChecker(rule, suffix);
				assertTrue(rule + " should have suffix " + suffix, checker.isSuffix());
				assertEquals(prefix, checker.getPrefix());
			}
		}
	}
}
