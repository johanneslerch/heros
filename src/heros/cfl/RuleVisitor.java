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
package heros.cfl;

import java.util.Collection;

public interface RuleVisitor<T> {

	T visit(ContextFreeRule contextFreeRule);

	T visit(NonLinearRule nonLinearRule);

	T visit(RegularRule regularRule);

	T visit(ConstantRule constantRule);

	public static abstract class CollectingRuleVisitor<T, C extends Collection<T>> implements RuleVisitor<C> {

		private C collection;

		public CollectingRuleVisitor(C collection) {
			this.collection = collection;
		}
		
		@Override
		public C visit(ContextFreeRule rule) {
			_visit(rule);
			return collection;
		}
		
		@Override
		public C visit(NonLinearRule rule) {
			_visit(rule);
			return collection;
		}
		
		@Override
		public C visit(RegularRule rule) {
			_visit(rule);
			return collection;
		}
		
		@Override
		public C visit(ConstantRule rule) {
			_visit(rule);
			return collection;
		}
		
		protected void yield(T result) {
			collection.add(result);
		}

		abstract void _visit(ContextFreeRule contextFreeRule);

		abstract void _visit(NonLinearRule nonLinearRule);

		abstract void _visit(RegularRule regularRule);

		abstract void _visit(ConstantRule constantRule);
	}
}
