/*
 * $Id$
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 *
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */
package org.apache.fop.fo.properties;

import org.apache.fop.datatypes.PercentBase;
import org.apache.fop.fo.FObj;

/**
 * A table-column width specification, possibly including some
 * number of proportional "column-units". The absolute size of a
 * column-unit depends on the fixed and proportional sizes of all
 * columns in the table, and on the overall size of the table.
 * It can't be calculated until all columns have been specified and until
 * the actual width of the table is known. Since this can be specified
 * as a percent of its parent containing width, the calculation is done
 * during layout.
 * NOTE: this is only supposed to be allowed if table-layout=fixed.
 */
public class TableColLength extends LengthProperty {
    /**
     * Number of table-column proportional units
     */
    private double tcolUnits;

    /**
     * The column the this column-units are defined on. 
     */
    private FObj column;

    /**
     * Construct an object with tcolUnits of proportional measure.
     * @param tcolUnits number of table-column proportional units
     */
    public TableColLength(double tcolUnits, FObj column) {
        this.tcolUnits = tcolUnits;
        this.column = column;
    }

    /**
     * Override the method in Length
     * @return the number of specified proportional table-column units.
     */
    public double getTableUnits() {
        return tcolUnits;
    }

    /**
     * Return false because table-col-units are a relative numeric.
     * @see org.apache.fop.datatypes.Numeric#isAbsolute()
     */
    public boolean isAbsolute() {
        return false;
    }

    /**
     * Return the value as a numeric value.
     * @see org.apache.fop.datatypes.Numeric#getNumericValue()
     */
    public double getNumericValue() {
        return tcolUnits * column.getLayoutDimension(PercentBase.TABLE_UNITS).floatValue();
    }

    /**
     * Return the value as a length.
     * @see org.apache.fop.datatypes.Length#getValue()
     */
    public int getValue() {
        return (int) (tcolUnits * column.getLayoutDimension(PercentBase.TABLE_UNITS).floatValue());
    }

    /**
     * Convert this to a String
     * @return the string representation of this
     */
    public String toString() {
        return (Double.toString(tcolUnits) + " table-column-units");
    }

}
