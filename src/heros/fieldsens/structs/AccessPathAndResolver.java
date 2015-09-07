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
package heros.fieldsens.structs;

import heros.fieldsens.AccessPath;
import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.AccessPath.PrefixTestResult;
import heros.fieldsens.FlowFunction.Constraint;
import heros.fieldsens.CallEdgeResolver;
import heros.fieldsens.InterestCallback;
import heros.fieldsens.PerAccessPathMethodAnalyzer;
import heros.fieldsens.Resolver;
import heros.fieldsens.ZeroCallEdgeResolver;

public class AccessPathAndResolver<Field, Fact, Stmt, Method> {

	public final AccessPath<Field> accessPath;
	public final Resolver<Field, Fact, Stmt, Method> resolver;
	private AccessPathAndResolver<Field, Fact, Stmt, Method> nesting;

	public AccessPathAndResolver(AccessPath<Field> accessPath, Resolver<Field, Fact, Stmt, Method> resolver) {
		this.accessPath = accessPath;
		this.resolver = resolver;
		this.nesting = null;
	}

	private AccessPathAndResolver(AccessPath<Field> accessPath, Resolver<Field, Fact, Stmt, Method> resolver,
			AccessPathAndResolver<Field, Fact, Stmt, Method> nesting) {
		this.accessPath = accessPath;
		this.resolver = resolver;
		this.nesting = nesting;
	}

	public AccessPathAndResolver<Field, Fact, Stmt, Method> withAccessPath(AccessPath<Field> accPath) {
		if(accPath == this.accessPath)
			return this;
		else
			return new AccessPathAndResolver<Field, Fact, Stmt, Method>(accPath, resolver, nesting);
	}

	public AccessPathAndResolver<Field, Fact, Stmt, Method> wrapWith(AccessPath<Field> accPath, Resolver<Field, Fact, Stmt, Method> resolver) {
		return new AccessPathAndResolver<Field, Fact, Stmt, Method>(accPath, resolver, this);
	}
	
	public void resolve(final Constraint<Field> constraint, final InterestCallback<Field, Fact, Stmt, Method> callback) {
		if (isNullOrCallEdgeResolver(resolver)) {
			resolveViaNesting(constraint, callback);
		} else {
			resolver.resolve(constraint, new InterestCallback<Field, Fact, Stmt, Method>() {

				@Override
				public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
						AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
					callback.interest(analyzer, accPathResolver.appendToLast(nesting));
				}

				@Override
				public void canBeResolvedEmpty() {
					resolveViaNesting(constraint, callback);
				}
			});
		}
	}

	private void resolveViaNesting(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback) {
		if(nesting == null)
			callback.canBeResolvedEmpty();
		else {
			AccessPath<Field> toBeResolved = constraint.applyToAccessPath(AccessPath.<Field> empty());
			if (toBeResolved.isPrefixOf(nesting.accessPath) == PrefixTestResult.GUARANTEED_PREFIX) {
				AccessPath<Field> remainingAccPath = toBeResolved.getDeltaToAsAccessPath(nesting.accessPath);
				callback.interest(nesting.getLast().resolver.getAnalyzer(), nesting.withAccessPath(remainingAccPath));
			} else if (nesting.accessPath.isPrefixOf(toBeResolved).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
				Delta<Field> delta = nesting.accessPath.getDeltaTo(toBeResolved);
				nesting.resolve(new DeltaConstraint<Field>(delta), callback);
			}
		}
	}

	private boolean isNullOrCallEdgeResolver(Resolver<Field, Fact, Stmt, Method> resolver) {
		if (resolver == null)
			return true;
		if (resolver instanceof CallEdgeResolver) {
			return !(resolver instanceof ZeroCallEdgeResolver);
		}
		return false;
	}

	public AccessPathAndResolver<Field, Fact, Stmt, Method> getLast() {
		if (nesting == null)
			return this;
		else
			return nesting.getLast();
	}

	public AccessPathAndResolver<Field, Fact, Stmt, Method> appendToLast(AccessPathAndResolver<Field, Fact, Stmt, Method> nesting) {
		if (nesting == null)
			return this;

		if (this.nesting == null)
			return new AccessPathAndResolver<Field, Fact, Stmt, Method>(accessPath, resolver, nesting);
		else
			return new AccessPathAndResolver<Field, Fact, Stmt, Method>(accessPath, resolver, this.nesting.appendToLast(nesting));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessPath == null) ? 0 : accessPath.hashCode());
		result = prime * result + ((nesting == null) ? 0 : nesting.hashCode());
		result = prime * result + ((resolver == null) ? 0 : resolver.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AccessPathAndResolver other = (AccessPathAndResolver) obj;
		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.equals(other.accessPath))
			return false;
		if (nesting == null) {
			if (other.nesting != null)
				return false;
		} else if (!nesting.equals(other.nesting))
			return false;
		if (resolver == null) {
			if (other.resolver != null)
				return false;
		} else if (!resolver.equals(other.resolver))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String result = accessPath.toString() + resolver;
		if(nesting != null)
			result+= " :: "+nesting;
		return result;
	}
}
