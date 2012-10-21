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

package org.apache.fop.visual;

/**
 * This interface is used to redirect output from an external application elsewhere.
 */
public interface RedirectorLineHandler {

    /**
     * Called before the first handleLine() call.
     */
    void notifyStart();

    /**
     * Called for each line of output to be processed.
     * @param line a line of application output
     */
    void handleLine(String line);
    
    /**
     * Called after the last handleLine() call.
     */
    void notifyEnd();
}