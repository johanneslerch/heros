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

import static heros.cfl.StrongConnectedComponentDetector.from;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import heros.cfl.StrongConnectedComponentDetector.Edge;

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
		vertices[v].addRule(new RegularRule(vertices[w], new ProducingTerminal("test")));
	}
	
	private void assertSccs(StrongConnectedComponentDetector sut, Set<NonTerminal>... expectation) {
		Collection<Set<NonTerminal>> actual = sut.results();
		assertEquals(expectation.length, actual.size());
		for(Set<NonTerminal> expectedScc : expectation) {
			assertTrue("Expected SCC '"+expectedScc+"', but not present in actual SCCs: "+actual, actual.contains(expectedScc));
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
		assertSccs(new StrongConnectedComponentDetector(vertices[0]), scc(1,3,4,5));
	}
	
	@Test
	public void test2() {
		terminals(5);
		edge(0, 1);
		edge(1, 2);
		edge(2, 0);
		edge(2, 1);
		edge(2, 3);
		edge(3, 4);
		assertSccs(new StrongConnectedComponentDetector(vertices[0]), scc(0,1,2));
	}
	
	@Test
	public void test3() {
		terminals(4);
		edge(0, 1);
		edge(1, 2);
		edge(2, 3);
		edge(3, 2);
		assertSccs(new StrongConnectedComponentDetector(vertices[0]), scc(2,3));
	}
	
	@Test
	public void test4() {
		terminals(4);
		edge(0, 1);
		edge(2, 3);
		edge(3, 2);
		assertSccs(new StrongConnectedComponentDetector(vertices[0], vertices[2]), scc(2, 3));
	}
	
	@Test
	public void test5() {
		terminals(4);
		edge(0, 1);
		edge(2, 3);
		edge(3, 2);
		assertSccs(new StrongConnectedComponentDetector(vertices[0], vertices[1], vertices[2]), scc(2, 3));
	}
	
	@Test
	public void test6() {
		terminals(4);
		edge(0, 1);
		edge(2, 3);
		edge(3, 2);
		assertSccs(new StrongConnectedComponentDetector(vertices[2], vertices[1], vertices[0]), scc(2, 3));
	}
	
	@Test
	public void test7() {
		terminals(4);
		edge(0, 1);
		edge(1, 0);
		edge(1, 2);
		edge(3, 2);
		assertSccs(new StrongConnectedComponentDetector(vertices[0], vertices[3]), scc(0, 1));
	}
	
	@Test
	public void testSingleElementNoLoop() {
		terminals(1);
		assertSccs(new StrongConnectedComponentDetector(vertices[0]));
	}
	
	@Test
	public void testSingleElementLoop() {
		terminals(1);
		edge(0, 0);
		assertSccs(new StrongConnectedComponentDetector(vertices[0]), scc(0));
	}
	
	@Test
	public void testCycleInCycle() {
		terminals(4);
		edge(0, 1);
		edge(1, 0);
		edge(0, 2);
		edge(2, 3);
		edge(3, 0);
		assertSccs(new StrongConnectedComponentDetector(vertices[0]), scc(0,1,2,3));
	}
	
	@Test
	public void testCycleInCycle2() {
		terminals(4);
		edge(0, 1);
		edge(1, 0);
		edge(0, 2);
		edge(2, 3);
		edge(3, 1);
		assertSccs(new StrongConnectedComponentDetector(vertices[0]), scc(0,1,2,3));
	}
	
	@Test
	public void simpleInitEdge() {
		terminals(3);
		edge(0,1);
		edge(1,0);
		edge(2,1);
		assertSccs(new StrongConnectedComponentDetector(new Edge(vertices[1], vertices[2])), scc(0,1,2)); 
	}
	
	@Test
	public void initialEdge() {
		terminals(6);
		edge(1, 2);
		edge(1, 3);
		edge(3, 2);
		edge(3, 4);
		edge(4, 1);
		edge(4, 5);
		edge(5, 3);
		edge(5, 2);
		assertSccs(new StrongConnectedComponentDetector(from(vertices[0]).to(vertices[1])), scc(1,3,4,5));
	}
	
	@Test
	public void initialEdges() {
		terminals(6);
		edge(0,1);
		edge(1,0);
		edge(2,3);
		edge(3,1);
		edge(3,2);
		edge(4,1);
		edge(4,5);
		edge(5,4);
		assertSccs(new StrongConnectedComponentDetector(from(vertices[1]).to(vertices[2], vertices[4])), scc(0,1,2,3,4,5));
	}
	
	@Test
	public void multipleEntryPoints() {
		terminals(5);
		edge(0, 1);
		edge(1, 2);
		edge(2, 3);
		edge(4, 0);
		assertSccs(new StrongConnectedComponentDetector(vertices));
	}
	
	@Test
	public void multipleEntryPoints2() {
		terminals(5);
		edge(0, 1);
		edge(1, 2);
		edge(2, 3);
		edge(3, 4);
		edge(4, 0);
		assertSccs(new StrongConnectedComponentDetector(vertices), scc(0,1,2,3,4));
	}
	
	@Test
	public void separatedGraphs() {
		terminals(5);
		edge(0, 1);
		edge(1, 0);
		edge(2, 3);
		edge(3, 4);
		edge(4, 3);
		assertSccs(new StrongConnectedComponentDetector(vertices[1], vertices[2]), scc(0,1), scc(3,4));
	}
}
