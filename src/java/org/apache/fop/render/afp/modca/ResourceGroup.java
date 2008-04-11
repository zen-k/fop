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

package org.apache.fop.render.afp.modca;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.fop.render.afp.DataObjectInfo;
import org.apache.fop.render.afp.ImageObjectInfo;
import org.apache.fop.render.afp.ResourceInfo;
import org.apache.fop.render.afp.modca.triplets.FullyQualifiedNameTriplet;
import org.apache.fop.render.afp.tools.StringUtils;
import org.apache.xmlgraphics.image.codec.tiff.TIFFImage;

/**
 * A Resource Group contains a set of overlays.
 */
public final class ResourceGroup extends AbstractNamedAFPObject {
    
    /**
     * Default name for the resource group
     */
    private static final String DEFAULT_NAME = "RG000001";

    /**
     * Mapping of resource uri to data resource object (image/graphic) 
     */
    private Map/*<String,IncludeObject>*/ resourceMap = null;

    /**
     * This resource groups container
     */
    private AbstractResourceGroupContainer container = null;

    /**
     * Default constructor
     * @param container the resource group container 
     */
    public ResourceGroup(AbstractResourceGroupContainer container) {
        this(DEFAULT_NAME, container);
    }

    /**
     * Constructor for the ResourceGroup, this takes a
     * name parameter which must be 8 characters long.
     * @param name the resource group name
     */
    public ResourceGroup(String name, AbstractResourceGroupContainer container) {
        super(name);
        this.container = container;
    }

    private static final String RESOURCE_NAME_PREFIX = "RES";
    private static final String INCLUDE_NAME_PREFIX = "INC";
    
    /**
     * Helper method to create a new resource object in the current container and to return
     * the object.
     * @return a newly created resource object
     */
    private ResourceObject createResourceObject() {
        String name = RESOURCE_NAME_PREFIX
        + StringUtils.lpad(String.valueOf(getResourceCount() + 1), '0', 5);
        ResourceObject resource = new ResourceObject(name);
        return resource;
    }
    
//    /**
//     * Helper method to create a new resource object in the current container and to return
//     * the object.
//     * @return a newly created resource object
//     */
//    private IncludeObject createIncludeObject() {
//        String name = INCLUDE_NAME_PREFIX
//        + StringUtils.lpad(String.valueOf(getResourceCount() + 1), '0', 5);
//        IncludeObject includeObj = new IncludeObject(name);
//        return includeObj;
//    }

    /**
     * Creates a data object in this resource group
     * @param info the data object info
     * @return an include object reference
     */
    public IncludeObject createObject(DataObjectInfo info) {
        IncludeObject includeObj = (IncludeObject)getResourceMap().get(info.getUri());
        if (includeObj == null) {
            AbstractDataObject dataObj;
            if (info instanceof ImageObjectInfo) {
                dataObj = container.createImage((ImageObjectInfo)info);
            } else {
                dataObj = container.createGraphic();
            }

            ResourceInfo resourceInfo = info.getResourceInfo();
            String resourceName = resourceInfo.getName();
            if (resourceInfo.isExternal()) {
                ObjectContainer objectContainer = new ObjectContainer(resourceName, dataObj, info);
            } else {
                
            }
            
            // Wrap the data object in a page segment
            PageSegment pageSegment = container.createPageSegment();
            pageSegment.addObject(dataObj);

            
            // TODO: AC - rotation?
            int rotation = 0;
            dataObj.setViewport(info.getX(), info.getY(),
                    info.getWidth(), info.getHeight(),
                    info.getWidthRes(), info.getHeightRes(), rotation);
            
            includeObj.setResourceInfo(resourceInfo);

            // If resource is to be stored externally,
            // wrap the page segment in a resource object 
            if (resourceInfo.isExternal()) {
                ResourceObject resource = createResourceObject();
                resource.setReferencedObject(pageSegment);
                includeObj = new IncludeObject(resource);
            } else {
                includeObj = new IncludeObject(pageSegment);
            }
            // Add include object to resource map
            getResourceMap().put(info.getUri(), includeObj);                
        }
        //includeObj.setObjectAreaSize(params.getX(), params.getY());
        return includeObj;
    }
    
    /**
     * @return the number of resources contained in this resource group
     */
    public int getResourceCount() {
        if (resourceMap != null) {
            return resourceMap.size(); 
        }
        return 0;
    }
    
    /**
     * Returns true if the resource exists within this resource group,
     * false otherwise.
     * 
     * @param uri the uri of the resource
     * @return true if the resource exists within this resource group
     */
    public boolean resourceExists(String uri) {
        return getResourceMap().containsKey(uri);
    }
    
    /**
     * Returns the list of resources
     * @return the list of resources
     */
    public Map/*<String,AbstractAFPObject>*/ getResourceMap() {
        if (resourceMap == null) {
            resourceMap = new java.util.HashMap/*<String,AbstractAFPObject>*/();
        }
        return resourceMap;
    }

    /**
     * {@inheritDoc}
     */
    public void writeContent(OutputStream os) throws IOException {
        if (resourceMap != null) {
            Collection includes = resourceMap.values();
            Iterator it = includes.iterator();
            while (it.hasNext()) {
                IncludeObject includeObj = (IncludeObject)it.next();
                AbstractStructuredAFPObject obj = includeObj.getReferencedObject();
                obj.writeDataStream(os);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void writeStart(OutputStream os) throws IOException {
        byte[] data = new byte[17];
        data[0] = 0x5A; // Structured field identifier
        data[1] = 0x00; // Length byte 1
        data[2] = 0x10; // Length byte 2
        data[3] = (byte) 0xD3; // Structured field id byte 1
        data[4] = (byte) 0xA8; // Structured field id byte 2
        data[5] = (byte) 0xC6; // Structured field id byte 3
        data[6] = 0x00; // Flags
        data[7] = 0x00; // Reserved
        data[8] = 0x00; // Reserved
        for (int i = 0; i < nameBytes.length; i++) {
            data[9 + i] = nameBytes[i];
        }
        os.write(data);
    }

    /**
     * {@inheritDoc}
     */
    protected void writeEnd(OutputStream os) throws IOException {
        byte[] data = new byte[17];
        data[0] = 0x5A; // Structured field identifier
        data[1] = 0x00; // Length byte 1
        data[2] = 0x10; // Length byte 2
        data[3] = (byte) 0xD3; // Structured field id byte 1
        data[4] = (byte) 0xA9; // Structured field id byte 2
        data[5] = (byte) 0xC6; // Structured field id byte 3
        data[6] = 0x00; // Flags
        data[7] = 0x00; // Reserved
        data[8] = 0x00; // Reserved
        for (int i = 0; i < nameBytes.length; i++) {
            data[9 + i] = nameBytes[i];
        }
        os.write(data);
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        return this.name + " " + this.resourceMap;
    }
}
