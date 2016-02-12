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
package heros.cfl.solver;

public interface EntityNameGenerator<Field, Fact, Stmt, Method> {

	String returnSite(Stmt returnSite, Fact fact);

	String joinStmt(Stmt joinStmt, Fact fact);
	
	String startPoint(Method method, Fact sourceFact);
}
