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
package heros.fieldsens;

import heros.fieldsens.FlowFunction.ConstrainedFact;
import heros.fieldsens.FlowFunction.ReadFieldConstraint;
import heros.fieldsens.FlowFunction.WriteFieldConstraint;
import heros.fieldsens.structs.AccessPathAndResolver;
import heros.fieldsens.structs.WrappedFact;

public class AccessPathHandler<Field, Fact, Stmt, Method> {

	private Debugger<Field, Fact, Stmt, Method> debugger;
	private AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver;

	public AccessPathHandler(AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver, Debugger<Field, Fact, Stmt, Method> debugger) {
		this.accPathResolver = accPathResolver;
		this.debugger = debugger;
	}

	public boolean canRead(Field field) {
		return accPathResolver.accessPath.canRead(field);
	}
	
	public boolean mayCanRead(Field field) {
		return accPathResolver.accessPath.canRead(field) || (accPathResolver.accessPath.hasEmptyAccessPath() && !accPathResolver.accessPath.isAccessInExclusions(field));
	}
	
	public boolean mayBeEmpty() {
		return accPathResolver.accessPath.hasEmptyAccessPath();
	}

	public ConstrainedFact<Field, Fact, Stmt, Method> generate(Fact fact) {
		return new ConstrainedFact<Field, Fact, Stmt, Method>(new WrappedFact<Field, Fact, Stmt, Method>(fact, accPathResolver));
	}
	
	public ConstrainedFact<Field, Fact, Stmt, Method> generateWithEmptyAccessPath(Fact fact, ZeroHandler<Field> zeroHandler) {
		return new ConstrainedFact<Field, Fact, Stmt, Method>(new WrappedFact<Field, Fact, Stmt, Method>(fact, 
				new AccessPathAndResolver<Field, Fact, Stmt, Method>(accPathResolver.getAnalyzer(), new AccessPath<Field>(), new ZeroCallEdgeResolver<Field, Fact, Stmt, Method>(
						accPathResolver.getAnalyzer(), zeroHandler, debugger))));
	}
	
	public ResultBuilder<Field, Fact, Stmt, Method> prepend(final Field field) {
		return new ResultBuilder<Field, Fact, Stmt, Method>() {
			@Override
			public ConstrainedFact<Field, Fact, Stmt, Method> generate(Fact fact) {
				return new ConstrainedFact<Field, Fact, Stmt, Method>(new WrappedFact<Field, Fact, Stmt, Method>(fact, 
						accPathResolver.withAccessPath(accPathResolver.accessPath.prepend(field))));
			}
		};
	}
	
	public ResultBuilder<Field, Fact, Stmt, Method> read(final Field field) {
		if(mayCanRead(field)) {
			return new ResultBuilder<Field, Fact, Stmt, Method>() {
				@Override
				public ConstrainedFact<Field, Fact, Stmt, Method> generate(Fact fact) {
					if(canRead(field))
						return new ConstrainedFact<Field, Fact, Stmt, Method>(new WrappedFact<Field, Fact, Stmt, Method>(fact, 
								accPathResolver.withAccessPath(accPathResolver.accessPath.removeFirst())));
					else
						return new ConstrainedFact<Field, Fact, Stmt, Method>(new WrappedFact<Field, Fact, Stmt, Method>(fact, 
								accPathResolver.withAccessPath(new AccessPath<Field>())), new ReadFieldConstraint<Field>(field));
				}
			};
		}
		else
			throw new IllegalArgumentException("Cannot read field "+field);
	}
	
	public ResultBuilder<Field, Fact, Stmt, Method> overwrite(final Field field) {
		if(mayBeEmpty())
			return new ResultBuilder<Field, Fact, Stmt, Method>() {
				@SuppressWarnings("unchecked")
				@Override
				public ConstrainedFact<Field, Fact, Stmt, Method> generate(Fact fact) {
					if(accPathResolver.accessPath.isAccessInExclusions(field))
						return new ConstrainedFact<Field, Fact, Stmt, Method>(new WrappedFact<Field, Fact, Stmt, Method>(fact, accPathResolver));
					else
						return new ConstrainedFact<Field, Fact, Stmt, Method>(new WrappedFact<Field, Fact, Stmt, Method>(fact, 
								accPathResolver.withAccessPath(accPathResolver.accessPath.appendExcludedFieldReference(field))), 
								new WriteFieldConstraint<Field>(field));
				}
			};
		else
			throw new IllegalArgumentException("Cannot write field "+field);
	}
	
	public static interface ResultBuilder<FieldRef, FactAbstraction, Stmt, Method> {
		public ConstrainedFact<FieldRef, FactAbstraction, Stmt, Method> generate(FactAbstraction fact);
	}

}
