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

import com.google.common.base.Predicate;

import heros.fieldsens.AccessPath;
import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.AccessPath.PrefixTestResult;
import heros.fieldsens.FlowFunction.Constraint;
import heros.fieldsens.CallEdgeResolver;
import heros.fieldsens.InterestCallback;
import heros.fieldsens.PerAccessPathMethodAnalyzer;
import heros.fieldsens.Resolver;
import heros.fieldsens.ReturnSiteHandling;
import heros.fieldsens.ZeroCallEdgeResolver;

public class AccessPathAndResolver<Field, Fact, Stmt, Method> {

	public final AccessPath<Field> accessPath;
	public final Resolver<Field, Fact, Stmt, Method> resolver;
	private AccessPathAndResolver<Field, Fact, Stmt, Method> nesting;
	private PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer;

	public AccessPathAndResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, AccessPath<Field> accessPath, Resolver<Field, Fact, Stmt, Method> resolver) {
		this(analyzer, accessPath, resolver, null);
	}

	private AccessPathAndResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, AccessPath<Field> accessPath, Resolver<Field, Fact, Stmt, Method> resolver,
			AccessPathAndResolver<Field, Fact, Stmt, Method> nesting) {
		this.analyzer = analyzer;
		this.accessPath = accessPath;
		this.resolver = resolver;
		this.nesting = nesting;
		
		assert depth() <= 3;
	}
	
	private int depth() {
		if(nesting == null)
			return 1;
		else
			return 1+nesting.depth();
	}

	public AccessPathAndResolver<Field, Fact, Stmt, Method> withAccessPath(AccessPath<Field> accPath) {
		if(accPath == this.accessPath)
			return this;
		else
			return new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, accPath, resolver, nesting);
	}

	public PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer() {
		return getLast().analyzer;
	}
	
	public void resolve(final Constraint<Field> constraint, final InterestCallback<Field, Fact, Stmt, Method> callback) {
		if (isNullOrCallEdgeResolver(resolver)) {
			resolveViaNesting(constraint, callback);
		} else {
			resolver.resolve(constraint, new InterestCallback<Field, Fact, Stmt, Method>() {
				@Override
				public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
						AccessPathAndResolver<Field, Fact, Stmt, Method> accPathResolver) {
					AccessPathAndResolver<Field, Fact, Stmt, Method> appended = accPathResolver.appendToLast(nesting);
					callback.interest(appended.getAnalyzer(), appended);
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
				callback.interest(nesting.getAnalyzer(), nesting.withAccessPath(remainingAccPath));
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
	
	public AccessPathAndResolver<Field, Fact, Stmt, Method> removeLast() {
		if(nesting == null)
			throw new IllegalStateException();
		if(nesting.nesting == null)
			return new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, accessPath, resolver);
		else
			return new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, accessPath, resolver, nesting.removeLast());
	}

	public AccessPathAndResolver<Field, Fact, Stmt, Method> appendToLast(AccessPathAndResolver<Field, Fact, Stmt, Method> nesting) {
		if (nesting == null)
			return this;

		if(this.resolver instanceof ZeroCallEdgeResolver) {
			//FIXME zero analyzer requires to have the same incoming edges as nesting.getAnalyzer()
			PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> zeroAnalyzer = nesting.getAnalyzer().createWithZeroCallEdgeResolver();
			return new AccessPathAndResolver<Field, Fact, Stmt, Method>(zeroAnalyzer, accessPath, zeroAnalyzer.getCallEdgeResolver());
		} else if (this.nesting == null) {
			//do not reduce to correctly identify transitive resolvers
			/*if(isNullOrCallEdgeResolver(resolver))
				return nesting.withAccessPath(accessPath.append(nesting.accessPath));
			else*/ if(resolver instanceof ReturnSiteHandling.CallSiteResolver && resolver==nesting.resolver)
				return appendToLast(nesting.nesting);
			else
				return new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, accessPath, resolver, nesting);
		}
		else {
			 /*if(isNullOrCallEdgeResolver(resolver))
				return this.nesting.withAccessPath(accessPath.append(this.nesting.accessPath)).appendToLast(nesting);
			else*/
				return new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, accessPath, resolver, this.nesting.appendToLast(nesting));
		}
	}

	public boolean exists(Predicate<AccessPathAndResolver<Field, Fact, Stmt, Method>> predicate) {
		if(predicate.apply(this))
			return true;
		else if(nesting == null)
			return false;
		else
			return nesting.exists(predicate);			
	}
	
	public AccessPathAndResolver<Field, Fact, Stmt, Method> removeStartingWith(Predicate<AccessPathAndResolver<Field, Fact, Stmt, Method>> predicate) {
		if(nesting == null)
			return this;
		if(predicate.apply(nesting))
			return new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, accessPath, resolver);
		return new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, accessPath, resolver, nesting.removeStartingWith(predicate));
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

	public boolean hasNesting() {
		return nesting != null;
	}

	public AccessPathAndResolver<Field, Fact, Stmt, Method> getNesting() {
		return nesting;
	}

	public AccessPathAndResolver<Field, Fact, Stmt, Method> withoutNesting() {
		if(nesting == null)
			return this;
		else
			return new AccessPathAndResolver<Field, Fact, Stmt, Method>(analyzer, accessPath, resolver);
	}
}
