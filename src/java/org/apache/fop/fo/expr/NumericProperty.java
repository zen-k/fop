/*
 * $Id: NumericProperty.java,v 1.3 2003/03/05 21:59:47 jeremias Exp $
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
package org.apache.fop.fo.expr;

import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.properties.ColorTypeProperty;
import org.apache.fop.fo.properties.Property;

/**
 * A numeric property which hold the final absolute result of an expression
 * calculations.  
 */
public class NumericProperty extends Property implements Numeric, Length {
    private double value;
    private int dim;

    /**
     * Construct a Numeric object by specifying one or more components,
     * including absolute length, percent length, table units.
     * @param valType A combination of bits representing the value types.
     * @param value The value of the numeric.
     * @param dim The dimension of the value. 0 for a Number, 1 for a Length
     * (any type), >1, <0 if Lengths have been multiplied or divided.
     */
    protected NumericProperty(double value, int dim) {
        this.value = value;
        this.dim = dim;
    }

    /**
     * Return the dimension.
     * @see Numeric#getDimension()
     */
    public int getDimension() {
        return dim;
    }

    /**
     * Return the value.
     * @see Numeric#getNumericValue()
     */
    public double getNumericValue() {
        return value;
    }

    /**
     * Return true of the numeric is absolute.
     * @see Numeric#isAbsolute()
     */
    public boolean isAbsolute() {
        return true;
    }

    /**
     * Cast this as a Numeric.
     */
    public Numeric getNumeric() {
        return this;
    }

    /**
     * Cast this as a number.
     */
    public Number getNumber() {
        return new Double(value);
    }

    /**
     * Return the value of this numeric as a length in millipoints. 
     */
    public int getValue() {
        return (int) value;
    }

    /**
     * Return false since a numeric can not have the enum value of 'auto'. 
     */
    public boolean isAuto() {
        return false;
    }

    /**
     * Cast this as a length. That is only possible when the dimension is 
     * one.
     */
    public Length getLength() {
        if (dim == 1) {
            return this;
        }
        System.err.print("Can't create length with dimension " + dim);
        return null;
    }

    /**
     * Cast this as a ColorTypeProperty.
     */
    public ColorTypeProperty getColorType() {
        // try converting to numeric number and then to color
        return null;
    }

    /**
     * Cast this as an Object.
     */
    public Object getObject() {
        return this;
    }

    /**
     * Return a string representation of this Numeric. It is only useable for
     * debugging.
     */
    public String toString() {
        return value + "^" + dim;
    }
}
