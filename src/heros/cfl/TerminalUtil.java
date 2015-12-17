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
		
		int skip = 0;
		for(int i=0; i<right.length; i++) {
			if(right[i].isConsumer() && left.length>i && left[left.length-i-1].isProducing((ConsumingTerminal) right[i]))  {
				skip++;
			}
			else
				break;
		}
		
		Terminal[] newTerminals = Arrays.copyOf(left, left.length+right.length -skip*2);
		System.arraycopy(right, skip, newTerminals, left.length-skip, right.length-skip);
		return newTerminals;
	}
}
