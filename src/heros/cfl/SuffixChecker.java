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

import heros.cfl.Rule.Traversal;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class SuffixChecker {

	private boolean isSuffix;
	private Optional<Rule> prefix;

	public SuffixChecker(Rule rule, Rule suffix) {
		if(rule.equals(suffix)) {
			isSuffix = true;
			prefix = Optional.<Rule>absent();
		}
		else {
			Iterator<Object> ruleTokens = Lists.reverse(tokenize(rule)).iterator();
			Iterator<Object> suffixTokens = Lists.reverse(tokenize(suffix)).iterator();
			List<Object> prefixTokens = Lists.newLinkedList();
			boolean hasSuffix = false;
			while(ruleTokens.hasNext()) {
				Object ruleToken = ruleTokens.next();
				if(suffixTokens.hasNext()) {
					Object suffixToken = suffixTokens.next();
					if(suffixToken.equals(ruleToken)) {
						hasSuffix = true;
						continue;
					}
					else
						hasSuffix = false;
				}
				if(hasSuffix) {
					prefixTokens.add(ruleToken);
				}
			}
			
			if(hasSuffix && !suffixTokens.hasNext()) {
				isSuffix = true;
				Rule prefixRule = new ConstantRule();
				for(Object token : Lists.reverse(prefixTokens)) {
					if(token instanceof NonTerminal) {
						prefixRule = prefixRule.append(new RegularRule((NonTerminal) token));
					} else {
						prefixRule = prefixRule.append((Terminal) token);
					}
				}
				prefix = prefixRule.isEmpty() ? Optional.<Rule> absent() : Optional.<Rule> of(prefixRule);
			}
			else {
				isSuffix = false;
				prefix = Optional.<Rule> absent();
			}
		}
	}
	
	private List<Object> tokenize(Rule r) {
		final List<Object> result = Lists.newLinkedList();
		r.traverse(new Traversal() {
			@Override
			public void nonTerminal(NonTerminal nt) {
				result.add(nt);
			}

			@Override
			public void terminal(Terminal t) {
				result.add(t);
			}
		});
		return result;
	}

	public boolean isSuffix() {
		return isSuffix;
	}

	public Optional<Rule> getPrefix() {
		return prefix;
	}

}
