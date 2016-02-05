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

import java.util.Arrays;

import fj.data.Option;

public class TerminalUtil {

	/**
	 * Precondition: sequence of terminals has been reduced already.
	 */
	public static BalanceResult isBalanced(Terminal...terminals) {
		BalanceResult result = BalanceResult.BALANCED;
		int firstConsumer = -1;
		for(int i=0; i<terminals.length; i++) {
			if(terminals[i].isConsumer()) {
				if(firstConsumer < 0)
					firstConsumer = i;
				int correspondingProducerIndex = 2*firstConsumer-i-1;
				if(correspondingProducerIndex >= 0) {
					if(!terminals[correspondingProducerIndex].isProducing((ConsumingTerminal) terminals[i]))
						return BalanceResult.IMBALANCED;
				}	
				else
					result = BalanceResult.MORE_CONSUMERS;
			}
			else if(terminals[i].isExclusion()) {
				if(i>0 && terminals[i-1] instanceof ProducingTerminal && terminals[i].isExcluding(terminals[i-1].getRepresentation()))
					return BalanceResult.IMBALANCED;
				if(i+1 < terminals.length && terminals[i+1].isConsumer() && terminals[i].isExcluding(terminals[i+1].getRepresentation()))
					return BalanceResult.IMBALANCED;
				else
					return BalanceResult.MORE_CONSUMERS;
			}
			else
				firstConsumer = -1;
		}
		return result;
	}
	
	public static enum BalanceResult {
		BALANCED, IMBALANCED, MORE_CONSUMERS
	}
	
	
	public static Terminal[] append(Terminal[] left, Terminal[] right) {
		if(right.length == 0)
			return left;
		if(left.length == 0)
			return right;
		
		int skipLeft = 0;
		int skipRight = 0;
		for(int i=0; i<right.length; i++) {
			if(right[i].isConsumer() && left.length>i)  {
				ConsumingTerminal consumer = (ConsumingTerminal) right[i];
				if(left[left.length-i-1].isProducing(consumer)) {
					skipLeft++;
					skipRight++;
				}
				else if(left[left.length-i-1].isExclusion() && !left[left.length-i-1].isExcluding(consumer.getRepresentation())) {
					skipLeft++;
				}
				else
					break;
			}
			else if(right[i].isExclusion() && left.length>i && !left[left.length-i-1].isConsumer()) {
				if(left[left.length-i-1].isExclusion()) {
					skipRight++;
					Terminal[] newTerminals = Arrays.copyOf(left, left.length-skipLeft + right.length-skipRight);
					System.arraycopy(right, skipRight, newTerminals, left.length-skipLeft, right.length-skipRight);
					newTerminals[left.length-i-1] = ((ExclusionTerminal) left[left.length-i-1]).merge((ExclusionTerminal) right[i]);
					return newTerminals;
				}
				else if(right[i].isExcluding(left[left.length-i-1].getRepresentation()))
					break;
				else
					skipRight++;
			}
			else
				break;
		}
		
		Terminal[] newTerminals = Arrays.copyOf(left, left.length-skipLeft + right.length-skipRight);
		System.arraycopy(right, skipRight, newTerminals, left.length-skipLeft, right.length-skipRight);
		return newTerminals;
	}

	public static boolean containsConstraints(Terminal[] terminals) {
		for(Terminal t : terminals) {
			if(t.isConsumer() || t.isExclusion())
				return true;
		}
		return false;
	}

	public static BalanceResult isBalanced(Rule rule) {
		return rule.accept(new RuleVisitor<BalanceResult>() {
			@Override
			public BalanceResult visit(ContextFreeRule contextFreeRule) {
				return TerminalUtil.isBalanced(contextFreeRule.getRightTerminals());
			}

			@Override
			public BalanceResult visit(NonLinearRule nonLinearRule) {
				return nonLinearRule.getRight().accept(this);
			}

			@Override
			public BalanceResult visit(RegularRule regularRule) {
				return TerminalUtil.isBalanced(regularRule.getTerminals());
			}

			@Override
			public BalanceResult visit(ConstantRule constantRule) {
				return TerminalUtil.isBalanced(constantRule.getTerminals());
			}
		});
	}

	public static Option<Terminal> lastTerminal(Rule rule) {
		return rule.accept(new RuleVisitor<Option<Terminal>>() {
			private Option<Terminal> last(Terminal[] terminals) {
				if(terminals.length > 0)
					return Option.some(terminals[terminals.length-1]);
				else
					return Option.none();
			}

			@Override
			public Option<Terminal> visit(ContextFreeRule contextFreeRule) {
				return last(contextFreeRule.getRightTerminals());
			}

			@Override
			public Option<Terminal> visit(NonLinearRule nonLinearRule) {
				return nonLinearRule.getRight().accept(this);
			}

			@Override
			public Option<Terminal> visit(RegularRule regularRule) {
				return last(regularRule.getTerminals());
			}

			@Override
			public Option<Terminal> visit(ConstantRule constantRule) {
				return last(constantRule.getTerminals());
			}
		});
	}
}
