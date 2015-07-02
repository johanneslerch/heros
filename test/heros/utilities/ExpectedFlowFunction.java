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
package heros.utilities;

import heros.fieldsens.AccessPathHandler;
import heros.fieldsens.FlowFunction;
import heros.ide.edgefunc.EdgeFunction;
import heros.ide.edgefunc.fieldsens.AccessPathBundle;

import com.google.common.base.Joiner;

public abstract class ExpectedFlowFunction<Fact> {

	public final Fact source;
	public final Fact[] targets;
	public Edge edge;
	int times;
	public final EdgeFunction<AccessPathBundle<String>> edgeFunction;

	public ExpectedFlowFunction(int times, Fact source, Fact... targets) {
		this(times, null, source, targets);
	}
	public ExpectedFlowFunction(int times, EdgeFunction<AccessPathBundle<String>> edgeFunction, Fact source, Fact... targets) {
		this.times = times;
		this.edgeFunction = edgeFunction;
		this.source = source;
		this.targets = targets;
	}

	@Override
	public String toString() {
		return String.format("%s: %s -> {%s}", edge, source, Joiner.on(",").join(targets));
	}
	
	public abstract String transformerString();

	public abstract FlowFunction.ConstrainedFact<String, TestFact, Statement, TestMethod> apply(TestFact target, AccessPathHandler<String, TestFact, Statement, TestMethod> accPathHandler);
	
}