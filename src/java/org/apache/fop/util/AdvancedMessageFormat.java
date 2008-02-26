/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

package org.apache.fop.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xml.sax.Locator;

/**
 * Formats messages based on a template and with a set of named parameters. This is similar to
 * {@link java.util.MessageFormat} but uses named parameters and supports conditional sub-groups.
 * <p>
 * Example:
 * </p>
 * <p><code>Missing field "{fieldName}"[ at location: {location}]!</code></p>
 * <ul>
 *   <li>Curly brackets ("{}") are used for fields.</li>
 *   <li>Square brackets ("[]") are used to delimit conditional sub-groups. A sub-group is
 *     conditional when all fields inside the sub-group have a null value. In the case, everything
 *     between the brackets is skipped.</li>
 * </ul>
 */
public class AdvancedMessageFormat {

    private CompositePart rootPart;
    
    /**
     * Construct a new message format.
     * @param pattern the message format pattern.
     */
    public AdvancedMessageFormat(CharSequence pattern) {
        parsePattern(pattern);
    }
    
    private void parsePattern(CharSequence pattern) {
        rootPart = new CompositePart(false);
        StringBuffer sb = new StringBuffer();
        parseInnerPattern(pattern, rootPart, sb, 0);
    }
    
    private int parseInnerPattern(CharSequence pattern, CompositePart parent,
            StringBuffer sb, int start) {
        assert sb.length() == 0;
        int i = start;
        int len = pattern.length();
        loop:
        while (i < len) {
            char ch = pattern.charAt(i);
            switch (ch) {
            case '{':
                if (sb.length() > 0) {
                    parent.addChild(new TextPart(sb.toString()));
                    sb.setLength(0);
                }
                i++;
                while (i < len) {
                    ch = pattern.charAt(i);
                    if (ch == '}') {
                        i++;
                        break;
                    }
                    sb.append(ch);
                    i++;
                }
                parent.addChild(parseField(sb.toString()));
                sb.setLength(0);
                break;
            case ']':
                i++;
                break loop; //Current composite is finished
            case '[':
                if (sb.length() > 0) {
                    parent.addChild(new TextPart(sb.toString()));
                    sb.setLength(0);
                }
                i++;
                CompositePart composite = new CompositePart(true);
                parent.addChild(composite);
                i += parseInnerPattern(pattern, composite, sb, i);
                break;
            case '\\':
                if (i < len - 1) {
                    i++;
                    ch = pattern.charAt(i);
                }
                //no break here! Must be right before "default" section
            default:
                sb.append(ch);
                i++;
            }
        }
        if (sb.length() > 0) {
            parent.addChild(new TextPart(sb.toString()));
            sb.setLength(0);
        }
        return i - start;
    }
    
    private Part parseField(String field) {
        //TODO Add advanced formatting like in MessageFormat here
        return new SimpleFieldPart(field);
    }

    /**
     * Formats a message with the given parameters.
     * @param params a Map of named parameters (Contents: <String, Object>)
     * @return the formatted message
     */
    public String format(Map params) {
        StringBuffer sb = new StringBuffer();
        rootPart.write(sb, params);
        return sb.toString();
    }

    private interface Part {
        void write(StringBuffer sb, Map params);
        boolean isGenerated(Map params);
    }
    
    private interface ObjectFormatter {
        void format(StringBuffer sb, Object obj);
        boolean supportsObject(Object obj);
    }
    
    private class TextPart implements Part {
        
        private String text;
        
        public TextPart(String text) {
            this.text = text;
        }
        
        public void write(StringBuffer sb, Map params) {
            sb.append(text);
        }
        
        public boolean isGenerated(Map params) {
            return true;
        }

        /** {@inheritDoc} */
        public String toString() {
            return this.text;
        }
    }
    
    private static class SimpleFieldPart implements Part {
        
        private static final List OBJECT_FORMATTERS = new java.util.ArrayList();
        
        static {
            OBJECT_FORMATTERS.add(new LocatorFormatter());
        }
        
        private String fieldName;
        
        public SimpleFieldPart(String fieldName) {
            this.fieldName = fieldName;
        }
        
        public void write(StringBuffer sb, Map params) {
            if (!params.containsKey(fieldName)) {
                throw new IllegalArgumentException(
                        "Message pattern contains unsupported field name: " + fieldName);
            }
            Object obj = params.get(fieldName);
            if (obj instanceof String) {
                sb.append(obj);
            } else {
                boolean handled = false;
                Iterator iter = OBJECT_FORMATTERS.iterator();
                while (iter.hasNext()) {
                    ObjectFormatter formatter = (ObjectFormatter)iter.next();
                    if (formatter.supportsObject(obj)) {
                        formatter.format(sb, obj);
                        handled = true;
                        break;
                    }
                }
                if (!handled) {
                    sb.append(obj.toString());
                }
            }
        }

        public boolean isGenerated(Map params) {
            Object obj = params.get(fieldName);
            return obj != null;
        }
        
        /** {@inheritDoc} */
        public String toString() {
            return "{" + this.fieldName + "}";
        }
    }
    
    private class CompositePart implements Part {
        
        private List parts = new java.util.ArrayList();
        private boolean conditional;
        
        public CompositePart(boolean conditional) {
            this.conditional = conditional;
        }
        
        public void addChild(Part part) {
            this.parts.add(part);
        }

        public void write(StringBuffer sb, Map params) {
            if (isGenerated(params)) {
                Iterator iter = this.parts.iterator();
                while (iter.hasNext()) {
                    Part part = (Part)iter.next();
                    part.write(sb, params);
                }
            }
        }

        public boolean isGenerated(Map params) {
            if (conditional) {
                Iterator iter = this.parts.iterator();
                while (iter.hasNext()) {
                    Part part = (Part)iter.next();
                    if (!part.isGenerated(params)) {
                        return false;
                    }
                }
            }
            return true;
        }
        
        /** {@inheritDoc} */
        public String toString() {
            return this.parts.toString();
        }
    }
    
    private static class LocatorFormatter implements ObjectFormatter {

        public void format(StringBuffer sb, Object obj) {
            Locator loc = (Locator)obj;
            sb.append(loc.getLineNumber()).append(":").append(loc.getColumnNumber());
        }

        public boolean supportsObject(Object obj) {
            return obj instanceof Locator;
        }
        
    }
}