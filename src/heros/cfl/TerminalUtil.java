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

public class TerminalUtil {

	public static BalanceResult isBalanced(Terminal...terminals) {
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
					return BalanceResult.MORE_CONSUMERS;
			}
			else if(terminals[i].isExclusion()) {
				if(i+1 != terminals.length)
					return BalanceResult.IMBALANCED;
				int correspondingProducerIndex = 2*firstConsumer-i-1;
				if(correspondingProducerIndex < 0)
					return BalanceResult.MORE_CONSUMERS;
				if(terminals[i].isExcluding(terminals[correspondingProducerIndex].getRepresentation()))
					return BalanceResult.IMBALANCED;
			}
		}
		return BalanceResult.BALANCED;
	}
	
	public static enum BalanceResult {
		BALANCED, IMBALANCED, MORE_CONSUMERS
	}
	
	
	public static Terminal[] append(Terminal[] left, Terminal[] right) {
		if(right.length == 0)
			return left;
		if(left.length == 0)
			return right;
		
		if(right[0].isExclusion()) {
			assert right.length == 1;
			if(left[left.length-1].isExclusion()) {
				Terminal[] newTerminals = Arrays.copyOf(left, left.length);
				newTerminals[left.length-1] = ((ExclusionTerminal) left[left.length-1]).merge((ExclusionTerminal) right[0]);
				return newTerminals;
			}
		}
		
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
				if(right[i].isExcluding(left[left.length-i-1].getRepresentation()))
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
}
