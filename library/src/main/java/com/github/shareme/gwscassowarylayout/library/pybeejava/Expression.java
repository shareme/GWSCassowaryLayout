/*
 * Cassowary-Java
 *
 * Copyright (C) 1998-2000 Greg J. Badros
 * Copyright (C) 2014 Russell Keith-Magee.
 * Modifications Copyright(C) 2015 Fred Grott(GrottWorkShop)
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   1. Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *
 *   2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of Cassowary nor the names of its contributors may
 *     be used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ===========================================================================
 *
 * This port of the Cassowary algorithm was derived from the original Java
 * implmentation, Copyright (C) 1998-2000 Greg J. Badros. That implementation is
 * distributed under the terms of the LGPL; however, dispensation has been granted
 * to release this derivative work under the BSD license; see:
 *
 * https://groups.google.com/d/msg/overconstrained/rqoXuonGGkc/qwHxV6tKkuQJ
 *
 */
package com.github.shareme.gwscassowarylayout.library.pybeejava;

import java.util.Hashtable;
import java.util.Iterator;

@SuppressWarnings("unused")
public class Expression
{
    private double _constant;
    private Hashtable<AbstractVariable, Double> _terms;

    public Expression(AbstractVariable clv, double value, double constant)
    {
        _constant = constant;
        _terms = new Hashtable<>();
        if (clv != null)
        {
            _terms.put(clv, value);
        }
    }

    public Expression(double num)
    {
        this(null, 0.0, num);
    }

    public Expression()
    {
        this(0.0);
    }

    public Expression(AbstractVariable clv, double value)
    {
        this(clv, value, 0.0);
    }

    public Expression(AbstractVariable clv)
    {
        this(clv, 1.0, 0.0);
    }

    // for use by the clone method
    protected Expression(double constant, Hashtable<AbstractVariable, Double> terms)
    {
        _constant = constant;
        _terms = new Hashtable<>();

        for (AbstractVariable clv: terms.keySet())
        {
            _terms.put(clv, terms.get(clv));
        }
    }

    public Expression multiplyMe(double x)
    {
        _constant = _constant * x;

        for (AbstractVariable clv: _terms.keySet())
        {
            _terms.put(clv, _terms.get(clv) * x);
        }
        return this;
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    public final Expression clone()
    {
        return new Expression(_constant, _terms);
    }

    public final Expression times(double x)
    {
      return clone().multiplyMe(x);
    }

    public final Expression times(Expression expr)
            throws NonlinearExpression
    {
        if (isConstant())
        {
            return expr.times(_constant);
        }
        else if (!expr.isConstant())
        {
            throw new NonlinearExpression();
        }
        return times(expr._constant);
    }

    public final Expression plus(Expression expr)
    {
        return clone().addExpression(expr, 1.0);
    }

    public final Expression plus(Variable var)
        throws NonlinearExpression
    {
        return clone().addVariable(var, 1.0);
    }

    public final Expression minus(Expression expr)
    {
        return clone().addExpression(expr, -1.0);
    }

    public final Expression minus(Variable var)
            throws NonlinearExpression
    {
        return clone().addVariable(var, -1.0);
    }

    public final Expression divide(double x)
            throws NonlinearExpression
    {
        if (Util.approx(x, 0.0))
        {
            throw new NonlinearExpression();
        }
        return times(1.0 / x);
    }

    public final Expression divide(Expression expr)
            throws NonlinearExpression
    {
        if (!expr.isConstant())
        {
            throw new NonlinearExpression();
        }
        return divide(expr._constant);
    }

    public final Expression divFrom(Expression expr)
            throws NonlinearExpression
    {
        if (!isConstant() || Util.approx(_constant, 0.0))
        {
            throw new NonlinearExpression();
        }
        return expr.divide(_constant);
    }

    public final Expression subtractFrom(Expression expr)
    {
        return expr.minus(this);
    }

    // Add n*expr to this expression from another expression expr.
    // Notify the solver if a variable is added or deleted from this
    // expression.
    public final Expression addExpression(Expression expr, double n, AbstractVariable subject, Tableau solver)
    {
        incrementConstant(n * expr.getConstant());

        for (AbstractVariable clv: expr.getTerms().keySet())
        {
            double coeff = expr.getTerms().get(clv);
            addVariable(clv, coeff*n, subject, solver);
        }
        return this;
    }

    // Add n*expr to this expression from another expression expr.
    public final Expression addExpression(Expression expr, double n)
    {
        incrementConstant(n * expr.getConstant());

        for (AbstractVariable clv: expr.getTerms().keySet())
        {
            double coeff = expr.getTerms().get(clv);
            addVariable(clv, coeff*n);
        }
        return this;
    }

    public final Expression addExpression(Expression expr)
    {
      return addExpression(expr, 1.0);
    }

    // Add a term c*v to this expression.  If the expression already
    // contains a term involving v, add c to the existing coefficient.
    // If the new coefficient is approximately 0, delete v.
    public final Expression addVariable(AbstractVariable v, double c)
    {
        // body largely duplicated below
        Double coeff = _terms.get(v);
        if (coeff != null)
        {
            double new_coefficient = coeff + c;
            if (Util.approx(new_coefficient, 0.0))
            {
                _terms.remove(v);
            }
            else
            {
                _terms.put(v, new_coefficient);
            }
        }
        else
        {
            if (!Util.approx(c, 0.0))
            {
                _terms.put(v, c);
            }
        }
        return this;
    }

    public final Expression addVariable(AbstractVariable v)
    {
        return addVariable(v, 1.0);
    }


    public final Expression setVariable(AbstractVariable v, double c)
    {
        _terms.put(v, c);
        return this;
    }

    // Add a term c*v to this expression.  If the expression already
    // contains a term involving v, add c to the existing coefficient.
    // If the new coefficient is approximately 0, delete v.  Notify the
    // solver if v appears or disappears from this expression.
    public final Expression addVariable(AbstractVariable v, double c, AbstractVariable subject, Tableau solver)
    {
        // body largely duplicated above
        Double coeff = _terms.get(v);
        if (coeff != null)
        {
            double new_coefficient = coeff + c;
            if (Util.approx(new_coefficient, 0.0))
            {
                solver.noteRemovedVariable(v, subject);
                _terms.remove(v);
            }
            else
            {
                _terms.put(v, new_coefficient);
            }
        }
        else
        {
            if (!Util.approx(c, 0.0))
            {
                _terms.put(v, c);
                solver.noteAddedVariable(v, subject);
            }
        }
        return this;
    }

    // Return a pivotable variable in this expression.  (It is an error
    // if this expression is constant -- signal InternalError in
    // that case).  Return null if no pivotable variables
    public final AbstractVariable anyPivotableVariable() throws InternalError
    {
        if (isConstant())
        {
            throw new InternalError("anyPivotableVariable called on a getConstant");
        }

        for (AbstractVariable clv: _terms.keySet())
        {
            if (clv.isPivotable())
            {
                return clv;
            }
        }

        // No pivotable variables, so just return null, and let the caller
        // error if needed
        return null;
    }

    // Replace var with a symbolic expression expr that is equal to it.
    // If a variable has been added to this expression that wasn't there
    // before, or if a variable has been dropped from this expression
    // because it now has a coefficient of 0, inform the solver.
    // PRECONDITIONS:
    //   var occurs with a non-zero coefficient in this expression.
    public final void substituteOut(AbstractVariable var, Expression expr, AbstractVariable subject, Tableau solver)
    {
        double multiplier = _terms.remove(var);
        incrementConstant(multiplier * expr.getConstant());

        for (AbstractVariable clv: expr.getTerms().keySet())
        {
            double coeff = expr.getTerms().get(clv);
            Double d_old_coeff = _terms.get(clv);
            if (d_old_coeff != null)
            {
                double old_coeff = d_old_coeff;
                double newCoeff = old_coeff + multiplier * coeff;
                if (Util.approx(newCoeff, 0.0))
                {
                    solver.noteRemovedVariable(clv, subject);
                    _terms.remove(clv);
                }
                else
                {
                    _terms.put(clv, newCoeff);
                }
            }
            else
            {
                // did not have that variable already
                _terms.put(clv, multiplier * coeff);
                solver.noteAddedVariable(clv, subject);
            }
        }
    }

    // This linear expression currently represents the equation
    // oldSubject=self.  Destructively modify it so that it represents
    // the equation newSubject=self.
    //
    // Precondition: newSubject currently has a nonzero coefficient in
    // this expression.
    //
    // NOTES
    //   Suppose this expression is c + a*newSubject + a1*v1 + ... + an*vn.
    //
    //   Then the current equation is
    //       oldSubject = c + a*newSubject + a1*v1 + ... + an*vn.
    //   The new equation will be
    //        newSubject = -c/a + oldSubject/a - (a1/a)*v1 - ... - (an/a)*vn.
    //   Note that the term involving newSubject has been dropped.
    public final void changeSubject(AbstractVariable old_subject, AbstractVariable new_subject)
    {
        _terms.put(old_subject, newSubject(new_subject));
    }

    // This linear expression currently represents the equation self=0.  Destructively modify it so
    // that subject=self represents an equivalent equation.
    //
    // Precondition: subject must be one of the variables in this expression.
    // NOTES
    //   Suppose this expression is
    //     c + a*subject + a1*v1 + ... + an*vn
    //   representing
    //     c + a*subject + a1*v1 + ... + an*vn = 0
    // The modified expression will be
    //    subject = -c/a - (a1/a)*v1 - ... - (an/a)*vn
    //   representing
    //    subject = -c/a - (a1/a)*v1 - ... - (an/a)*vn
    //
    // Note that the term involving subject has been dropped.
    // Returns the reciprocal, so changeSubject can use it, too
    public final double newSubject(AbstractVariable subject)
    {
        Double coeff = _terms.remove(subject);
        double reciprocal = 1.0 / coeff;
        multiplyMe(-reciprocal);
        return reciprocal;
    }

    // Return the coefficient corresponding to variable var, i.e.,
    // the 'ci' corresponding to the 'vi' that var is:
    //     v1*c1 + v2*c2 + .. + vn*cn + c
    public final double coefficientFor(AbstractVariable var)
    {
        Double coeff = _terms.get(var);
        if (coeff != null)
        {
            return coeff;
        }
        else
        {
            return 0.0;
        }
    }

    public final double getConstant()
    {
        return _constant;
    }

    public final void setConstant(double c)
    {
        _constant = c;
    }

    public final Hashtable<AbstractVariable, Double> getTerms()
    {
        return _terms;
    }

    public final void incrementConstant(double c)
    {
        _constant = _constant + c;
    }

    public final boolean isConstant()
    {
        return _terms.size() == 0;
    }

    public final String toString()
    {
        StringBuilder bstr = new StringBuilder();
        Iterator<AbstractVariable> e = _terms.keySet().iterator();

        if (!Util.approx(_constant, 0.0) || _terms.size() == 0)
        {
            bstr.append(_constant);
        }
        else
        {
            if (_terms.size() == 0)
            {
                return bstr.toString();
            }
            AbstractVariable clv = e.next();
            Double coeff = _terms.get(clv);
            bstr.append(coeff.toString()).append("*").append(clv.toString());
        }
        while (e.hasNext())
        {
            AbstractVariable clv = e.next();
            Double coeff = _terms.get(clv);
            bstr.append(" + ").append(coeff.toString()).append("*").append(clv.toString());
        }
        return bstr.toString();
    }

    public static Expression plus(Expression e1, Expression e2)
    {
        return e1.plus(e2);
    }

    public static Expression minus(Expression e1, Expression e2)
    {
        return e1.minus(e2);
    }

    public static Expression times(Expression e1, Expression e2)
            throws NonlinearExpression
    {
        return e1.times(e2);
    }

    public static Expression divide(Expression e1, Expression e2)
            throws NonlinearExpression
    {
        return e1.divide(e2);
    }

    public static boolean fequals(Expression e1, Expression e2)
    {
        return e1 == e2;
    }
}
