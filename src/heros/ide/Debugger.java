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

import heros.InterproceduralCFG;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.structs.WrappedFactAtStatement;

public interface Debugger<Fact, Stmt, Method, Value> {

	public void setICFG(InterproceduralCFG<Stmt, Method> icfg);
	public void initialSeed(Stmt stmt);
	public void edgeTo(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt);
	public void newResolver(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Resolver<Fact, Stmt, Method, Value> resolver);
	public void newJob(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt);
	public void jobStarted(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt);
	public void jobFinished(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt);
	public void askedToResolve(Resolver<Fact, Stmt, Method, Value> resolver, EdgeFunction<Value> constraint);
	
	public static class NullDebugger <Fact, Stmt, Method, Value> implements Debugger<Fact, Stmt, Method, Value> {

		@Override
		public void setICFG(InterproceduralCFG<Stmt, Method> icfg) {
			
		}

		@Override
		public void initialSeed(Stmt stmt) {
			
		}

		@Override
		public void edgeTo(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer,
				WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt) {
			
		}

		@Override
		public void newResolver(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer, Resolver<Fact, Stmt, Method, Value> resolver) {
			
		}

		@Override
		public void newJob(PerAccessPathMethodAnalyzer<Fact, Stmt, Method, Value> analyzer,
				WrappedFactAtStatement<Fact, Stmt, Method, Value> factAtStmt) {
			
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
}