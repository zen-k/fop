/*
 * Copyright 2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.fo.properties;

import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.properties.NumberProperty.Maker;

/**
 * Custom Maker adding validity check for reference-orientation
 */
public class ReferenceOrientationMaker extends Maker {

    /**
     * Constructor
     * @param propId the Constant Id for the property to be made
     * @see org.apache.fop.fo.properties.PropertyMaker#PropertyMaker(propId)
     */
    public ReferenceOrientationMaker(int propId) {
        super(propId);
    }
    
    /**
     * Check the value of the reference-orientation property.
     * 
     * @see org.apache.fop.fo.properties.PropertyMaker#get(int, PropertyList, boolean, boolean)
     */
    public Property get(int subpropId, PropertyList propertyList,
                        boolean tryInherit, boolean tryDefault) 
            throws PropertyException {
        
        Property p = super.get(0, propertyList, tryInherit, tryDefault);
        int ro = 0;
        if (p != null) {
            ro = p.getNumeric().getValue();
        }
        if ((Math.abs(ro) % 90) == 0 && (Math.abs(ro) / 90) <= 3) {
            return p;
        } else {
            throw new PropertyException("Illegal property value: "
                    + "reference-orientation=\"" + ro + "\" "
                    + "on " + propertyList.getFObj().getName());
        }
    }

}