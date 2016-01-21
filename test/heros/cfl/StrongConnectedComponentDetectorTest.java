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
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;

@SuppressWarnings("unchecked")
public class StrongConnectedComponentDetectorTest {

	private NonTerminal[] vertices;
	
	private void terminals(int n) {
		vertices = new NonTerminal[n];
		for(int i=0; i<n; i++) 
			vertices[i] = new NonTerminal(i);
	}
	
	private void edge(int v, int w) {
		vertices[v].addRule(new RegularRule(vertices[w]));
	}
	
	private void assertSccs(Set<NonTerminal>... expectation) {
		Collection<Set<NonTerminal>> actual = new StrongConnectedComponentDetector(vertices[0]).results();
		assertEquals(expectation.length, actual.size());
		for(Set<NonTerminal> expectedScc : expectation) {
			assertTrue(actual.contains(expectedScc));
		}
	}
	
	private Set<NonTerminal> scc(int... is) {
		Set<NonTerminal> result = Sets.newHashSet();
		for(int i : is)
			result.add(vertices[i]);
		return result;
	}
	
	@Test
	public void test() {
		terminals(6);
		edge(0, 1);
		edge(0, 2);
		edge(1, 2);
		edge(1, 3);
		edge(3, 2);
		edge(3, 4);
		edge(4, 1);
		edge(4, 5);
		edge(5, 3);
		edge(5, 2);
		assertSccs(scc(0), scc(1,3,4,5), scc(2));
	}
}
