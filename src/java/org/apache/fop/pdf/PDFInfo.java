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

package org.apache.fop.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * class representing an /Info object
 */
public class PDFInfo extends PDFObject {

    /**
     * the application producing the PDF
     */
    private String producer;

    private String title = null;
    private String author = null;
    private String subject = null;
    private String keywords = null;
    private Date creationDate = null;
    private Date modDate = null;

    /**
     * the name of the application that created the
     * original document before converting to PDF
     */
    private String creator;

    /** @return the producer of the document or null if not set */
    public String getProducer() {
        return this.producer;
    }

    /**
     * set the producer string
     *
     * @param producer the producer string
     */
    public void setProducer(String producer) {
        this.producer = producer;
    }

    /** @return the creator of the document or null if not set */
    public String getCreator() {
        return this.creator;
    }

    /**
     * set the creator string
     *
     * @param creator the document creator
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /** @return the title string */
    public String getTitle() {
        return this.title;
    }

    /**
     * set the title string
     *
     * @param t the document title
     */
    public void setTitle(String t) {
        this.title = t;
    }

    /** @return the author of the document or null if not set */
    public String getAuthor() {
        return this.author;
    }

    /**
     * set the author string
     *
     * @param a the document author
     */
    public void setAuthor(String a) {
        this.author = a;
    }

    /** @return the subject of the document or null if not set */
    public String getSubject() {
        return this.subject;
    }

    /**
     * set the subject string
     *
     * @param s the document subject
     */
    public void setSubject(String s) {
        this.subject = s;
    }

    /** @return the keywords for the document or null if not set */
    public String getKeywords() {
        return this.keywords;
    }

    /**
     * set the keywords string
     *
     * @param k the keywords for this document
     */
    public void setKeywords(String k) {
        this.keywords = k;
    }

    /**
     * @return last set creation date
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * @param date Date to store in the PDF as creation date. Use null to force current system date.
     */
    public void setCreationDate(Date date) {
        creationDate = date;
    }

    /** @return last modification date
     */
    public Date getModDate() {
        return this.modDate;
    }

    /**
     * Sets the date of the last modification.
     * @param date the last modification date or null if there are no modifications
     */
    public void setModDate(Date date) {
        this.modDate = date;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] toPDF() {
        PDFProfile profile = getDocumentSafely().getProfile();
        ByteArrayOutputStream bout = new ByteArrayOutputStream(128);
        try {
            bout.write(encode(getObjectID()));
            bout.write(encode("<<\n"));
            if (title != null && title.length() > 0) {
                bout.write(encode("/Title "));
                bout.write(encodeText(this.title));
                bout.write(encode("\n"));
            } else {
                profile.verifyTitleAbsent();
            }
            if (author != null) {
                bout.write(encode("/Author "));
                bout.write(encodeText(this.author));
                bout.write(encode("\n"));
            }
            if (subject != null) {
                bout.write(encode("/Subject "));
                bout.write(encodeText(this.subject));
                bout.write(encode("\n"));
            }
            if (keywords != null) {
                bout.write(encode("/Keywords "));
                bout.write(encodeText(this.keywords));
                bout.write(encode("\n"));
            }

            if (creator != null) {
                bout.write(encode("/Creator "));
                bout.write(encodeText(this.creator));
                bout.write(encode("\n"));
            }

            bout.write(encode("/Producer "));
            bout.write(encodeText(this.producer));
            bout.write(encode("\n"));

            // creation date in form (D:YYYYMMDDHHmmSSOHH'mm')
            if (creationDate == null) {
                creationDate = new Date();
            }
            bout.write(encode("/CreationDate "));
            bout.write(encodeString(formatDateTime(creationDate)));
            bout.write(encode("\n"));

            if (profile.isModDateRequired() && this.modDate == null) {
                this.modDate = this.creationDate;
            }
            if (this.modDate != null) {
                bout.write(encode("/ModDate "));
                bout.write(encodeString(formatDateTime(modDate)));
                bout.write(encode("\n"));
            }
            if (profile.isPDFXActive()) {
                bout.write(encode("/GTS_PDFXVersion "));
                bout.write(encodeString(profile.getPDFXMode().getName()));
                bout.write(encode("\n"));
            }
            if (profile.isTrappedEntryRequired()) {
                bout.write(encode("/Trapped /False\n"));
            }

            bout.write(encode(">>\nendobj\n"));
        } catch (IOException ioe) {
            log.error("Ignored I/O exception", ioe);
        }
        return bout.toByteArray();
    }

    /**
     * Returns a SimpleDateFormat instance for formatting PDF date-times.
     * @return a new SimpleDateFormat instance
     */
    protected static SimpleDateFormat getPDFDateFormat() {
        SimpleDateFormat df = new SimpleDateFormat("'D:'yyyyMMddHHmmss", Locale.ENGLISH);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df;
    }

    /**
     * Formats a date/time according to the PDF specification (D:YYYYMMDDHHmmSSOHH'mm').
     * @param time date/time value to format
     * @param tz the time zone
     * @return the requested String representation
     */
    protected static String formatDateTime(Date time, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz, Locale.ENGLISH);
        cal.setTime(time);

        int offset = cal.get(Calendar.ZONE_OFFSET);
        offset += cal.get(Calendar.DST_OFFSET);

        // DateFormat is operating on GMT so adjust for time zone offset
        Date dt1 = new Date(time.getTime() + offset);
        StringBuffer sb = new StringBuffer();
        sb.append(getPDFDateFormat().format(dt1));

        offset /= (1000 * 60); // Convert to minutes

        if (offset == 0) {
            sb.append('Z');
        } else {
            if (offset > 0) {
                sb.append('+');
            } else {
                sb.append('-');
            }
            int offsetHour = Math.abs(offset / 60);
            int offsetMinutes = Math.abs(offset % 60);
            if (offsetHour < 10) {
                sb.append('0');
            }
            sb.append(Integer.toString(offsetHour));
            sb.append('\'');
            if (offsetMinutes < 10) {
                sb.append('0');
            }
            sb.append(Integer.toString(offsetMinutes));
            sb.append('\'');
        }
        return sb.toString();
    }

    /**
     * Formats a date/time according to the PDF specification. (D:YYYYMMDDHHmmSSOHH'mm').
     * @param time date/time value to format
     * @return the requested String representation
     */
    protected static String formatDateTime(Date time) {
        return formatDateTime(time, TimeZone.getDefault());
    }
}

