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


public class NonLinearRule implements Rule {

	private final Rule left;
	private final Rule right;

	public NonLinearRule(Rule left, Rule right) {
		assert !left.isEmpty() && !right.isEmpty() && !(right instanceof ConstantRule);
		this.left = left;
		this.right = right;
	}

	@Override
	public boolean isSolved() {
		return right.isSolved();
	}

	@Override
	public boolean isPossible() {
		return right.isPossible();
	}

	@Override
	public <T> T accept(RuleVisitor<T> ruleVisitor) {
		return ruleVisitor.visit(this);
	}

	@Override
	public Rule append(Terminal... terminals) {
		return new NonLinearRule(left, right.append(terminals));
	}
	
	@Override
	public Rule append(Rule rule) {
		return rule.accept(new RuleVisitor<Rule>() {
			@Override
			public Rule visit(ContextFreeRule contextFreeRule) {
				return new NonLinearRule(NonLinearRule.this, contextFreeRule);
			}

			@Override
			public Rule visit(NonLinearRule nonLinearRule) {
				return new NonLinearRule(NonLinearRule.this, nonLinearRule);
			}

			@Override
			public Rule visit(RegularRule regularRule) {
				return new NonLinearRule(NonLinearRule.this, regularRule);
			}

			@Override
			public Rule visit(ConstantRule constantRule) {
				return append(constantRule.getTerminals());
			}
		});
	}

	public Rule getLeft() {
		return left;
	}
	
	public Rule getRight() {
		return right;
	}

	@Override
	public boolean containsNonTerminals() {
		return right.containsNonTerminals() || left.containsNonTerminals();
	}

	@Override
	public Terminal[] getTerminals() {
		throw new IllegalStateException("Only allowed to be called when rule does not contain non-terminals. A non-linear rule should never exist without non-terminals on left and right side.");
	}

	@Override
	public String toString() {
		return "("+left.toString()+","+right.toString()+")";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
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
		NonLinearRule other = (NonLinearRule) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}

	@Override
	public boolean isEmpty() {
		return left.isEmpty() && right.isEmpty();
	}

	@Override
	public void traverse(Traversal t) {
		left.traverse(t);
		right.traverse(t);
	}

}
