package org.qiunet.function.formula.parse;

import org.qiunet.function.formula.IFormula;
import org.qiunet.function.formula.IFormulaParam;

/***
 *
 * 括号的parse
 *
 * @author qiunet
 * 2020-12-02 11:03
 */
public class BracketsFormulaParse<Obj extends IFormulaParam> extends BasicBracketsFormulaParse<Obj> {
	public BracketsFormulaParse() {
		super(Formula::new, "");
	}
	/***
	 * 括号
	 *
	 * @author qiunet
	 * 2020-12-02 10:24
	 */
	private static class Formula<Obj extends IFormulaParam> implements IFormula<Obj> {
		private final IFormula<Obj> formula;

		public Formula(IFormula<Obj> formula) {
			this.formula = formula;
		}

		@Override
		public double cal(Obj params) {
			return formula.cal(params);
		}

		@Override
		public String toString() {
			return "("+formula.toString()+")";
		}
	}

}
