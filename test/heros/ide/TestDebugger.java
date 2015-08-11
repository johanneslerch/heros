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
package heros.ide;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import com.google.common.collect.Sets;

import heros.InterproceduralCFG;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.structs.WrappedFactAtStatement;
import heros.utilities.JsonDocument;

public class TestDebugger<Fact, Stmt, Method, Value> implements Debugger<Fact, Stmt, Method, Value> {

	private JsonDocument root = new JsonDocument();
	private InterproceduralCFG<Stmt, Method> icfg;
	
	public void writeJsonDebugFile(String filename) {
		try {
			FileWriter writer = new FileWriter(filename);
			StringBuilder builder = new StringBuilder();
			builder.append("var root=");
			root.write(builder, 0);
			writer.write(builder.toString());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see heros.alias.Debugger#setICFG(I)
	 */
	@Override
	public void setICFG(InterproceduralCFG<Stmt, Method> icfg) {
		this.icfg = icfg;
	}

	/* (non-Javadoc)
	 * @see heros.alias.Debugger#initialSeed(Stmt)
	 */
	@Override
	public void initialSeed(Stmt stmt) {
		stmt(stmt).keyValue("seed", "true");
		
		includeSuccessors(stmt, Sets.<Stmt> newHashSet());
	}
	
	private void includeSuccessors(Stmt stmt, Set<Stmt> visited) {
		if(!visited.add(stmt))
			return;
		
		JsonDocument doc = stmt(stmt);
		for(Stmt succ : icfg.getSuccsOf(stmt)) {
			doc.array("successors").add(succ.toString());
			stmt(succ);
			includeSuccessors(succ, visited);
		}
		
		if(icfg.isCallStmt(stmt)) {
			for(Method m : icfg.getCalleesOfCallAt(stmt)) {
				doc.doc("calls").doc(m.toString());
				for(Stmt sp : icfg.getStartPointsOf(m)) {
					stmt(sp).keyValue("startPoint", "true");
					includeSuccessors(sp, visited);
				}
			}
			for(Stmt retSite :icfg.getReturnSitesOfCallAt(stmt)) {
				doc.array("successors").add(retSite.toString());
				stmt(retSite);
				includeSuccessors(retSite, visited);
			}
		}
		if(icfg.isExitStmt(stmt)) {
			for(Stmt callSite : icfg.getCallersOf(icfg.getMethodOf(stmt))) {
				for(Stmt retSite : icfg.getReturnSitesOfCallAt(callSite)) {
					doc.doc("returns").doc(retSite.toString());
					includeSuccessors(retSite, visited);
				}
			}
		}
	}

	protected JsonDocument stmt(Stmt stmt) {
		Method methodOf = icfg.getMethodOf(stmt);
		return root.doc("methods").doc(methodOf.toString()).doc(stmt.toString());
	}
	
	public void expectNormalFlow(Stmt unit, String expectedFlowFunctionsToString) {
		stmt(unit).keyValue("flow", expectedFlowFunctionsToString);
	}

	public void expectCallFlow(Stmt callSite, Method destinationMethod, String expectedFlowFunctionsToString) {
		stmt(callSite).doc("calls").doc(destinationMethod.toString()).keyValue("flow", expectedFlowFunctionsToString);
	}

	public void expectReturnFlow(Stmt exitStmt, Stmt returnSite, String expectedFlowFunctionsToString) {
		if(returnSite != null)
			stmt(exitStmt).doc("returns").doc(returnSite.toString()).keyValue("flow", expectedFlowFunctionsToString);
	}

	@Override
	public void edgeTo(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt) {
	}

	@Override
	public void newResolver(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Resolver<Fact, Stmt, Method, Value> resolver) {
	}

	@Override
	public void newJob(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt) {
	}

	@Override
	public void jobStarted(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer,
			WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt) {
	}

	@Override
	public void jobFinished(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer,
			WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt) {
	}

	@Override
	public void askedToResolve(Resolver<Fact, Stmt, Method, Value> resolver, EdgeFunction<Value> constraint) {
	}

}
