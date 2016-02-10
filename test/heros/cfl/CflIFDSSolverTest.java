/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.cfl;



import static heros.cfl.utilities.CflTestHelper.callSite;
import static heros.cfl.utilities.CflTestHelper.exitStmt;
import static heros.cfl.utilities.CflTestHelper.flow;
import static heros.cfl.utilities.CflTestHelper.kill;
import static heros.cfl.utilities.CflTestHelper.normalStmt;
import static heros.cfl.utilities.CflTestHelper.over;
import static heros.cfl.utilities.CflTestHelper.overwriteField;
import static heros.cfl.utilities.CflTestHelper.prependField;
import static heros.cfl.utilities.CflTestHelper.readField;
import static heros.cfl.utilities.CflTestHelper.startPoints;
import static heros.cfl.utilities.CflTestHelper.to;
import heros.cfl.utilities.CflTestHelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

public class CflIFDSSolverTest {

	private CflTestHelper helper;

	@Before
	public void before() {
		System.err.println("-----");
		helper = new CflTestHelper();
	}
	
	@Rule
	public TestWatcher watcher = new TestWatcher() {
		protected void failed(Throwable e, org.junit.runner.Description description) {
			System.err.println("---failed: "+description.getMethodName()+" ----");
		};
	};
	
	@Test
	public void fieldReadAndWrite() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", prependField("f"), "2")).succ("c"),
				normalStmt("c", flow("2", "2")).succ("d"),
				normalStmt("d", flow("2", readField("f"), "4")).succ("e"),
				normalStmt("e", kill("4")).succ("f"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void createSummaryForBaseValue() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", overwriteField("field"), "2")).succ("c"),
				callSite("c").calls("foo", flow("2", "3")));
		
		helper.method("foo",startPoints("d"),
				normalStmt("d", flow("3", "4")).succ("e"),
				normalStmt("e", flow("4","4")).succ("f"));
		helper.runSolver(false, "a");
	}
	
	@Test
	public void reuseSummaryForBaseValue() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", overwriteField("field"), "2")).succ("c"),
				callSite("c").calls("foo", flow("2", "3")).retSite("retC", flow("2", "2")));
		
		helper.method("foo",startPoints("d"),
				normalStmt("d", flow("3", "4")).succ("e"),
				normalStmt("e", flow("4","4")).succ("f"),
				exitStmt("f").returns(over("c"), to("retC"), flow("4", "5")).returns(over("g"), to("retG"), flow("4", "6")));

		helper.method("xyz", 
				startPoints("g"),
				callSite("g").calls("foo", flow("0", overwriteField("anotherField"), "3")).retSite("retG", kill("0")));
		
		helper.runSolver(false, "a", "g");
	}
	
	@Test
	public void hold() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", prependField("field"), "2")).succ("c"),
				callSite("c").calls("foo", flow("2", "3")));
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d", flow("3", readField("notfield"), "5"), flow("3", "3")).succ("e"),
				normalStmt("e", flow("3","4")).succ("f"));
		helper.runSolver(false, "a");
	}
	
	@Test
	public void holdAndResume() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", prependField("field"), "2")).succ("c"),
				callSite("c").calls("foo", flow("2", "3")).retSite("rs", kill("2")),
				callSite("rs").calls("foo", flow("5", prependField("notfield"), "3")));
		
		helper.method("foo",startPoints("d"),
				normalStmt("d", flow("3", "3"), flow("3", readField("notfield"), "6")).succ("e"),
				normalStmt("e", flow("3","4"), kill("6")).succ("f"),
				exitStmt("f").returns(over("c"), to("rs"), flow("4", "5")));
		
		helper.runSolver(false, "a");
	}

	@Test
	public void doNotHoldIfInterestedTransitiveCallerExists() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")));
		
		helper.method("bar",
				startPoints("c"),
				callSite("c").calls("xyz", flow("2", "3")));
		
		helper.method("xyz", 
				startPoints("d"),
				normalStmt("d", flow("3", readField("f"), "4")).succ("e"),
				normalStmt("e", kill("4")).succ("f"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void prefixFactOfSummaryIgnored() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0","1")).succ("b"),
				callSite("b").calls("bar", flow("1", prependField("f"), "2")).retSite("e", kill("1")),
				callSite("e").calls("bar", flow("4", "2")).retSite("f", kill("4")),
				normalStmt("f", kill("5")).succ("g"));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", readField("f"), "3")).succ("d"),
				exitStmt("d").returns(over("b"), to("e"), flow("3", "4")).returns(over("e"), to("f"), flow("3", "5")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void doNotPauseZeroSources() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", readField("f"), "1")).succ("b"),
				normalStmt("b", kill("1")).succ("c"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void loopAndMerge() {
		helper.method("foo",
				startPoints("a0"),
				normalStmt("a0", flow("0", "1")).succ("a1"),
				callSite("a1").calls("bar", flow("1", prependField("g"), "1")));
		
		helper.method("bar",
				startPoints("b"),
				normalStmt("b", flow("1", prependField("f"), "1")).succ("c"),
				normalStmt("c", flow("1", "1")).succ("b").succ("d"),
				normalStmt("d", flow("1", readField("f"), "2")).succ("e"),
				normalStmt("e", kill("2")).succ("f"));
		
		helper.runSolver(false, "a0");
	}
	
	@Test
	public void pauseOnOverwrittenFieldOfInterest() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", overwriteField("f"), "2")).succ("d"),
				normalStmt("d").succ("e")); //only interested in 2.f, but f excluded so this should not be reached
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void pauseOnOverwrittenFieldOfInterest2() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("g"), "1")).succ("b"),
				callSite("b").calls("bar", flow("1", prependField("f"), "2")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", overwriteField("f"), "2")).succ("d"),
				normalStmt("d").succ("e")); //only interested in 2.f.g, but f excluded so this should not be reached
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void doNotPauseOnOverwrittenFieldOfInterestedPrefix() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("b"),
				callSite("b").calls("bar", flow("1", prependField("g"), "2")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", overwriteField("f"), "2")).succ("d"),
				normalStmt("d", kill("2")).succ("e")); 
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void pauseOnTransitiveExclusion() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")));
		
		helper.method("bar",
				startPoints("c"),
				callSite("c").calls("xyz", flow("2", "3")));
		
		helper.method("xyz",
				startPoints("d"),
				normalStmt("d", flow("3", overwriteField("f"), "3")).succ("e"),
				normalStmt("e").succ("f")); 
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void resumePausedOnTransitiveExclusion() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")));
		
		helper.method("bar",
				startPoints("c"),
				callSite("c").calls("xyz", flow("2", "3")));
		
		helper.method("xyz",
				startPoints("d"),
				normalStmt("d", flow("3", overwriteField("f"), "3"), flow("3", "4")).succ("e"),
				callSite("e").calls("bar", flow("4", prependField("g"), "2"), kill("3"))); 
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void resumeEdgePausedOnOverwrittenField() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")).retSite("e", kill("1")),
				callSite("e").calls("bar", flow("4", prependField("g"), "2")).retSite("f", kill("4")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", overwriteField("f"), "2"), flow("2", "3")).succ("d"),
				exitStmt("d").returns(over("b"), to("e"), flow("3", "4")).returns(over("e"), to("f"), kill("3"), kill("2"))); 
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void resumeEdgePausedOnOverwrittenFieldForPrefixes() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")).retSite("e", kill("1")),
				normalStmt("e", flow("4", readField("f"), "2")).succ("f"),
				callSite("f").calls("bar", flow("2", "2")).retSite("g", kill("2")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", overwriteField("f"), "2"), flow("2", "3")).succ("d"),
				exitStmt("d").returns(over("b"), to("e"), flow("3", "4")).returns(over("f"), to("g"), kill("3"), kill("2"))); 
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void exclusionOnPotentiallyInterestedCaller() {
		helper.method("foo",
				startPoints("sp"),
				normalStmt("sp", flow("0", "1")).succ("a"),
				callSite("a").calls("bar", flow("1", overwriteField("f"), "1")).retSite("d", kill("1")));
		
		helper.method("bar",
				startPoints("b"),
				normalStmt("b", flow("1", readField("f"), "2")).succ("c"),
				exitStmt("c").returns(over("a"), to("d")));
		
		helper.runSolver(false, "sp");
	}
	
	@Test
	public void registerPausedEdgeInLateCallers() {
		helper.method("foo", 
				startPoints("a"),
				normalStmt("a", flow("0", prependField("g"), "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "1")).retSite("e", kill("1")),
				normalStmt("e", flow("1", readField("g"), "3")).succ("f"),
				callSite("f").calls("bar", flow("3", "1")).retSite("g", kill("3"))); 
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("1", readField("f"), "2"), flow("1", "1")).succ("d"),
				exitStmt("d").returns(over("b"), to("e"), flow("1", "1") /* ignore fact 2, not possible with this caller ctx*/)
							 .returns(over("f"), to("g"), kill("1"), kill("2")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void resumeOnTransitiveInterestedCaller() {
		helper.method("foo",
				startPoints("sp"),
				normalStmt("sp", flow("0", prependField("f"), "1")).succ("a"),
				callSite("a").calls("bar", flow("1", "1")).retSite("f", kill("1")),
				callSite("f").calls("bar", flow("2", prependField("g"), "1")));
				
		helper.method("bar",
				startPoints("b"),
				callSite("b").calls("xyz", flow("1", "1")).retSite("e", kill("1")),
				exitStmt("e").returns(over("a"), to("f"), flow("2", "2")));
		
		helper.method("xyz",
				startPoints("c"),
				normalStmt("c", flow("1", readField("g"), "3"), flow("1", readField("f"), "2")).succ("d"),
				exitStmt("d").returns(over("b"), to("e"), flow("2", "2"), kill("3")));
		
				
		helper.runSolver(false, "sp");
	}
	
	@Test
	public void happyPath() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a", flow("0", "x")).succ("b"),
				normalStmt("b", flow("x", "x")).succ("c"),
				callSite("c").calls("foo", flow("x", "y")).retSite("f", flow("x", "x")),
				normalStmt("f", kill("x"), kill("u")).succ("g"));
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d", flow("y", "y", "z")).succ("e"),
				exitStmt("e").returns(over("c"), to("f"), flow("z", "u"), kill("y")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void reuseSummary() {
		helper.method("foo", 
				startPoints("a"),
				callSite("a").calls("bar", flow("0", "x")).retSite("b", kill("0")),
				callSite("b").calls("bar", flow("y", "x")).retSite("c", kill("y")),
				normalStmt("c", flow("w", "0")).succ("c0"));
		
		helper.method("bar",
				startPoints("d"),
				normalStmt("d", flow("x", "z")).succ("e"),
				exitStmt("e").returns(over("a"), to("b"), flow("z", "y"))
							  .returns(over("b"), to("c"), flow("z", "w")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void reuseSummaryForRecursiveCall() {
		helper.method("foo",
				startPoints("a"),
				callSite("a").calls("bar", flow("0", prependField("y"), "1")).retSite("b", kill("0")),
				normalStmt("b", flow("2", readField("x"), "3")).succ("c"));
		
		helper.method("bar",
				startPoints("g"),
				normalStmt("g", flow("1", "1")).succ("i").succ("h"),
				callSite("i").calls("bar", flow("1", prependField("x"), "1")).retSite("h", kill("1")),
				exitStmt("h").returns(over("a"), to("b"), kill("1"), flow("2" ,"2"))
							.returns(over("i"), to("h"), flow("1", "2"), flow("2", "2")));
				
		helper.runSolver(false, "a");
	}
	
	@Test
	public void branch() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "x")).succ("b2").succ("b1"),
				normalStmt("b1", flow("x", "x", "y")).succ("c"),
				normalStmt("b2", flow("x", "x")).succ("c"),
				normalStmt("c", flow("x", "z"), flow("y", "w")).succ("d"),
				normalStmt("d", flow("z"), flow("w")).succ("e"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void unbalancedReturn() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				exitStmt("b").returns(over("x"),  to("y"), flow("1", "1")));
		
		helper.method("bar", 
				startPoints("unused"),
				normalStmt("x").succ("y"),
				normalStmt("y", flow("1", "2")).succ("z"));
		
		helper.runSolver(true, "a");
	}
	
	@Test
	public void artificalReturnEdgeForNoCallersCase() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				exitStmt("b").returns(null, null, flow("1", "1")));
		
		helper.runSolver(true, "a");
	}
	
	@Test
	public void pauseEdgeMutuallyRecursiveCallers() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("x"), "1")).succ("b"),
				callSite("b").calls("bar",flow("1", "2")));
		
		helper.method("bar",
				startPoints("c"),
				callSite("c").calls("xyz", flow("2", "3")));
		
		helper.method("xyz",
				startPoints("d"),
				callSite("d").calls("bar", flow("3", "2")).retSite("e", flow("3", "3")),
				normalStmt("e", flow("3", readField("f"), "4")).succ("f"));
				
		helper.runSolver(false, "a");
	}
	
	@Test
	public void pauseDiamondShapedCallerChain() {
		helper.method("bar",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("x"), "1")).succ("b"),
				callSite("b").calls("foo1", flow("1", "2")).calls("foo2", flow("1", "2")));
		
		helper.method("foo1",
				startPoints("c1"),
				callSite("c1").calls("xyz", flow("2", "3")));
		
		helper.method("foo2",
				startPoints("c2"),
				callSite("c2").calls("xyz", flow("2", "3")));
		
		helper.method("xyz",
				startPoints("d"),
				normalStmt("d", flow("3", readField("f"), "4")).succ("e"),
				normalStmt("e").succ("f"));
		
		helper.runSolver(false, "a");
	}

	@Test
	public void dontPauseDiamondShapedCallerChain() {
		helper.method("bar",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("b"),
				callSite("b").calls("foo1", flow("1", "2")).calls("foo2", flow("1", "2")));
		
		helper.method("foo1",
				startPoints("c1"),
				callSite("c1").calls("xyz", flow("2", "3")));
		
		helper.method("foo2",
				startPoints("c2"),
				callSite("c2").calls("xyz", flow("2", "3")));
		
		helper.method("xyz",
				startPoints("d"),
				normalStmt("d", flow("3", readField("f"), "4")).succ("e"),
				normalStmt("e", kill("4")).succ("f"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void correctDeltaConstraintApplication() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "1")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("1", overwriteField("a"), "1")).succ("d"),
				callSite("d").calls("xyz", flow("1", "1")));
		
		helper.method("xyz",
				startPoints("e"),
				normalStmt("e", flow("1", readField("f"), "2")).succ("f"),
				callSite("f").calls("baz", flow("2", "3")));
		
		helper.method("baz",
				startPoints("g"),
				normalStmt("g", flow("3", readField("a"), "4")).succ("h"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void pauseForSameSourceMultipleTimes() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")));
				
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", readField("x"), "3"), flow("2", "2")).succ("d"),
				normalStmt("d", flow("2", readField("x"), "4")).succ("e"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void pauseForSameSourceMultipleTimesTransitively() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("b"),
				callSite("b").calls("xyz", flow("1", "2")).retSite("f", flow("1", "1")),
				callSite("f").calls("xyz", flow("1", "2")));
		
		helper.method("xyz",
				startPoints("g"),
				callSite("g").calls("bar", flow("2", "2")));
				
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", readField("x"), "3"), flow("2", "2")).succ("d"),
				normalStmt("d", flow("2", readField("x"), "4")).succ("e"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void multipleExclusions() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", overwriteField("h"), "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")));
				
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", overwriteField("f"), "3")).succ("d"),
				callSite("d").calls("xyz", flow("3", "4")));
		
		helper.method("xyz", 
				startPoints("e"),
				normalStmt("e", flow("4", overwriteField("g"), "5")).succ("f"),
				normalStmt("f", kill("5")).succ("g"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void unbalancedReturnWithFieldRead() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				exitStmt("b").returns(over("cs"), to("c"), flow("1", "2")));
		
		helper.method("xyz",
				startPoints("n/a"),
				normalStmt("cs").succ("c"),
				exitStmt("c").returns(over("cs2"), to("d"), flow("2", "2")));
		
		helper.method("bar",
				startPoints("unused"),
				normalStmt("cs2").succ("d"),
				normalStmt("d", flow("2", readField("f"), "3")).succ("e"),
				normalStmt("e", kill("3")).succ("f"));
		
		helper.runSolver(true, "a");
	}
	
	@Test
	public void unbalancedReturnAbstraction() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", prependField("f"), "2")).succ("c"),
				exitStmt("c").returns(over("cs"), to("rs"), flow("2", "2")));
		
		helper.method("bar",
				startPoints("unused"),
				normalStmt("cs").succ("rs"),
				normalStmt("rs", flow("2", "3")).succ("d"));
		
		helper.runSolver(true, "a");
	}
	
	@Test
	public void unbalancedReturnReadAbstractedField() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", prependField("f"), "2")).succ("c"),
				exitStmt("c").returns(over("cs"), to("rs"), flow("2", "2")));
		
		helper.method("bar",
				startPoints("unused"),
				normalStmt("cs").succ("rs"),
				normalStmt("rs", flow("2", "3")).succ("d"),
				normalStmt("d", flow("3", readField("f"), "4")).succ("e"),
				normalStmt("e", kill("4")).succ("f"));
		
		helper.runSolver(true, "a");
	}

	@Test
	public void unbalancedReturnReadUnwrittenAbstractedField() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", prependField("f"), "2")).succ("c"),
				exitStmt("c").returns(over("cs"), to("rs"), flow("2", "2")));
		
		helper.method("bar",
				startPoints("unused"),
				normalStmt("cs").succ("rs"),
				normalStmt("rs", flow("2", "3")).succ("d"),
				normalStmt("d", flow("3", readField("h"), "4")).succ("e"),
				normalStmt("e").succ("f"));
		
		helper.runSolver(true, "a");
	}
	
	@Test
	public void unbalancedReturnTransitiveAbstraction() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", prependField("f"), "2")).succ("c"),
				exitStmt("c").returns(over("cs1"), to("rs1"), flow("2", "2")));
		
		helper.method("bar",
				startPoints("unused1"),
				normalStmt("cs1").succ("rs1"),
				normalStmt("rs1", flow("2", prependField("g"), "3")).succ("d"),
				exitStmt("d").returns(over("cs2"), to("rs2"), flow("3", "4")));
		
		helper.method("xyz",
				startPoints("unused2"),
				normalStmt("cs2").succ("rs2"),
				normalStmt("rs2", flow("4", "5")).succ("e"),
				normalStmt("e", flow("5", readField("g"), "6")).succ("f"),
				normalStmt("f", flow("6", readField("f"), "7")).succ("g"),
				normalStmt("g", kill("7")).succ("h"));
		
		
		helper.runSolver(true, "a");
	}
	
	@Test
	public void unbalancedReturnPauseAndResume() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				exitStmt("b").returns(over("cs"), to("rs"), flow("1", prependField("g"), "2")));
		
		helper.method("bar",
				startPoints("unused"),
				normalStmt("cs").succ("rs"),
				normalStmt("rs", flow("2", "2")).succ("c").succ("d1"),
				exitStmt("c").returns(over("cs"), to("rs"), flow("2", prependField("f"), "2")),
				normalStmt("d1", flow("2", readField("f"), "3")).succ("d2"),
				normalStmt("d2", kill("3")).succ("e"));
		
		helper.runSolver(true, "a");
	}
	
	@Test
	public void abstractedReturnUseCallerInterest() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")).retSite("c", kill("1")),
				normalStmt("c", flow("2", readField("f"), "3")).succ("d"),
				normalStmt("d", kill("3")).succ("e"));
		
		helper.method("bar",
				startPoints("f"),
				exitStmt("f").returns(over("b"), to("c"), flow("2", "2")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void abstractedReturnDeltaBlockingCallerInterest() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", prependField("h"), "2")).retSite("c", kill("1")),
				normalStmt("c", flow("2", readField("f"), "3")).succ("d"),
				normalStmt("d").succ("e"));
		
		helper.method("bar",
				startPoints("f"),
				exitStmt("f").returns(over("b"), to("c"), flow("2", "2")));
		
		helper.runSolver(false, "a");		
	}
	
	@Test
	public void abstractedReturnResolveThroughDelta() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b1"),
				callSite("b1").calls("xyz", flow("1", prependField("f"), "1")));
				
		helper.method("xyz",
				startPoints("b2"),
				callSite("b2").calls("bar", flow("1", prependField("h"), "2")).retSite("c", kill("1")),
				normalStmt("c", flow("2", readField("h"), "3")).succ("d"),
				normalStmt("d", kill("3")).succ("e"));
		
		helper.method("bar",
				startPoints("f"),
				exitStmt("f").returns(over("b2"), to("c"), flow("2", "2")));
		
		helper.runSolver(false, "a");		
	}
	
	@Test
	public void unbalancedAbstractedReturnRecursive() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("cs1").succ("b"),
				exitStmt("b").returns(over("cs1"), to("b"), flow(2, "1", "1"))
							 .returns(over("cs2"), to("c"), flow(2, "1", "1")));
		
		helper.method("foo",
				startPoints("unused"),
				normalStmt("cs2").succ("c"),
				normalStmt("c", flow("1", readField("f"), "2")).succ("d"),
				normalStmt("d", kill("2")).succ("e"));
		
		helper.runSolver(true, "a");
	}
	
	@Test
	public void includeResolversInCallDeltas() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")).retSite("e", kill("1")),
				callSite("e").calls("xyz", flow("3", "3")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", overwriteField("f"), "2")).succ("d"),
				exitStmt("d").returns(over("b"), to("e"), flow("2", "3")));
		
		helper.method("xyz", 
				startPoints("f"),
				normalStmt("f", flow("3", readField("f"), "4")).succ("g"),
				normalStmt("g").succ("h"));
				
		helper.runSolver(false, "a");
	}
	
	@Test
	public void includeResolversInCallDeltas2() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")).retSite("e", kill("1")),
				callSite("e").calls("xyz", flow("3", "3")).retSite("g", kill("3")),
				normalStmt("g", flow("3", readField("f"), "4")).succ("h"),
				normalStmt("h").succ("i"));

		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", overwriteField("f"), "2")).succ("d"),
				exitStmt("d").returns(over("b"), to("e"), flow("2", "3")));
		
		helper.method("xyz", 
				startPoints("f"),
				exitStmt("f").returns(over("e"), to("g"), flow("3", "3")));
				
		helper.runSolver(false, "a");
	}
	
	@Test
	public void includeResolversInCallDeltas3() {
		helper.method("main",
				startPoints("m_a"),
				normalStmt("m_a", flow("0", "1")).succ("m_b"),
				callSite("m_b").calls("foo", flow("1", prependField("g"), "1")).retSite("m_c", kill("1")),
				callSite("m_c").calls("foo", flow("5", prependField("f"), "1")).retSite("m_d", kill("5")),
				normalStmt("m_d", kill("6")).succ("m_e"));
		
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("1", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")).retSite("e", kill("1")),
				callSite("e").calls("xyz", flow("3", "3")).retSite("g", kill("3")),
				normalStmt("g", flow("3", readField("f"), "4"), flow("3", readField("g"), "5")).succ("h"),
				exitStmt("h").returns(over("m_c"), to("m_d"), flow("4", "6")).returns(over("m_b"), to("m_c"), flow("5", "5")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", overwriteField("f"), "2"), flow("2", overwriteField("g"), "2")).succ("d"),
				exitStmt("d").returns(over("b"), to("e"), flow(2, "2", "3")));
		
		helper.method("xyz", 
				startPoints("f"),
				exitStmt("f").returns(over("e"), to("g"), flow("3", "3")));
				
		helper.runSolver(false, "m_a");
	}
	
	@Test
	public void includeResolversInCallDeltas4() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")).retSite("e1", kill("1")),
				normalStmt("e1", flow("3", prependField("g"), "3")).succ("e2"),
				callSite("e2").calls("xyz", flow("3", "3")).retSite("g", kill("3")),
				normalStmt("g", flow("3", readField("h"), "4")).succ("h"),
				normalStmt("h", flow("4", readField("g"), "5")).succ("i"),
				normalStmt("i", flow("5", readField("f"), "6")).succ("j"));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", prependField("f"), "2")).succ("d"),
				exitStmt("d").returns(over("b"), to("e1"), flow("2", "3")));
		
		helper.method("xyz", 
				startPoints("f1"),
				normalStmt("f1", flow("3", prependField("h"), "3")).succ("f2"),
				exitStmt("f2").returns(over("e2"), to("g"), flow("3", "3")));
				
		helper.runSolver(false, "a");
	}
	
	@Test
	public void includeResolversInCallDeltas5() {
		helper.method("foo", 
				startPoints("a"),
				normalStmt("a", flow("0", prependField("z"), "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")).retSite("l", kill("1")),
				normalStmt("l").succ("m"));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", prependField("f"), "2")).succ("d"),
				callSite("d").calls("xyz", flow("2", "3")).retSite("i", kill("2")),
				normalStmt("i", flow("4", readField("g"), "5")).succ("j"),
				normalStmt("j", flow("5", readField("f"), "6")).succ("k"),
				exitStmt("k").returns(over("b"), to("l"), kill("6")));
		
		helper.method("xyz",
				startPoints("e"),
				callSite("e").calls("baz", flow("3", "3")).retSite("h", kill("3")),
				exitStmt("h").returns(over("d"), to("i"), flow("4", "4")));
		
		helper.method("baz",
				startPoints("f"),
				normalStmt("f", flow("3", prependField("g"), "4")).succ("g"),
				exitStmt("g").returns(over("e"), to("h"), flow("4", "4")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void includeResolversInCallDeltas6() {
		helper.method("foo", 
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("a2"),
				callSite("a2").calls("xyz", flow("1", "3")).retSite("b", kill("1")),
				callSite("b").calls("bar", flow("1", "2")).retSite("l", kill("1")),
				normalStmt("l").succ("m"));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("2", prependField("z"), "2")).succ("d"),
				callSite("d").calls("xyz", flow("2", "3")).retSite("i", kill("2")),
				normalStmt("i", flow("4", readField("g"), "5")).succ("j"),
				normalStmt("j", flow("5", readField("z"), "6")).succ("k"),
				exitStmt("k").returns(over("b"), to("l"), kill("6")));
		
		helper.method("xyz",
				startPoints("e"),
				callSite("e").calls("baz", flow("3", "3")).retSite("h", kill("3")),
				exitStmt("h").returns(over("d"), to("i"), flow("4", "4"))
							 .returns(over("a2"), to("b"), flow("4", "1")));
		
		helper.method("baz",
				startPoints("f"),
				normalStmt("f", flow("3", prependField("g"), "4")).succ("g"),
				exitStmt("g").returns(over("e"), to("h"), flow("4", "4")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveCallReturnCase() {
		helper.method("xyz",
				startPoints("x"),
				normalStmt("x", flow("0", "1")).succ("y"),
				callSite("y").calls("foo", flow("1", prependField("g"), "1")));
		
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("1", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")).retSite("c", kill("1")),
				callSite("c").calls("bar", flow("2", "2")));
		
		helper.method("bar", 
				startPoints("d"),
				normalStmt("d", flow("2", "2")).succ("d1").succ("d2"),
				normalStmt("d1", flow("2", readField("f"), "3")).succ("e"),
				normalStmt("d2", flow("2", "2")).succ("f"),
				exitStmt("f").returns(over("b"), to("c"), flow("2", "2")));
		
		helper.runSolver(false, "x");
	}
	
	@Test
	public void recursivelyUseIncompatibleReturnResolver() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "1")).retSite("f", kill("1")),
				normalStmt("f", flow("2", readField("f"), "3")).succ("g"),
				normalStmt("h").succ("i"));
		
		helper.method("bar",
				startPoints("c"),
				callSite("c").calls("xyz", flow("1", "1")).retSite("d", kill("1")),
				normalStmt("d", flow("1", overwriteField("f"), "1")).succ("e"),
				exitStmt("e").returns(over("b"), to("f"), flow("1", "2")));
		
		helper.method("xyz",
				startPoints("x"),
				exitStmt("x").returns(over("c"), to("d"), flow("1", "1")));
		
		helper.runSolver(false, "a");
	}

	@Test
	public void unbalancedUseIncompatibleReturnResolver() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				exitStmt("b").returns(over("cs"), to("c"), flow("1", "1")));
		
		helper.method("bar",
				startPoints("unused1"),
				normalStmt("cs").succ("c"),
				normalStmt("c", flow("1", readField("g"), "1")).succ("d"),
				normalStmt("d", flow("1", overwriteField("f"), "2")).succ("e"),
				exitStmt("e").returns(over("cs2"), to("f"), flow("2", "2")));
		
		helper.method("xyz",
				startPoints("unused2"),
				normalStmt("cs2").succ("f"),
				normalStmt("f", flow("2", readField("f"), "3")).succ("g"),
				normalStmt("g").succ("h"));
				
		helper.runSolver(true, "a");
	}

	@Test
	public void recursiveReadField() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "1")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("1", readField("f"), "1")).succ("d"),
				callSite("d").calls("bar", flow("1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveReadField2() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "1")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("1", readField("f"), "1")).succ("d"),
				callSite("d").calls("xyz", flow("1", "1")));
		
		helper.method("xyz", 
				startPoints("e"),
				callSite("e").calls("bar", flow("1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveReadField3() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "1")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("1", readField("f"), "1")).succ("d"),
				callSite("d").calls("xyz", flow("1", "1")));
		
		helper.method("xyz", 
				startPoints("e"),
				normalStmt("e", flow("1", readField("g"), "1")).succ("f"),
				callSite("f").calls("bar", flow("1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveReadField4() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b1"),
				callSite("b1").calls("bar", flow("1", "1")).retSite("b2", flow("1", "1")),
				callSite("b2").calls("xyz", flow("1", "1")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c", flow("1", readField("f"), "1")).succ("d"),
				callSite("d").calls("xyz", flow("1", "1")));
		
		helper.method("xyz", 
				startPoints("e"),
				normalStmt("e", flow("1", readField("g"), "1")).succ("f"),
				callSite("f").calls("bar", flow("1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void loopReadingField() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", readField("f"), "1")).succ("b").succ("c"),
				normalStmt("c", kill("1")).succ("d"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void loopReadingField2() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", "1")).succ("c1").succ("c2"),
				normalStmt("c1", flow("1", readField("f"), "1")).succ("b").succ("d"),
				normalStmt("c2", flow("1", readField("g"), "1")).succ("b").succ("d"),
				normalStmt("d", kill(2, "1")).succ("e"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void loopReadingField3() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", "1")).succ("c1").succ("c2"),
				normalStmt("c1", flow("1", readField("f"), "1")).succ("d"),
				normalStmt("c2", flow("1", readField("g"), "1")).succ("d"),
				normalStmt("d", flow("1", "1")).succ("e").succ("b"),
				normalStmt("e", kill("1")).succ("f"));
		
		helper.runSolver(false, "a");
	}

	@Test
	public void recursiveReturn() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")));
		
		helper.method("bar",
				startPoints("c"),
				callSite("c").calls("bar", flow("2", "2")).retSite("d", flow("2", "2")),
				normalStmt("d", flow(2, "2", "2")).succ("e").succ("f"),
				exitStmt("e").returns(over("c"), to("d"), flow(2, "2", "2")),
				normalStmt("f", flow(2, "2", readField("f"), "3")).succ("g"),
				normalStmt("g", kill(2, "3")).succ("h"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void joinStmtIsCallSite() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", kill(2,"1")).retSite("c", flow("1", "1")),
				normalStmt("c", flow("1", prependField("f"), "1")).succ("b"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void readMultipleAbstractedFields() {
		helper.method("foo",
			startPoints("a"),
			normalStmt("a", flow("0", prependField("f"), "1")).succ("b1").succ("b2"),
			normalStmt("b1", kill("1")).succ("c"),
			normalStmt("b2", flow("1", prependField("g"), "1")).succ("c"),
			normalStmt("c", flow("1", "2")).succ("d"),
			normalStmt("d", flow("2", "2")).succ("e1").succ("e2"),
			normalStmt("e1", flow("2", "3")).succ("f"),
			normalStmt("e2", kill("2")).succ("f"),
			normalStmt("f", flow("3", readField("g"), "4")).succ("g"),
			normalStmt("g", flow("4", readField("f"), "5")).succ("h"),
			normalStmt("h", kill("5")).succ("i"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void intraproceduralStateExplosionInline0Resolver() {
		helper.method("main",
				startPoints("m_a"),
				normalStmt("m_a", flow("0", "1")).succ("m_b"),
				callSite("m_b").calls("foo", flow("1", "1")));
		
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("1", "1")).succ("b"),
				normalStmt("b", flow("1", "1")).succ("c1").succ("c2"),
				normalStmt("c1", flow("1", "1")).succ("d"),
				normalStmt("c2", flow("1", overwriteField("f"), "1")).succ("d"),
				normalStmt("d", flow(2, "1", "1")).succ("e1").succ("e2"),
				normalStmt("e1", flow(2, "1", "1")).succ("f"),
				normalStmt("e2", flow(2, "1", overwriteField("g"), "1")).succ("f"),
				normalStmt("f", flow(4, "1", "1")).succ("g1").succ("g2"),
				normalStmt("g1", flow(4, "1", "1")).succ("h"),
				normalStmt("g2", flow(4, "1", overwriteField("h"), "1")).succ("h"),
				normalStmt("h", flow(8, "1", "1")).succ("i"));
		
		helper.runSolver(false, "m_a");
	}
	
	@Test
	public void intraproceduralStateExplosion() {
		helper.method("main",
				startPoints("m_a"),
				normalStmt("m_a", flow("0", "1")).succ("m_b"),
				callSite("m_b").calls("foo", flow("1", prependField("x"), "1")));
		
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("1", "1")).succ("b"),
				normalStmt("b", flow("1", "1")).succ("c1").succ("c2"),
				normalStmt("c1", flow("1", "1")).succ("d"),
				normalStmt("c2", flow("1", overwriteField("f"), "1")).succ("d"),
				normalStmt("d", flow(2, "1", "1")).succ("e1").succ("e2"),
				normalStmt("e1", flow(2, "1", "1")).succ("f"),
				normalStmt("e2", flow(2, "1", overwriteField("g"), "1")).succ("f"),
				normalStmt("f", flow(4, "1", "1")).succ("g1").succ("g2"),
				normalStmt("g1", flow(4, "1", "1")).succ("h"),
				normalStmt("g2", flow(4, "1", overwriteField("h"), "1")).succ("h"),
				normalStmt("h", flow(8, "1", "1")).succ("i"));
		
		helper.runSolver(false, "m_a");
	}
	
	@Test
	public void nestedResolversShouldFormAGraph() {
		helper.method("main",
				startPoints("m_a"),
				normalStmt("m_a", flow("0", "1")).succ("m_b"),
				callSite("m_b").calls("foo", flow("1", prependField("f"), "1")));
		
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("1", "1")).succ("b1").succ("b2"),
				normalStmt("b1", flow("1", overwriteField("x"), "1")).succ("c"),
				normalStmt("b2", flow("1", overwriteField("y"), "1")).succ("c"),
				normalStmt("c", flow(2, "1", "1")).succ("d1").succ("d2"),
				normalStmt("d1", flow(2, "1", overwriteField("x"), "1")).succ("e"),
				normalStmt("d2", flow(2, "1", overwriteField("y"), "1")).succ("e"),
				normalStmt("e", flow(3, "1", "1")).succ("f"));
		
		helper.runSolver(false, "m_a");
	}
	
	@Test
	public void maintainCallingContextInCtrlFlowJoin() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b1").succ("b2"),
				callSite("b1").calls("foo", flow("1", "2")).retSite("c1", kill("1")),
				callSite("b2").calls("foo", flow("1", prependField("f"), "2")).retSite("c2", kill("1")),
				normalStmt("c1", kill("3")).succ("d"),
				normalStmt("c2", flow("3", readField("f"), "4")).succ("d"),
				normalStmt("d", kill("4")).succ("e"));
				
		helper.method("foo", 
				startPoints("f"),
				normalStmt("f", flow("2", "2")).succ("g").succ("f"),
				exitStmt("g").returns(over("b1"), to("c1"), flow("2", "3")).returns(over("b2"), to("c2"), flow("2", "3")));
				
		helper.runSolver(false, "a");
	}
	
	@Test
	public void readPrefixInLoopToResolveFieldRead() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", prependField("f"), "1")).succ("c"),
				normalStmt("c", flow("1", prependField("a"), "1")).succ("d"),
				normalStmt("d", flow("1", prependField("a"), "1")).succ("e"),
				normalStmt("e", flow("1",  "1")).succ("f").succ("g"),
				normalStmt("f", flow("1", readField("a"), "1")).succ("e"),
				normalStmt("g", flow("1", readField("f"), "2")).succ("h"),
				normalStmt("h", kill("2")).succ("i"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void loopReadAfterRecursiveReturnWrite() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("g"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("f", kill("1")),
				normalStmt("f", flow("1", "1")).succ("g").succ("h"),
				normalStmt("g", flow("1", readField("f"), "1")).succ("f"),
				
				normalStmt("h", flow("1", readField("f"), "1")).succ("i"),
				normalStmt("i", flow("1", readField("f"), "1")).succ("j"),
				normalStmt("j", flow("1", readField("g"), "1")).succ("k"),
				normalStmt("k", kill("1")).succ("l"));
		
		helper.method("foo",
				startPoints("c"),
				normalStmt("c", flow("1", "1")).succ("d"),
				callSite("d").calls("foo", flow("1", "1")).retSite("e", flow("1", "1")),
				normalStmt("e", flow(2, "1", prependField("f"), "1")).succ("ep"),
				exitStmt("ep").returns(over("d"), to("e"), flow(2, "1", "1")).returns(over("b"), to("f"), flow(2, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void loopReadAfterRecursiveCallWrite() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", overwriteField("f"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("f", kill("1")),
				normalStmt("f", flow("1", "1")).succ("g").succ("h"),
				normalStmt("g", flow("1", readField("f"), "1")).succ("f"),
				
				normalStmt("h", flow("1", readField("f"), "1")).succ("i"),
				normalStmt("i", flow("1", readField("f"), "1")).succ("j"),
				normalStmt("j", flow("1", readField("f"), "1")).succ("k"),
				normalStmt("k", kill("1")).succ("l"));
		
		helper.method("foo",
				startPoints("c"),
				normalStmt("c", flow("1", prependField("f"), "1")).succ("d"),
				callSite("d").calls("foo", flow("1", "1")).retSite("e", flow("1", "1")),
				exitStmt("e").returns(over("d"), to("e"), flow(2, "1", "1")).returns(over("b"), to("f"), flow(2, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void loopReadAfterReturnFromLoopWrite() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("f", kill("1")),
				normalStmt("f", flow("1", "1")).succ("g").succ("h"),
				normalStmt("g", flow("1", readField("f"), "1")).succ("f"),
				normalStmt("h", kill("1")).succ("i"));
		
		helper.method("foo",
				startPoints("c"),
				normalStmt("c", flow("1", "1")).succ("d").succ("e"),
				normalStmt("d", flow("1", prependField("f"), "1")).succ("c"),
				exitStmt("e").returns(over("b"), to("f"), flow("1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void loopAndReadAfterCalls() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("g"), "1")).succ("b"),
				normalStmt("b", flow("1", "1")).succ("c").succ("d"),
				normalStmt("c", flow("1", prependField("f"), "1")).succ("b"),
				callSite("d").calls("foo", flow("1", "1")));
		
		helper.method("foo",
				startPoints("e"),
				callSite("e").calls("bar", flow("1", "1")));
				
		helper.method("bar",
				startPoints("f"),
				normalStmt("f", flow("1", "1")).succ("g").succ("h"),
				normalStmt("g", flow("1", readField("f"), "1")).succ("f"),
				normalStmt("h", flow("1", readField("f"), "1")).succ("i"),
				normalStmt("i", flow("1", readField("g"), "1")).succ("j"),
				normalStmt("j", kill("1")).succ("k"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void loopAndReadMultipleFieldsAfterCalls() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("b"),
				normalStmt("b", flow("1", "1")).succ("c1").succ("d"),
				normalStmt("c1", flow("1", prependField("b"), "1")).succ("c2"),
				normalStmt("c2", flow("1", prependField("a"), "1")).succ("b"),
				callSite("d").calls("foo", flow("1", "1")));
		
		helper.method("foo",
				startPoints("e"),
				callSite("e").calls("bar", flow("1", "1")));
				
		helper.method("bar",
				startPoints("f"),
				normalStmt("f", flow("1", "1")).succ("g1").succ("h"),
				normalStmt("g1", flow("1", readField("a"), "1")).succ("g2"),
				normalStmt("g2", flow("1", readField("b"), "1")).succ("f"),
				
				normalStmt("h", flow("1", readField("a"), "1")).succ("i"),
				normalStmt("i", flow("1", readField("b"), "1")).succ("j"),
				normalStmt("j", flow("1", readField("a"), "1")).succ("k"),
				normalStmt("k", flow( "1", readField("b"), "1")).succ("l"),
				normalStmt("l", flow("1", readField("f"), "1")).succ("m"),
				normalStmt("m", kill("1")).succ("n"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void resolveViaReturnAndConsiderCallEdgeConstraints() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", overwriteField("f"), "1")).succ("b0"),
				normalStmt("b0", flow("1", overwriteField("g"), "1")).succ("b1"),
				normalStmt("b1", flow("1", prependField("f"), "2")).succ("b2"),
				normalStmt("b2", flow("2", prependField("f"), "2")).succ("c"),
				callSite("c").calls("foo", flow("2", "3")).retSite("h", kill("2")),
				normalStmt("h", flow("4", readField("g"), "5")).succ("i"),
				normalStmt("i", flow("5", readField("g"), "5")).succ("j"),
				normalStmt("j", kill(0, "5")).succ("k")); //cannot read 2 g, because summary not generated
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d", flow("3", readField("f"), "3")).succ("e"),
				normalStmt("e", flow("3", "3")).succ("ep").succ("f"),
				callSite("f").calls("foo", flow("3", "3")).retSite("g", kill("3")),
				normalStmt("g", flow("3", prependField("g"), "3")).succ("ep"),
				exitStmt("ep").returns(over("c"), to("h"), flow(2, "3", "4")).returns(over("f"), to("g"), flow("3", "3")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void resolveViaReturnAndConsiderCallEdgeConstraints2() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", overwriteField("f"), "1")).succ("b0"),
				normalStmt("b0", flow("1", overwriteField("g"), "1")).succ("b1"),
				normalStmt("b1", flow("1", prependField("f"), "1")).succ("b2"),
				normalStmt("b2", flow("1", prependField("f"), "2")).succ("b3"),
				normalStmt("b3", flow("2", prependField("f"), "2")).succ("c"),
				callSite("c").calls("foo", flow("2", "3")).retSite("h", kill("2")),
				normalStmt("h", flow("4", readField("g"), "5")).succ("i"),
				normalStmt("i", flow("5", readField("g"), "5")).succ("j"),
				normalStmt("j", flow("5", readField("g"), "5")).succ("k"),
				normalStmt("k", kill("5")).succ("l")); //can read 3 g because of over-approximation
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d", flow("3", readField("f"), "3")).succ("e"),
				normalStmt("e", flow("3", "3")).succ("ep").succ("f"),
				callSite("f").calls("foo", flow("3", "3")).retSite("g", kill("3")),
				normalStmt("g", flow("3", prependField("g"), "3")).succ("ep"),
				exitStmt("ep").returns(over("c"), to("h"), flow(2, "3", "4")).returns(over("f"), to("g"), flow(2, "3", "3")));
		
		helper.runSolver(false, "a");
	}
	
	public void recursiveReturnWriteBeforeRecursiveReturnRead() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("c", kill("1")),
				callSite("c").calls("bar", flow("1", "1")).retSite("d", kill("1")),
				normalStmt("d", kill("1")).succ("e"));
		
		helper.method("foo",
				startPoints("foo_sp"),
				normalStmt("foo_sp", flow("1", "1")).succ("foo_ep").succ("foo_cs"),
				callSite("foo_cs").calls("foo", flow("1", "1")).retSite("foo_rs", kill("1")),
				normalStmt("foo_rs", flow("1", prependField("f"), "1")).succ("foo_ep"),
				exitStmt("foo_ep").returns(over("foo_cs"), to("foo_rs"), flow("1", "1")).
									returns(over("b"), to("c"), flow("1", "1")));
		
		helper.method("bar",
				startPoints("bar_sp"),
				normalStmt("bar_sp", flow("1", "1")).succ("bar_ep").succ("bar_cs"),
				callSite("bar_cs").calls("bar", flow("1", "1")).retSite("bar_rs", kill("1")),
				normalStmt("bar_rs", flow("1", readField("f"), "1")).succ("bar_ep"),
				exitStmt("bar_ep").returns(over("bar_cs"), to("bar_rs"), flow("1", "1")).
				returns(over("c"), to("d"), flow("1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void intraproceduralWriteAndReadInLoops() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", "1")).succ("c1").succ("c2").succ("d"),
				normalStmt("c1", flow("1", prependField("f"), "1")).succ("b"),
				normalStmt("c2", flow("1", prependField("g"), "1")).succ("b"),
				normalStmt("d", flow("1", "1")).succ("e1").succ("e2").succ("f"),
				normalStmt("e1", flow("1", readField("g"), "1")).succ("d"),
				normalStmt("e2", flow("1", readField("f"), "1")).succ("d"),
				normalStmt("f", flow("1", readField("g"), "1")).succ("g"),
				normalStmt("g", flow("1", readField("f"), "1")).succ("h"),
				normalStmt("h", kill("1")).succ("i"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void intraproceduralWriteAndReadMultipleFieldsSubsequentlyInLoops() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", overwriteField("g"), "1")).succ("b"),
				normalStmt("b", flow("1", "1")).succ("c1").succ("d"),
				normalStmt("c1", flow("1", prependField("f"), "1")).succ("c2"),
				normalStmt("c2", flow("1", prependField("g"), "1")).succ("b"),
				normalStmt("d", flow("1", "1")).succ("e1").succ("f"),
				normalStmt("e1", flow("1", readField("g"), "1")).succ("e2"),
				normalStmt("e2", flow("1", readField("f"), "1")).succ("d"),
				normalStmt("f", flow("1", readField("g"), "1")).succ("g"),
				normalStmt("g", flow("1", readField("f"), "1")).succ("h"),
				normalStmt("h", kill("1")).succ("i"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void loopWriteThenCallAndLoopRead() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("g"), "1")).succ("js1"),
				normalStmt("js1", flow("1", "1")).succ("b").succ("c"),
				normalStmt("b", flow("1", prependField("f"), "1")).succ("js1"),
				callSite("c").calls("foo", flow("1", "1")));
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d", flow("1", "1")).succ("js2"),
				normalStmt("js2", flow("1", "1")).succ("e").succ("f"),
				normalStmt("e", flow("1", readField("f"), "1")).succ("js2"),
				normalStmt("f", flow("1", readField("f"), "1")).succ("g"),
				normalStmt("g", flow("1", readField("g"), "1")).succ("h"),
				normalStmt("h", kill("1")).succ("i"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void callAndLoopRead() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("g"), "1")).succ("b"),
				normalStmt("b", flow("1", prependField("f"), "1")).succ("c"),
				callSite("c").calls("foo", flow("1", prependField("f"), "1")));
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d", flow("1", "1")).succ("js2"),
				normalStmt("js2", flow("1", "1")).succ("e").succ("f"),
				normalStmt("e", flow("1", readField("f"), "1")).succ("js2"),
				normalStmt("f", flow("1", readField("f"), "1")).succ("g"),
				normalStmt("g", flow("1", readField("g"), "1")).succ("h"),
				normalStmt("h", kill("1")).succ("i"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveWriteBeforeAndAfterCall() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", overwriteField("f"), "1")).succ("b"),
				normalStmt("b", flow("1", overwriteField("g"), "1")).succ("c"),
				callSite("c").calls("foo", flow("1", "1")).retSite("f", kill("1")),
				normalStmt("f", flow("1", readField("g"), "1")).succ("g").succ("js"),
				normalStmt("g", flow(2, "1", readField("f"), "1")).succ("h"),
				normalStmt("h", flow(2, "1", readField("f"), "1")).succ("i"),
				normalStmt("i", kill(2, "1")).succ("j"),
				normalStmt("js", flow("1", "1")).succ("js_inner1").succ("js_inner2").succ("g"),
				normalStmt("js_inner1", flow("1", readField("f"), "1")).succ("js"),
				normalStmt("js_inner2", flow("1", readField("g"), "1")).succ("js"));
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d", flow("1", "1")).succ("ep").succ("e"),
				normalStmt("e", flow("1", prependField("f"), "1")).succ("cs"),
				callSite("cs").calls("foo", flow("1", "1")).retSite("rs", kill("1")),
				normalStmt("rs", flow("1", prependField("g"), "1")).succ("ep"),
				exitStmt("ep").returns(over("cs"), to("rs"), flow(2, "1", "1")).returns(over("c"), to("f"), flow(2, "1", "1")));
				
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveWriteBeforeAndAfterCallIncludingAbstractionPoints() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", overwriteField("f"), "1")).succ("b"),
				normalStmt("b", flow("1", overwriteField("g"), "1")).succ("c"),
				callSite("c").calls("foo", flow("1", "1")).retSite("f", kill("1")),
				normalStmt("f", flow("1", readField("g"), "1")).succ("g1").succ("js"),
				normalStmt("g1", flow("1", readField("g"), "1")).succ("g2"),
				normalStmt("g2", flow("1", readField("f"), "1")).succ("h"),
				normalStmt("h", flow("1", readField("f"), "1")).succ("i"),
				normalStmt("i", kill("1")).succ("j"),
				normalStmt("js", flow("1", "1")).succ("js_inner1").succ("js_inner2").succ("g"),
				normalStmt("js_inner1", flow("1", readField("f"), "1")).succ("js"),
				normalStmt("js_inner2", flow("1", readField("g"), "1")).succ("js"));
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d", flow("1", "1")).succ("ep").succ("e"),
				normalStmt("e", flow("1", prependField("f"), "1")).succ("ap1"),
				normalStmt("ap1", flow("1", "1")).succ("ap1").succ("cs"),				
				callSite("cs").calls("foo", flow("1", "1")).retSite("rs", kill("1")),
				normalStmt("rs", flow("1", prependField("g"), "1")).succ("ap2"),
				normalStmt("ap2", flow("1", "1")).succ("ap2").succ("ep"),
				exitStmt("ep").returns(over("cs"), to("rs"), flow(2, "1", "1")).returns(over("c"), to("f"), flow(2, "1", "1")));
				
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveCallReadAndWrite() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", overwriteField("f"), "1")).succ("c"),
				callSite("c").calls("foo", flow("1", "1")).retSite("g", kill("1")),
				normalStmt("g", kill("1")).succ("h"));
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d", flow("1", "1")).succ("e").succ("f"),
				normalStmt("e", flow("1", readField("f"), "1")).succ("f2"),
				normalStmt("f", flow("1", prependField("f"), "1")).succ("f2"),
				normalStmt("f2", flow(2, "1", "1")).succ("cs").succ("ep"),
				callSite("cs").calls("foo", flow(2, "1", "1")).retSite("rs", kill(2, "1")),
				normalStmt("rs", flow("1", "1")).succ("ep"),
				exitStmt("ep").returns(over("cs"), to("rs"), flow(6, "1", "1")).returns(over("c"), to("g"), flow(2, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test 
	public void mutualRecursiveCallReadAndWrite() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("h"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("g", kill("1")));
		
		helper.method("foo",
				startPoints("c"),
				normalStmt("c", flow("1", "1")).succ("d").succ("cs1").succ("ep1"),
				normalStmt("d", flow("1", prependField("x"), "1")).succ("cs1").succ("ep1"),
				callSite("cs1").calls("foo", flow(2, "1", "1")).calls("bar", flow(2, "1", "1")).retSite("rs1", kill(2, "1")),
				normalStmt("rs1", flow("1", "1")).succ("ep1"),
				exitStmt("ep1").returns(over("b"), to("g"), flow(3, "1", "1"))
								.returns(over("cs1"), to("rs1"), flow(6, "1", "1"))
								.returns(over("cs2"), to("rs2"), flow(6, "1", "1")));
		
		helper.method("bar",
				startPoints("e"),
				normalStmt("e", flow("1", "1")).succ("f").succ("cs2").succ("ep2"),
				normalStmt("f", flow("1", readField("x"), "1")).succ("cs2").succ("ep2"),
				callSite("cs2").calls("foo", flow(2, "1", "1")).calls("bar", flow(2, "1", "1")).retSite("rs2", kill(2, "1")),
				normalStmt("rs2", flow("1", "1")).succ("ep2"),
				exitStmt("ep2").returns(over("cs1"), to("rs1"), flow(6, "1", "1"))
								.returns(over("cs2"), to("rs2"), flow(6, "1", "1")));
	
		helper.runSolver(false, "a");
	}
	
	@Test 
	public void mutualRecursiveCallReadAndWriteMultipleFields() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				normalStmt("b", flow("1", prependField("h"), "1")).succ("c"),
				callSite("c").calls("foo1", flow("1", "1")).retSite("g", kill("1")),
				normalStmt("g", kill("1")).succ("end"));
		
		helper.method("foo1",
				startPoints("d"),
				normalStmt("d", flow("1", "1")).succ("e").succ("f").succ("f2"),
				normalStmt("e", flow("1", readField("f"), "1")).succ("f").succ("f2"),
				normalStmt("f", flow(2, "1", prependField("f"), "1")).succ("f2"),
				normalStmt("f2", flow(4, "1", "1")).succ("cs").succ("ep"),
				callSite("cs").calls("foo1", flow(4, "1", "1")).calls("foo2", flow(4, "1", "1")).retSite("rs", kill(4, "1")),
				normalStmt("rs", flow("1", "1")).succ("ep"),
				exitStmt("ep").returns(over("cs"), to("rs"), flow(20, "1", "1"))
							.returns(over("cs2"), to("rs2"), flow(16, "1", "1"))
							.returns(over("c"), to("g"), flow(3, "1", "1")));
		
		helper.method("foo2",
				startPoints("h"),
				normalStmt("h", flow("1", "1")).succ("i").succ("j").succ("k"),
				normalStmt("i", flow("1", readField("g"), "1")).succ("j").succ("k"),
				normalStmt("j", flow(2, "1", prependField("g"), "1")).succ("k"),
				normalStmt("k", flow(4, "1", "1")).succ("cs2").succ("ep2"),
				callSite("cs2").calls("foo1", flow(4, "1", "1")).calls("foo2", flow(4, "1", "1")).retSite("rs2", kill(4, "1")),
				normalStmt("rs2", flow("1", "1")).succ("ep2"),
				exitStmt("ep2").returns(over("cs"), to("rs"), flow(16, "1", "1"))
							.returns(over("cs2"), to("rs2"), flow(20, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void mutualRecursiveReturnReadAndWrite() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("h"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("g", kill("1")),
				normalStmt("g", flow("1", readField("x"), "1")).succ("h"),
				normalStmt("h", flow("1", readField("x"), "1")).succ("i"),
				normalStmt("i", flow("1", readField("h"), "1")).succ("j"),
				normalStmt("j", kill("1")).succ("k"));
		
		helper.method("foo",
				startPoints("c"),
				normalStmt("c", flow("1", "1")).succ("cs1").succ("ep1"),
				callSite("cs1").calls("foo", flow("1", "1")).calls("bar", flow("1", "1")).retSite("rs1", kill("1")),
				normalStmt("rs1", flow("1", "1")).succ("d").succ("ep1"),
				normalStmt("d", flow("1", prependField("x"), "1")).succ("ep1"),
				exitStmt("ep1").returns(over("b"), to("g"), flow(3, "1", "1"))
								.returns(over("cs1"), to("rs1"), flow(3, "1", "1"))
								.returns(over("cs2"), to("rs2"), flow(3, "1", "1")));

		helper.method("bar",
				startPoints("e"),
				normalStmt("e", flow("1", "1")).succ("cs2").succ("ep2"),
				callSite("cs2").calls("foo", flow("1", "1")).calls("bar", flow("1", "1")).retSite("rs2", kill("1")),
				normalStmt("rs2", flow("1", "1")).succ("f").succ("ep2"),
				normalStmt("f", flow("1", readField("x"), "1")).succ("ep2"),
				exitStmt("ep2").returns(over("cs1"), to("rs1"), flow(3, "1", "1"))
								.returns(over("cs2"), to("rs2"), flow(3, "1", "1")));
	
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveTest() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("h"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("c", kill("1")),
				callSite("c").calls("bar", flow("1", "1")).retSite("d", kill("1")),
				normalStmt("d", flow("1", readField("h"), "1")).succ("e"),
				normalStmt("e", kill("1")).succ("f"));
		
		helper.method("foo",
				startPoints("foo_sp"),
				normalStmt("foo_sp", flow("1", "1")).succ("foo_read").succ("foo_write").succ("foo_cs").succ("foo_ep"),
				normalStmt("foo_read", flow("1", readField("f"), "1")).succ("foo_write").succ("foo_cs").succ("foo_ep"),
				normalStmt("foo_write", flow(2, "1", prependField("f"), "1")).succ("foo_cs").succ("foo_ep"),
				callSite("foo_cs").calls("foo", flow(4, "1", "1")).retSite("foo_rs", kill(4, "1")),
				normalStmt("foo_rs", flow("1", "1")).succ("foo_ep"),
				exitStmt("foo_ep").returns(over("b"), to("c"), flow(3, "1", "1"))
								.returns(over("bar_cs"), to("bar_rs"), flow(5, "1", "1"))
								.returns(over("foo_cs"), to("foo_rs"), flow(20, "1", "1")));
		
		helper.method("bar",
				startPoints("bar_sp"),
				normalStmt("bar_sp", flow("1", "1")).succ("bar_cs").succ("bar_ep"),
				callSite("bar_cs").calls("foo", flow("1", "1")).retSite("bar_rs", kill("1")),
				normalStmt("bar_rs", flow("1", "1")).succ("bar_ep"),
				exitStmt("bar_ep").returns(over("c"), to("d"), flow(2, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveTest2() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("h"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("c", kill("1")),
				callSite("c").calls("foo", flow("1", "1")).retSite("d", kill("1")),
				normalStmt("d", flow("1", readField("h"), "1")).succ("e"),
				normalStmt("e", kill("1")).succ("f"));
		
		helper.method("foo",
				startPoints("foo_sp"),
				normalStmt("foo_sp", flow("1", "1")).succ("foo_read").succ("foo_write").succ("foo_cs").succ("foo_ep"),
				normalStmt("foo_read", flow("1", readField("f"), "1")).succ("foo_write").succ("foo_cs").succ("foo_ep"),
				normalStmt("foo_write", flow(2, "1", prependField("f"), "1")).succ("foo_cs").succ("foo_ep"),
				callSite("foo_cs").calls("bar", flow(4, "1", "1")).retSite("foo_rs", kill(4, "1")),
				normalStmt("foo_rs", flow("1", "1")).succ("foo_ep"),
				exitStmt("foo_ep").returns(over("b"), to("c"), flow(3, "1", "1"))
								.returns(over("c"), to("d"), flow(5, "1", "1"))
								.returns(over("bar_cs"), to("bar_rs"), flow(5, "1", "1"))
								.returns(over("foo_cs"), to("foo_rs"), flow(0, "1", "1")));
		
		helper.method("bar",
				startPoints("bar_sp"),
				normalStmt("bar_sp", flow("1", "1")).succ("bar_cs").succ("bar_ep"),
				callSite("bar_cs").calls("foo", flow("1", "1")).retSite("bar_rs", kill("1")),
				normalStmt("bar_rs", flow("1", "1")).succ("bar_ep"),
				exitStmt("bar_ep").returns(over("bar_cs"), to("bar_rs"), flow(0, "1", "1"))
								.returns(over("foo_cs"), to("foo_rs"), flow(8, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test 
	public void recursiveTest3() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("h"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("c", kill("1")),
				callSite("c").calls("foo", flow("1", "1")).retSite("d", kill("1")),
				normalStmt("d", flow("1", readField("h"), "1")).succ("e"),
				normalStmt("e", kill("1")).succ("f"));
		
		helper.method("foo",
				startPoints("foo_sp"),
				normalStmt("foo_sp", flow("1", "1")).succ("foo_read").succ("foo_write").succ("foo_cs").succ("foo_ep"),
				normalStmt("foo_read", flow("1", readField("f"), "1")).succ("foo_write").succ("foo_cs").succ("foo_ep"),
				normalStmt("foo_write", flow(2, "1", prependField("f"), "1")).succ("foo_cs").succ("foo_ep"),
				callSite("foo_cs").calls("foo", flow(4, "1", "1")).calls("bar", flow(4, "1", "1")).retSite("foo_rs", kill(4, "1")),
				normalStmt("foo_rs", flow("1", "1")).succ("foo_ep"),
				exitStmt("foo_ep").returns(over("b"), to("c"), flow(3, "1", "1"))
								.returns(over("c"), to("d"), flow(5, "1", "1"))
								.returns(over("bar_cs"), to("bar_rs"), flow(5, "1", "1"))
								.returns(over("foo_cs"), to("foo_rs"), flow(20, "1", "1")));
		
		helper.method("bar",
				startPoints("bar_sp"),
				normalStmt("bar_sp", flow("1", "1")).succ("bar_cs").succ("bar_ep"),
				callSite("bar_cs").calls("foo", flow("1", "1")).calls("bar", flow("1", "1")).retSite("bar_rs", kill("1")),
				normalStmt("bar_rs", flow("1", "1")).succ("bar_ep"),
				exitStmt("bar_ep").returns(over("bar_cs"), to("bar_rs"), flow(2, "1", "1"))
								.returns(over("foo_cs"), to("foo_rs"), flow(8, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveTest4() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("h"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("c", flow("1", "1")),
				callSite("c").calls("foo", flow(2, "1", "1")).retSite("d", flow(2, "1", "1")),
				normalStmt("d", flow(3, "1", readField("h"), "1")).succ("e"),
				normalStmt("e", kill(3, "1")).succ("f"));
		
		helper.method("foo",
				startPoints("foo_sp"),
				normalStmt("foo_sp", flow("1", "1")).succ("foo_read").succ("foo_write").succ("foo_cs").succ("foo_ep"),
				normalStmt("foo_read", flow("1", readField("f"), "1")).succ("foo_write").succ("foo_cs").succ("foo_ep"),
				normalStmt("foo_write", flow(2, "1", prependField("f"), "1")).succ("foo_cs").succ("foo_ep"),
				callSite("foo_cs").calls("foo", flow(4, "1", "1")).calls("bar", flow(4, "1", "1")).retSite("foo_rs", kill(4, "1")),
				normalStmt("foo_rs", flow("1", "1")).succ("foo_ep"),
				exitStmt("foo_ep").returns(over("b"), to("c"), flow(3, "1", "1"))
								.returns(over("c"), to("d"), flow(8, "1", "1"))
								.returns(over("bar_cs"), to("bar_rs"), flow(16, "1", "1"))
								.returns(over("foo_cs"), to("foo_rs"), flow(20, "1", "1")));

		helper.method("bar",
				startPoints("bar_sp"),
				normalStmt("bar_sp", flow("1", "1")).succ("bar_read").succ("bar_write").succ("bar_cs").succ("bar_ep"),
				normalStmt("bar_read", flow("1", readField("g"), "1")).succ("bar_write").succ("bar_cs").succ("bar_ep"),
				normalStmt("bar_write", flow(2, "1", prependField("g"), "1")).succ("bar_cs").succ("bar_ep"),
				callSite("bar_cs").calls("foo", flow(4, "1", "1")).calls("bar", flow(4, "1", "1")).retSite("bar_rs", kill(4, "1")),
				normalStmt("bar_rs", flow("1", "1")).succ("bar_ep"),
				exitStmt("bar_ep")
								.returns(over("bar_cs"), to("bar_rs"), flow(20, "1", "1"))
								.returns(over("foo_cs"), to("foo_rs"), flow(16, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveTest5() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("h"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("c", flow("1", "1")),
				normalStmt("c", flow("1", "1")).succ("d").succ("e").succ("f"),
				normalStmt("d", flow("1", readField("f"), "1")).succ("c"),
				normalStmt("e", flow("1", readField("g"), "1")).succ("c"),
				normalStmt("f", flow("1", readField("h"), "1")).succ("g"),
				normalStmt("g", kill("1")).succ("h"));
		
		helper.method("foo",
				startPoints("foo_sp"),
				normalStmt("foo_sp", flow("1", "1")).succ("foo_read").succ("foo_write").succ("foo_cs").succ("foo_ep"),
				normalStmt("foo_read", flow("1", readField("f"), "1")).succ("foo_write").succ("foo_cs").succ("foo_ep"),
				normalStmt("foo_write", flow(2, "1", prependField("f"), "1")).succ("foo_cs").succ("foo_ep"),
				callSite("foo_cs").calls("foo", flow(4, "1", "1")).calls("bar", flow(4, "1", "1")).retSite("foo_rs", kill(4, "1")),
				normalStmt("foo_rs", flow("1", "1")).succ("foo_ep"),
				exitStmt("foo_ep").returns(over("b"), to("c"), flow(3, "1", "1"))
								.returns(over("bar_cs"), to("bar_rs"), flow(16, "1", "1"))
								.returns(over("foo_cs"), to("foo_rs"), flow(20, "1", "1")));

		helper.method("bar",
				startPoints("bar_sp"),
				normalStmt("bar_sp", flow("1", "1")).succ("bar_read").succ("bar_write").succ("bar_cs").succ("bar_ep"),
				normalStmt("bar_read", flow("1", readField("g"), "1")).succ("bar_write").succ("bar_cs").succ("bar_ep"),
				normalStmt("bar_write", flow(2, "1", prependField("g"), "1")).succ("bar_cs").succ("bar_ep"),
				callSite("bar_cs").calls("foo", flow(4, "1", "1")).calls("bar", flow(4, "1", "1")).retSite("bar_rs", kill(4, "1")),
				normalStmt("bar_rs", flow(1, "1", "1")).succ("bar_ep"),
				exitStmt("bar_ep").returns(over("b"), to("c"), flow(0, "1", "1"))
								.returns(over("bar_cs"), to("bar_rs"), flow(20, "1", "1"))
								.returns(over("foo_cs"), to("foo_rs"), flow(16, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void writeFieldInLoopBeforeRecursiveCallRead() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("g"), "1")).succ("js"),
				normalStmt("js", flow("1", "1")).succ("inner").succ("main_cs"),
				normalStmt("inner", flow("1", prependField("f"), "1")).succ("js"),
				callSite("main_cs").calls("foo", flow("1", "1")).retSite("main_rs", kill("1")),
				normalStmt("main_rs", flow("1", readField("h"), "1")).succ("main_read"),
				normalStmt("main_read", flow("1", readField("g"), "1")).succ("main_ep"),
				normalStmt("main_ep", kill("1")).succ("main_null"));
		
		helper.method("foo",
				startPoints("b"),
				normalStmt("b", flow("1", readField("f"), "1")).succ("c").succ("d"),
				callSite("c").calls("foo", flow("1", "1")).retSite("d", kill("1")),
				exitStmt("d").returns(over("c"), to("d"), flow(2, "1", prependField("h"), "1"))
							.returns(over("main_cs"), to("main_rs"), flow(2, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test 
	public void twoWritingFieldLoopsBeforeRecursiveCallRead() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b").succ("c"),
				callSite("b").calls("mainA", flow("1", "1")).retSite("d", kill("1")),
				callSite("c").calls("mainB", flow("1", "1")).retSite("d", kill("1")),
				normalStmt("d").succ("e"));
		
		helper.method("mainA",
				startPoints("mainA_init"),
				normalStmt("mainA_init", flow("1", prependField("a"), "1")).succ("mainA_js"),
				normalStmt("mainA_js", flow("1", "1")).succ("mainA_cs").succ("mainA_inner"),
				normalStmt("mainA_inner", flow("1", prependField("f"), "1")).succ("mainA_js"),
				callSite("mainA_cs").calls("fooA", flow("1", "1")).calls("fooB", flow("1", "1")).retSite("mainA_rs", kill("1")),
				normalStmt("mainA_rs", flow("1", readField("a"), "1")).succ("mainA_ep"),
				normalStmt("mainA_ep", kill("1")).succ("mainA_null"));
		
		helper.method("mainB",
				startPoints("mainB_init"),
				normalStmt("mainB_init", flow("1", prependField("b"), "1")).succ("mainB_js"),
				normalStmt("mainB_js", flow("1", "1")).succ("mainB_cs").succ("mainB_inner"),
				normalStmt("mainB_inner", flow("1", prependField("f"), "1")).succ("mainB_js"),
				callSite("mainB_cs").calls("fooA", flow("1", "1")).calls("fooB", flow("1", "1")).retSite("mainB_rs", kill("1")),
				normalStmt("mainB_rs", flow("1", readField("a"), "1")).succ("mainB_ep"),
				normalStmt("mainB_ep", kill(0, "1")).succ("mainB_null"));
		
		helper.method("fooA",
				startPoints("fooA_sp"),
				callSite("fooA_sp").calls("bar", flow("1", "1")).retSite("fooA_rs", kill("1")),
				exitStmt("fooA_rs").returns(over("mainA_cs"), to("mainA_rs"), flow("1", "1"))
							.returns(over("mainB_cs"), to("mainB_rs"), flow("1", "1")));
		
		helper.method("fooB",
				startPoints("fooB_sp"),
				callSite("fooB_sp").calls("bar", flow("1", "1")).retSite("fooB_rs", kill("1")),
				exitStmt("fooB_rs").returns(over("mainA_cs"), to("mainA_rs"), flow("1", "1"))
							.returns(over("mainB_cs"), to("mainB_rs"), flow("1", "1")));
		
		helper.method("bar",
				startPoints("bar_sp"),
				normalStmt("bar_sp", flow("1", readField("f"), "1")).succ("bar_cs").succ("bar_rs"),
				callSite("bar_cs").calls("bar", flow("1", "1")).retSite("bar_rs", flow("1", "1")),
				exitStmt("bar_rs").returns(over("fooA_sp"), to("fooA_rs"), flow(2, "1", "1"))
							.returns(over("fooB_sp"), to("fooB_rs"), flow(2, "1", "1"))
							.returns(over("bar_cs"), to("bar_rs"), flow(2, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void concretizeOnOverwrittenField() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("h"), "1")).succ("b"),
				normalStmt("b", flow("1", prependField("f"), "1")).succ("b").succ("c"),
				callSite("c").calls("foo", flow("1", "1")));
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d", flow("1", readField("f"), "1")).succ("e"),
				normalStmt("e", flow("1", overwriteField("h"), "1")).succ("f"),
				callSite("f").calls("foo", flow("1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void concretizeOnOverwrittenField2() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("y"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")));
		
		helper.method("foo",
				startPoints("c"),
				normalStmt("c", flow("1", "1")).succ("d1").succ("d2"),
				normalStmt("d1", flow("1", prependField("x"), "1")).succ("e"),
				normalStmt("d2", flow("1", overwriteField("y"), "1")).succ("e"),
				callSite("e").calls("foo", flow(2, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void concretizeOnOverwrittenField3() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("main_ap"),
				normalStmt("main_ap", flow("1", "1")).succ("main_loop").succ("main_b1"),
				normalStmt("main_loop", flow("1", prependField("x"), "1")).succ("main_ap"),
				normalStmt("main_b1", flow("1", prependField("x"), "1")).succ("main_b2"),
				normalStmt("main_b2", flow("1", prependField("x"), "1")).succ("main_cs"),
				callSite("main_cs").calls("foo", flow("1", "1")));
		
		helper.method("foo",
				startPoints("c"),
				normalStmt("c", flow("1", "1")).succ("d"),
				normalStmt("d", flow("1", overwriteField("y"), "1")).succ("e"),
				normalStmt("e", flow("1", readField("x"), "1")).succ("f"),
				callSite("f").calls("foo", flow("1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void concretizeOnOverwrittenField4() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("y"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")));
		
		helper.method("foo",
				startPoints("c"),
				normalStmt("c", flow("1", "1")).succ("d1").succ("d2"),
				normalStmt("d1", flow("1", overwriteField("x"), "1")).succ("e"),
				normalStmt("d2", flow("1", prependField("y"), "1")).succ("d3"),
				normalStmt("d3", flow("1", overwriteField("z"), "1")).succ("e"),
				callSite("e").calls("foo", flow(2, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void twoLoopsWritingSameFieldThenRecursiveRead() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("loop1").succ("loop2"),
				normalStmt("loop1", flow("1", prependField("f"), "1")).succ("loop1").succ("cs"),
				normalStmt("loop2", flow("1", prependField("f"), "1")).succ("loop2").succ("cs"),
				callSite("cs").calls("foo", flow(2, "1", "1")));
		
		helper.method("foo",
				startPoints("b"),
				normalStmt("b", flow("1", readField("f"), "1")).succ("c"),
				callSite("c").calls("foo", flow("1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void twoLoopsWritingSameFieldThenLoopRead() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("g"), "1")).succ("loop1").succ("loop2"),
				normalStmt("loop1", flow("1", prependField("f"), "1")).succ("loop1").succ("cs"),
				normalStmt("loop2", flow("1", prependField("f"), "1")).succ("loop2").succ("cs"),
				callSite("cs").calls("foo", flow(2, "1", "1")));
		
		helper.method("foo",
				startPoints("b"),
				normalStmt("b", flow("1", readField("f"), "1")).succ("b"));
		
		helper.runSolver(false, "a");
	}

	@Test
	public void analyzerIndependenceOfCtrlFlowJoins() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("z"), "1")).succ("b"),
				normalStmt("b", flow("1", prependField("f"), "1")).succ("c"),
				callSite("c").calls("foo", flow("1", "1")));
		
		helper.method("foo", 
				startPoints("d"),
				normalStmt("d", flow("1", "1")).succ("e1").succ("js"),
				normalStmt("e1", flow("1", readField("f"), "1")).succ("e2"),
				normalStmt("e2", flow("1", prependField("f"), "1")).succ("js"),
				normalStmt("js", flow("1", "1")).succ("js").succ("f"),
				normalStmt("f", flow("1", overwriteField("f"), "1")).succ("g"),
				normalStmt("g", flow(0, "1", readField("z"), "1")).succ("h"),
				normalStmt("h").succ("i"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void analyzerIndependenceOfReturnSiteHandling() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("z"), "1")).succ("b"),
				normalStmt("b", flow("1", prependField("f"), "1")).succ("c"),
				callSite("c").calls("foo", flow("1", "1")).retSite("main_rs", kill("1")),
				normalStmt("main_rs").succ("nop"));
		
		helper.method("foo", 
				startPoints("d"),
				normalStmt("d", flow("1", "1")).succ("cs").succ("e"),
				callSite("cs").calls("foo", flow("1", "1")).retSite("rs", kill("1")),
				normalStmt("rs", flow("1", overwriteField("f"), "1")).succ("e"),
				normalStmt("e", flow("1", "1")).succ("f1").succ("g"),
				normalStmt("f1", flow("1", readField("f"), "1")).succ("f2"),
				normalStmt("f2", flow("1", prependField("f"), "1")).succ("g"),
				exitStmt("g").returns(over("cs"), to("rs"), flow(2, "1", "1")).returns(over("c"), to("main_rs"), kill(2, "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void selfSatisfyingLoop() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("f"), "1")).succ("b"),
				normalStmt("b", flow("1", readField("f"), "1")).succ("c"),
				normalStmt("c", flow("1", prependField("f"), "1")).succ("d"),
				normalStmt("d", flow("1", prependField("f"), "1")).succ("b"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveCallWrite() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("g"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("f", kill("1")),
				normalStmt("f", flow("1", readField("f"), "1")).succ("g"),
				normalStmt("g", flow("1", readField("f"), "1")).succ("h"),
				normalStmt("h", flow("1", readField("f"), "1")).succ("i"),
				normalStmt("i", kill("1")).succ("j"));
		
		helper.method("foo",
				startPoints("c"),
				normalStmt("c", flow("1", "1")).succ("d"),
				callSite("d").calls("foo", flow("1", prependField("f"), "1")).retSite("e", flow("1", "1")),
				exitStmt("e").returns(over("d"), to("e"), flow(2, "1", "1")).returns(over("b"), to("f"), flow(2, "1", "1")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void recursiveCallWriteAndRecursiveReadSameField() {
		helper.method("main",
				startPoints("a"),
				normalStmt("a", flow("0", prependField("g"), "1")).succ("b"),
				callSite("b").calls("foo", flow("1", "1")).retSite("d", kill("1")),
				normalStmt("d", flow("1", readField("f"), "1")).succ("e"),
				normalStmt("e", flow("1", readField("f"), "1")).succ("f"),
				normalStmt("f", flow("1", readField("f"), "1")).succ("g"),
				normalStmt("g", flow("1", readField("f"), "1")).succ("h"),
				normalStmt("h", flow("1", readField("f"), "1")).succ("i"),
				normalStmt("i", flow("1", readField("f"), "1")).succ("j"),
				normalStmt("j", flow("1", readField("f"), "1")).succ("k"),
				normalStmt("k", flow("1", readField("f"), "1")).succ("l"),
				normalStmt("l", flow("1", readField("f"), "1")).succ("m"),
				normalStmt("m", flow("1", readField("f"), "1")).succ("n"),
				normalStmt("n", flow("1", readField("f"), "1")).succ("o"),
				normalStmt("o", flow("1", readField("f"), "1")).succ("p"),
				normalStmt("p", flow("1", readField("f"), "1")).succ("q"),
				normalStmt("q", flow("1", readField("f"), "1")).succ("r"),
				normalStmt("r", kill("1")).succ("s"));
		
		helper.method("foo",
				startPoints("c"),
				normalStmt("c", flow("1", prependField("f"), "1")).succ("ep").succ("cs"),
				callSite("cs").calls("foo", flow("1", prependField("f"), "1")).retSite("rs", kill("1")),
				normalStmt("rs", flow("1", readField("f"), "1")).succ("ep"),
				exitStmt("ep").returns(over("cs"), to("rs"), flow(2, "1", "1")).returns(over("b"), to("d"), flow(2, "1", "1")));
		
		helper.runSolver(false, "a");
	}
}
