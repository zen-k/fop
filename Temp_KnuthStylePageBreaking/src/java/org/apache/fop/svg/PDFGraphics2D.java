/*
 * Copyright 1999-2004 The Apache Software Foundation.
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

package org.apache.fop.svg;

import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.pdf.PDFResources;
import org.apache.fop.pdf.PDFGState;
import org.apache.fop.pdf.PDFColorSpace;
import org.apache.fop.pdf.PDFColor;
import org.apache.fop.pdf.PDFState;
import org.apache.fop.pdf.PDFNumber;
import org.apache.fop.pdf.PDFText;
import org.apache.fop.pdf.PDFXObject;
import org.apache.fop.pdf.PDFPattern;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFLink;
import org.apache.fop.pdf.PDFAnnotList;
import org.apache.fop.pdf.BitmapImage;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontSetup;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.fonts.LazyFont;
import org.apache.fop.image.JpegImage;
import org.apache.fop.fonts.CIDFont;
import org.apache.fop.render.pdf.FopPDFImage;

import org.apache.batik.ext.awt.g2d.AbstractGraphics2D;
import org.apache.batik.ext.awt.g2d.GraphicContext;
import org.apache.batik.ext.awt.RadialGradientPaint;
import org.apache.batik.ext.awt.LinearGradientPaint;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.apache.batik.gvt.PatternPaint;
import org.apache.batik.gvt.GraphicsNode;

import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.GraphicsConfiguration;
/*  java.awt.Font is not imported to avoid confusion with
    org.apache.fop.fonts.Font */
import java.awt.Image;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.renderable.RenderableImage;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.color.ColorSpace;
import java.io.StringWriter;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Map;
import java.util.List;

/**
 * PDF Graphics 2D.
 * Used for drawing into a pdf document as if it is a graphics object.
 * This takes a pdf document and draws into it.
 *
 * @author <a href="mailto:keiron@aftexsw.com">Keiron Liddle</a>
 * @version $Id: PDFGraphics2D.java,v 1.48 2003/03/07 09:51:26 jeremias Exp $
 * @see org.apache.batik.ext.awt.g2d.AbstractGraphics2D
 */
public class PDFGraphics2D extends AbstractGraphics2D {
    /**
     * the PDF Document being created
     */
    protected PDFDocument pdfDoc;

    /**
     * The current resource context for adding fonts, patterns etc.
     */
    protected PDFResourceContext resourceContext;

    /**
     * The PDF reference of the current page.
     */
    protected String pageRef;

    /**
     * the current state of the pdf graphics
     */
    protected PDFState graphicsState;

    /**
     * The PDF graphics state level that this svg is being drawn into.
     */
    protected int baseLevel = 0;

    /**
     * The count of JPEG images added to document so they recieve
     * unique keys.
     */
    protected int jpegCount = 0;

    /**
     * The current font information.
     */
    protected FontInfo fontInfo;

    /**
     * The override font state used when drawing text and the font cannot be
     * set using java fonts.
     */
    protected Font ovFontState = null;

    /**
     * the current stream to add PDF commands to
     */
    protected StringWriter currentStream = new StringWriter();

    /**
     * the current (internal) font name
     */
    protected String currentFontName;

    /**
     * the current font size in millipoints
     */
    protected float currentFontSize;

    /**
     * The output stream for the pdf document.
     * If this is set then it can progressively output
     * the pdf document objects to reduce memory.
     * Especially with images.
     */
    protected OutputStream outputStream = null;

    /**
     * Create a new PDFGraphics2D with the given pdf document info.
     * This is used to create a Graphics object for use inside an already
     * existing document.
     *
     * @param textAsShapes if true then draw text as shapes
     * @param fi the current font information
     * @param doc the pdf document for creating pdf objects
     * @param page the current resource context or page
     * @param pref the PDF reference of the current page
     * @param font the current font name
     * @param size the current font size
     */
    public PDFGraphics2D(boolean textAsShapes, FontInfo fi, PDFDocument doc,
                         PDFResourceContext page, String pref, String font, float size) {
        this(textAsShapes);
        pdfDoc = doc;
        resourceContext = page;
        currentFontName = font;
        currentFontSize = size;
        fontInfo = fi;
        pageRef = pref;
        graphicsState = new PDFState();
    }

    /**
     * Create a new PDFGraphics2D.
     *
     * @param textAsShapes true if drawing text as shapes
     */
    protected PDFGraphics2D(boolean textAsShapes) {
        super(textAsShapes);
    }

    /**
     * This constructor supports the create method.
     * This is not implemented properly.
     *
     * @param g the PDF graphics to make a copy of
     */
    public PDFGraphics2D(PDFGraphics2D g) {
        super(g);
    }

    /**
     * Creates a new <code>Graphics</code> object that is
     * a copy of this <code>Graphics</code> object.
     * @return     a new graphics context that is a copy of
     * this graphics context.
     */
    public Graphics create() {
        return new PDFGraphics2D(this);
    }

    /**
     * Set the PDF state to use when starting to draw
     * into the PDF graphics.
     *
     * @param state the PDF state
     */
    public void setPDFState(PDFState state) {
        graphicsState = state;
        baseLevel = graphicsState.getStackLevel();
    }

    /**
     * Set the output stream that this PDF document is
     * being drawn to. This is so that it can progressively
     * use the PDF document to output data such as images.
     * This results in a significant saving on memory.
     *
     * @param os the output stream that is being used for the PDF document
     */
    public void setOutputStream(OutputStream os) {
        outputStream = os;
    }

    /**
     * Get the string containing all the commands written into this
     * Grpahics.
     * @return the string containing the PDF markup
     */
    public String getString() {
        return currentStream.toString();
    }

    /**
     * Set the Grpahics context.
     * @param c the graphics context to use
     */
    public void setGraphicContext(GraphicContext c) {
        gc = c;
        setPrivateHints();
    }

    private void setPrivateHints() {
        setRenderingHint(RenderingHintsKeyExt.KEY_AVOID_TILE_PAINTING, 
                RenderingHintsKeyExt.VALUE_AVOID_TILE_PAINTING_ON);
    }
    
    /**
     * Set the override font state for drawing text.
     * This is used by the PDF text painter so that it can temporarily
     * set the font state when a java font cannot be used.
     * The next drawString will use this font state.
     *
     * @param infont the font state to use
     */
    public void setOverrideFontState(Font infont) {
        ovFontState = infont;
    }

    /**
     * Restore the PDF graphics state to the starting state level.
     */
    /* seems not to be used
    public void restorePDFState() {
        for (int count = graphicsState.getStackLevel(); count > baseLevel; count--) {
            currentStream.write("Q\n");
        }
        graphicsState.restoreLevel(baseLevel);
    }*/

    /**
     * This is a pdf specific method used to add a link to the
     * pdf document.
     *
     * @param bounds the bounds of the link in user coordinates
     * @param trans the transform of the current drawing position
     * @param dest the PDF destination
     * @param linkType the type of link, internal or external
     */
    public void addLink(Rectangle2D bounds, AffineTransform trans, String dest, int linkType) {
        AffineTransform at = getTransform();
        Shape b = at.createTransformedShape(bounds);
        b = trans.createTransformedShape(b);
        if (b != null) {
            Rectangle rect = b.getBounds();

            if (linkType != PDFLink.EXTERNAL) {
                String pdfdest = "/FitR " + dest;
                resourceContext.addAnnotation(
                    pdfDoc.getFactory().makeLink(rect, pageRef, pdfdest));
            } else {
                resourceContext.addAnnotation(
                    pdfDoc.getFactory().makeLink(rect, dest, linkType, 0));
            }
        }
    }

    /**
     * Add a JPEG image directly to the PDF document.
     * This is used by the PDFImageElementBridge to draw a JPEG
     * directly into the pdf document rather than converting the image into
     * a bitmap and increasing the size.
     *
     * @param jpeg the jpeg image to draw
     * @param x the x position
     * @param y the y position
     * @param width the width to draw the image
     * @param height the height to draw the image
     */
    public void addJpegImage(JpegImage jpeg, float x, float y, 
                             float width, float height) {
        String key = "__AddJPEG_"+jpegCount;
        jpegCount++;
        FopPDFImage fopimage = new FopPDFImage(jpeg, key);
        int xObjectNum = this.pdfDoc.addImage(resourceContext, 
                                              fopimage).getXNumber();

        AffineTransform at = getTransform();
        double[] matrix = new double[6];
        at.getMatrix(matrix);
        currentStream.write("q\n");
        Shape imclip = getClip();
        writeClip(imclip);
        if (!at.isIdentity()) {
            currentStream.write("" + matrix[0] + " " + matrix[1] + " "
                                + matrix[2] + " " + matrix[3] + " "
                                + matrix[4] + " " + matrix[5] + " cm\n");
        }

        currentStream.write("" + width + " 0 0 "
                          + (-height) + " "
                          + x + " "
                          + (y + height) + " cm\n" + "/Im"
                          + xObjectNum + " Do\nQ\n");

        if (outputStream != null) {
            try {
                this.pdfDoc.output(outputStream);
            } catch (IOException ioe) {
                // ignore exception, will be thrown again later
            }
        }
    }

    /**
     * Draws as much of the specified image as is currently available.
     * The image is drawn with its top-left corner at
     * (<i>x</i>,&nbsp;<i>y</i>) in this graphics context's coordinate
     * space. Transparent pixels in the image do not affect whatever
     * pixels are already there.
     * <p>
     * This method returns immediately in all cases, even if the
     * complete image has not yet been loaded, and it has not been dithered
     * and converted for the current output device.
     * <p>
     * If the image has not yet been completely loaded, then
     * <code>drawImage</code> returns <code>false</code>. As more of
     * the image becomes available, the process that draws the image notifies
     * the specified image observer.
     * @param    img the specified image to be drawn.
     * @param    x   the <i>x</i> coordinate.
     * @param    y   the <i>y</i> coordinate.
     * @param    observer    object to be notified as more of
     * the image is converted.
     * @return true if the image was drawn
     * @see      java.awt.Image
     * @see      java.awt.image.ImageObserver
     * @see      java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     */
    public boolean drawImage(Image img, int x, int y,
                             ImageObserver observer) {
        // System.err.println("drawImage:x, y");

        int width = img.getWidth(observer);
        int height = img.getHeight(observer);

        if (width == -1 || height == -1) {
            return false;
        }

        return drawImage(img, x, y, width, height, observer);
    }

    private BufferedImage buildBufferedImage(Dimension size) {
        return new BufferedImage(size.width, size.height,
                                 BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Draws as much of the specified image as has already been scaled
     * to fit inside the specified rectangle.
     * <p>
     * The image is drawn inside the specified rectangle of this
     * graphics context's coordinate space, and is scaled if
     * necessary. Transparent pixels do not affect whatever pixels
     * are already there.
     * <p>
     * This method returns immediately in all cases, even if the
     * entire image has not yet been scaled, dithered, and converted
     * for the current output device.
     * If the current output representation is not yet complete, then
     * <code>drawImage</code> returns <code>false</code>. As more of
     * the image becomes available, the process that draws the image notifies
     * the image observer by calling its <code>imageUpdate</code> method.
     * <p>
     * A scaled version of an image will not necessarily be
     * available immediately just because an unscaled version of the
     * image has been constructed for this output device.  Each size of
     * the image may be cached separately and generated from the original
     * data in a separate image production sequence.
     * @param    img    the specified image to be drawn.
     * @param    x      the <i>x</i> coordinate.
     * @param    y      the <i>y</i> coordinate.
     * @param    width  the width of the rectangle.
     * @param    height the height of the rectangle.
     * @param    observer    object to be notified as more of
     * the image is converted.
     * @return true if the image was drawn
     * @see      java.awt.Image
     * @see      java.awt.image.ImageObserver
     * @see      java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     */
    public boolean drawImage(Image img, int x, int y, int width, int height,
                               ImageObserver observer) {
        //System.out.println("drawImage x=" + x + " y=" + y + " width=" + width + " height=" + height + " image=" + img.toString());
        // first we look to see if we've already added this image to
        // the pdf document. If so, we just reuse the reference;
        // otherwise we have to build a FopImage and add it to the pdf
        // document
        PDFXObject imageInfo = pdfDoc.getImage("TempImage:" + img.toString());
        if (imageInfo == null) {
            // OK, have to build and add a PDF image

            Dimension size = new Dimension(width, height);
            BufferedImage buf = buildBufferedImage(size);

            java.awt.Graphics2D g = buf.createGraphics();
            g.setComposite(AlphaComposite.SrcOver);
            g.setBackground(new Color(1, 1, 1, 0));
            g.setPaint(new Color(1, 1, 1, 0));
            g.fillRect(0, 0, width, height);
            g.clip(new Rectangle(0, 0, buf.getWidth(), buf.getHeight()));
            g.setComposite(gc.getComposite());

            if (!g.drawImage(img, 0, 0, buf.getWidth(), buf.getHeight(), observer)) {
                return false;
            }
            g.dispose();

            final byte[] result = new byte[buf.getWidth() * buf.getHeight() * 3 /*for RGB*/];
            byte[] mask = new byte[buf.getWidth() * buf.getHeight()];
            boolean hasMask = false;
            //boolean binaryMask = true;

            Raster raster = buf.getData();
            DataBuffer bd = raster.getDataBuffer();

            int count = 0;
            int maskpos = 0;
            int[] iarray;
            int i, j, val, alpha;
            switch (bd.getDataType()) {
                case DataBuffer.TYPE_INT:
                int[][] idata = ((DataBufferInt)bd).getBankData();
                for (i = 0; i < idata.length; i++) {
                    iarray = idata[i];
                    for (j = 0; j < iarray.length; j++) {
                        val = iarray[j];
                        alpha = val >>> 24;
                        mask[maskpos++] = (byte)(alpha & 0xFF);
                        if (alpha != 255) {
                            hasMask = true;
                        }
                        result[count++] = (byte)((val >> 16) & 0xFF);
                        result[count++] = (byte)((val >> 8) & 0xFF);
                        result[count++] = (byte)((val) & 0xFF);
                    }
                }
                break;
                default:
                // error
                break;
            }
            String ref = null;
            if (hasMask) {
                // if the mask is binary then we could convert it into a bitmask
                BitmapImage fopimg = new BitmapImage("TempImageMask:"
                                             + img.toString(), buf.getWidth(),
                                             buf.getHeight(), mask, null);
                fopimg.setColorSpace(new PDFColorSpace(PDFColorSpace.DEVICE_GRAY));
                PDFXObject xobj = pdfDoc.addImage(resourceContext, fopimg);
                ref = xobj.referencePDF();

                if (outputStream != null) {
                    try {
                        this.pdfDoc.output(outputStream);
                    } catch (IOException ioe) {
                        // ignore exception, will be thrown again later
                    }
                }
            } else {
                mask = null;
            }

            BitmapImage fopimg = new BitmapImage("TempImage:"
                                          + img.toString(), buf.getWidth(),
                                          buf.getHeight(), result, ref);
            fopimg.setTransparent(new PDFColor(255, 255, 255));
            imageInfo = pdfDoc.addImage(resourceContext, fopimg);
            //int xObjectNum = imageInfo.getXNumber();

            if (outputStream != null) {
                try {
                    this.pdfDoc.output(outputStream);
                } catch (IOException ioe) {
                    // ignore exception, will be thrown again later
                }
            }
        } else {
            resourceContext.getPDFResources().addXObject(imageInfo);
        }

        // now do any transformation required and add the actual image
        // placement instance
        AffineTransform at = getTransform();
        double[] matrix = new double[6];
        at.getMatrix(matrix);
        currentStream.write("q\n");
        Shape imclip = getClip();
        writeClip(imclip);
        if (!at.isIdentity()) {
            currentStream.write("" + matrix[0] + " " + matrix[1] + " "
                                + matrix[2] + " " + matrix[3] + " "
                                + matrix[4] + " " + matrix[5] + " cm\n");
        }
        currentStream.write("" + width + " 0 0 " + (-height) + " " + x
                            + " " + (y + height) + " cm\n" + "/Im"
                            + imageInfo.getXNumber() + " Do\nQ\n");
        return true;
    }

    /**
     * Disposes of this graphics context and releases
     * any system resources that it is using.
     * A <code>Graphics</code> object cannot be used after
     * <code>dispose</code>has been called.
     * <p>
     * When a Java program runs, a large number of <code>Graphics</code>
     * objects can be created within a short time frame.
     * Although the finalization process of the garbage collector
     * also disposes of the same system resources, it is preferable
     * to manually free the associated resources by calling this
     * method rather than to rely on a finalization process which
     * may not run to completion for a long period of time.
     * <p>
     * Graphics objects which are provided as arguments to the
     * <code>paint</code> and <code>update</code> methods
     * of components are automatically released by the system when
     * those methods return. For efficiency, programmers should
     * call <code>dispose</code> when finished using
     * a <code>Graphics</code> object only if it was created
     * directly from a component or another <code>Graphics</code> object.
     * @see         java.awt.Graphics#finalize
     * @see         java.awt.Component#paint
     * @see         java.awt.Component#update
     * @see         java.awt.Component#getGraphics
     * @see         java.awt.Graphics#create
     */
    public void dispose() {
        // System.out.println("dispose");
        pdfDoc = null;
        fontInfo = null;
        currentStream = null;
        currentFontName = null;
    }

    /**
     * Strokes the outline of a <code>Shape</code> using the settings of the
     * current <code>Graphics2D</code> context.  The rendering attributes
     * applied include the <code>Clip</code>, <code>Transform</code>,
     * <code>Paint</code>, <code>Composite</code> and
     * <code>Stroke</code> attributes.
     * @param s the <code>Shape</code> to be rendered
     * @see #setStroke
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #transform
     * @see #setTransform
     * @see #clip
     * @see #setClip
     * @see #setComposite
     */
    public void draw(Shape s) {
        // System.out.println("draw(Shape)");
        AffineTransform trans = getTransform();
        double[] tranvals = new double[6];
        trans.getMatrix(tranvals);

        Shape imclip = getClip();
        boolean newClip = graphicsState.checkClip(imclip);
        boolean newTransform = graphicsState.checkTransform(trans)
                               && !trans.isIdentity();

        if (newClip || newTransform) {
            currentStream.write("q\n");
            graphicsState.push();
            if (newClip) {
                writeClip(imclip);
            }
            if (newTransform) {
                currentStream.write(PDFNumber.doubleOut(tranvals[0], 5) + " "
                            + PDFNumber.doubleOut(tranvals[1], 5) + " "
                            + PDFNumber.doubleOut(tranvals[2], 5) + " "
                            + PDFNumber.doubleOut(tranvals[3], 5) + " "
                            + PDFNumber.doubleOut(tranvals[4], 5) + " "
                            + PDFNumber.doubleOut(tranvals[5], 5) + " cm\n");
            }
        }

        Color c;
        c = getColor();
        if (c.getAlpha() == 0) {
            return;
        }
        if (c.getAlpha() != 255) {
            Map vals = new java.util.HashMap();
            vals.put(PDFGState.GSTATE_ALPHA_STROKE, new Float(c.getAlpha() / 255f));
            PDFGState gstate = pdfDoc.getFactory().makeGState(
                    vals, graphicsState.getGState());
            //gstate.setAlpha(c.getAlpha() / 255f, false);
            resourceContext.addGState(gstate);
            currentStream.write("/" + gstate.getName() + " gs\n");
        }

        applyColor(c, false);

        applyPaint(getPaint(), false);
        applyStroke(getStroke());

        PathIterator iter = s.getPathIterator(new AffineTransform());
        while (!iter.isDone()) {
            double vals[] = new double[6];
            int type = iter.currentSegment(vals);
            switch (type) {
            case PathIterator.SEG_CUBICTO:
                currentStream.write(PDFNumber.doubleOut(vals[0], 5) + " "
                                    + PDFNumber.doubleOut(vals[1], 5) + " "
                                    + PDFNumber.doubleOut(vals[2], 5) + " "
                                    + PDFNumber.doubleOut(vals[3], 5) + " "
                                    + PDFNumber.doubleOut(vals[4], 5) + " "
                                    + PDFNumber.doubleOut(vals[5], 5) + " c\n");
                break;
            case PathIterator.SEG_LINETO:
                currentStream.write(PDFNumber.doubleOut(vals[0], 5) + " "
                                    + PDFNumber.doubleOut(vals[1], 5) + " l\n");
                break;
            case PathIterator.SEG_MOVETO:
                currentStream.write(PDFNumber.doubleOut(vals[0], 5) + " "
                                    + PDFNumber.doubleOut(vals[1], 5) + " m\n");
                break;
            case PathIterator.SEG_QUADTO:
                currentStream.write(PDFNumber.doubleOut(vals[0], 5) + " "
                                    + PDFNumber.doubleOut(vals[1], 5) + " "
                                    + PDFNumber.doubleOut(vals[2], 5) + " "
                                    + PDFNumber.doubleOut(vals[3], 5) + " y\n");
                break;
            case PathIterator.SEG_CLOSE:
                currentStream.write("h\n");
                break;
            default:
                break;
            }
            iter.next();
        }
        doDrawing(false, true, false);
        if (newClip || newTransform) {
            currentStream.write("Q\n");
            graphicsState.pop();
        }
    }

/*
    // in theory we could set the clip using these methods
    // it doesn't seem to improve the file sizes much
    // and makes everything more complicated

    Shape lastClip = null;

    public void clip(Shape cl) {
        super.clip(cl);
        Shape newClip = getClip();
        if (newClip == null || lastClip == null
                || !(new Area(newClip).equals(new Area(lastClip)))) {
        graphicsState.setClip(newClip);
        writeClip(newClip);
        }

        lastClip = newClip;
    }

    public void setClip(Shape cl) {
        super.setClip(cl);
        Shape newClip = getClip();
        if (newClip == null || lastClip == null
                || !(new Area(newClip).equals(new Area(lastClip)))) {
        for (int count = graphicsState.getStackLevel(); count > baseLevel; count--) {
            currentStream.write("Q\n");
        }
        graphicsState.restoreLevel(baseLevel);
        currentStream.write("q\n");
        graphicsState.push();
        if (newClip != null) {
            graphicsState.setClip(newClip);
        }
        writeClip(newClip);
        }

        lastClip = newClip;
    }
*/

    /**
     * Set the clipping shape for future PDF drawing in the current graphics state.
     * This sets creates and writes a clipping shape that will apply
     * to future drawings in the current graphics state.
     *
     * @param s the clipping shape
     */
    protected void writeClip(Shape s) {
        if (s == null) {
            return;
        }
        PathIterator iter = s.getPathIterator(getTransform());
        while (!iter.isDone()) {
            double vals[] = new double[6];
            int type = iter.currentSegment(vals);
            switch (type) {
            case PathIterator.SEG_CUBICTO:
                currentStream.write(PDFNumber.doubleOut(vals[0]) + " "
                                    + PDFNumber.doubleOut(vals[1]) + " "
                                    + PDFNumber.doubleOut(vals[2]) + " "
                                    + PDFNumber.doubleOut(vals[3]) + " "
                                    + PDFNumber.doubleOut(vals[4]) + " "
                                    + PDFNumber.doubleOut(vals[5]) + " c\n");
                break;
            case PathIterator.SEG_LINETO:
                currentStream.write(PDFNumber.doubleOut(vals[0]) + " "
                                    + PDFNumber.doubleOut(vals[1]) + " l\n");
                break;
            case PathIterator.SEG_MOVETO:
                currentStream.write(PDFNumber.doubleOut(vals[0]) + " "
                                    + PDFNumber.doubleOut(vals[1]) + " m\n");
                break;
            case PathIterator.SEG_QUADTO:
                currentStream.write(PDFNumber.doubleOut(vals[0]) + " "
                                    + PDFNumber.doubleOut(vals[1]) + " "
                                    + PDFNumber.doubleOut(vals[2]) + " "
                                    + PDFNumber.doubleOut(vals[3]) + " y\n");
                break;
            case PathIterator.SEG_CLOSE:
                currentStream.write("h\n");
                break;
            default:
                break;
            }
            iter.next();
        }
        // clip area
        currentStream.write("W\n");
        currentStream.write("n\n");
    }

    /**
     * Apply the java Color to PDF.
     * This converts the java colour to a PDF colour and
     * sets it for the next drawing.
     *
     * @param col the java colour
     * @param fill true if the colour will be used for filling
     */
    protected void applyColor(Color col, boolean fill) {
        Color c = col;
        if (c.getColorSpace().getType()
                == ColorSpace.TYPE_RGB) {
            PDFColor currentColour = new PDFColor(c.getRed(), c.getGreen(),
                                         c.getBlue());
            currentStream.write(currentColour.getColorSpaceOut(fill));
        } else if (c.getColorSpace().getType()
                   == ColorSpace.TYPE_CMYK) {
            float[] cComps = c.getColorComponents(new float[3]);
            double[] cmyk = new double[3];
            for (int i = 0; i < 3; i++) {
                // convert the float elements to doubles for pdf
                cmyk[i] = cComps[i];
            }
            PDFColor currentColour = new PDFColor(cmyk[0], cmyk[1], cmyk[2], cmyk[3]);
            currentStream.write(currentColour.getColorSpaceOut(fill));
        } else if (c.getColorSpace().getType()
                   == ColorSpace.TYPE_2CLR) {
            // used for black/magenta
            float[] cComps = c.getColorComponents(new float[1]);
            double[] blackMagenta = new double[1];
            for (int i = 0; i < 1; i++) {
                blackMagenta[i] = cComps[i];
            }
            //PDFColor  currentColour = new PDFColor(blackMagenta[0], blackMagenta[1]);
            //currentStream.write(currentColour.getColorSpaceOut(fill));
        } else {
            System.err.println("Color Space not supported by PDFGraphics2D");
        }
    }

    /**
     * Apply the java paint to the PDF.
     * This takes the java paint sets up the appropraite PDF commands
     * for the drawing with that paint.
     * Currently this supports the gradients and patterns from batik.
     *
     * @param paint the paint to convert to PDF
     * @param fill true if the paint should be set for filling
     */
    protected void applyPaint(Paint paint, boolean fill) {

        if (paint instanceof LinearGradientPaint) {
            LinearGradientPaint gp = (LinearGradientPaint)paint;
            Color[] cols = gp.getColors();
            float[] fractions = gp.getFractions();
            Point2D p1 = gp.getStartPoint();
            Point2D p2 = gp.getEndPoint();
            //MultipleGradientPaint.CycleMethodEnum cycenum = gp.getCycleMethod();
            //boolean cyclic = (cycenum == MultipleGradientPaint.REPEAT);
            AffineTransform transform = graphicsState.getTransform();
            transform.concatenate(gp.getTransform());
            transform.concatenate(getTransform());

            p1 = transform.transform(p1, null);
            p2 = transform.transform(p2, null);

            List theCoords = new java.util.ArrayList();
            theCoords.add(new Double(p1.getX()));
            theCoords.add(new Double(p1.getY()));
            theCoords.add(new Double(p2.getX()));
            theCoords.add(new Double(p2.getY()));

            List theExtend = new java.util.ArrayList();
            theExtend.add(new Boolean(true));
            theExtend.add(new Boolean(true));

            List theDomain = new java.util.ArrayList();
            theDomain.add(new Double(0));
            theDomain.add(new Double(1));

            List theEncode = new java.util.ArrayList();
            theEncode.add(new Double(0));
            theEncode.add(new Double(1));
            theEncode.add(new Double(0));
            theEncode.add(new Double(1));

            List theBounds = new java.util.ArrayList();

            List someColors = new java.util.ArrayList();

            for (int count = 0; count < cols.length; count++) {
                Color c1 = cols[count];
                PDFColor color1 = new PDFColor(c1.getRed(), c1.getGreen(),
                                               c1.getBlue());
                someColors.add(color1);
                if (count > 0 && count < cols.length - 1) {
                    theBounds.add(new Double(fractions[count]));
                }
            }

            PDFColorSpace aColorSpace = new PDFColorSpace(PDFColorSpace.DEVICE_RGB);
            PDFPattern myPat = pdfDoc.getFactory().makeGradient(
                    resourceContext, false, aColorSpace,
                    someColors, theBounds, theCoords);
            currentStream.write(myPat.getColorSpaceOut(fill));

        } else if (paint instanceof RadialGradientPaint) {
            RadialGradientPaint rgp = (RadialGradientPaint)paint;

            double ar = rgp.getRadius();
            Point2D ac = rgp.getCenterPoint();
            Point2D af = rgp.getFocusPoint();
            AffineTransform transform = graphicsState.getTransform();
            AffineTransform gradt = rgp.getTransform();
            transform.concatenate(gradt);

            // find largest scaling for the radius
            double scale = gradt.getScaleX();
            if (gradt.getScaleY() > scale) {
                scale = gradt.getScaleY();
            }
            ar = ar * scale;
            ac = transform.transform(ac, null);
            af = transform.transform(af, null);

            List theCoords = new java.util.ArrayList();
            // the center point af must be within the circle with
            // radius ar centered at ac
            theCoords.add(new Double(af.getX()));
            theCoords.add(new Double(af.getY()));
            theCoords.add(new Double(0));
            theCoords.add(new Double(ac.getX())); // Fx
            theCoords.add(new Double(ac.getY())); // Fy
            theCoords.add(new Double(ar));

            Color[] cols = rgp.getColors();
            List someColors = new java.util.ArrayList();
            for (int count = 0; count < cols.length; count++) {
                Color cc = cols[count];
                someColors.add(new PDFColor(cc.getRed(), cc.getGreen(), cc.getBlue()));
            }

            float[] fractions = rgp.getFractions();
            List theBounds = new java.util.ArrayList();
            for (int count = 1; count < fractions.length - 1; count++) {
                float offset = fractions[count];
                theBounds.add(new Double(offset));
            }
            PDFColorSpace colSpace = new PDFColorSpace(PDFColorSpace.DEVICE_RGB);
            PDFPattern myPat = pdfDoc.getFactory().makeGradient(
                                    resourceContext, true, colSpace,
                                    someColors, theBounds, theCoords);

            currentStream.write(myPat.getColorSpaceOut(fill));

        } else if (paint instanceof PatternPaint) {
            PatternPaint pp = (PatternPaint)paint;
            createPattern(pp, fill);
        }
    }

    private void createPattern(PatternPaint pp, boolean fill) {
        Rectangle2D rect = pp.getPatternRect();

        FontInfo fontInfo = new FontInfo();
        FontSetup.setup(fontInfo, null);

        PDFResources res = pdfDoc.getFactory().makeResources();
        PDFResourceContext context = new PDFResourceContext(res);
        PDFGraphics2D pattGraphic = new PDFGraphics2D(textAsShapes, fontInfo,
                                        pdfDoc, context, pageRef,
                                        "", 0);
        pattGraphic.gc = (GraphicContext)this.gc.clone();
        pattGraphic.gc.validateTransformStack();
        pattGraphic.setOutputStream(outputStream);

        GraphicsNode gn = pp.getGraphicsNode();
        gn.paint(pattGraphic);

        StringWriter pattStream = new StringWriter();
        pattStream.write("q\n");

        // this makes the pattern the right way up, since
        // it is outside the original transform around the
        // whole svg document
        pattStream.write("1 0 0 -1 0 " + (rect.getHeight() + rect.getY()) + " cm\n");

        pattStream.write(pattGraphic.getString());
        pattStream.write("Q");

        List bbox = new java.util.ArrayList();
        bbox.add(new Double(0));
        bbox.add(new Double(0));
        bbox.add(new Double(rect.getWidth() + rect.getX()));
        bbox.add(new Double(rect.getHeight() + rect.getY()));

        List translate = new java.util.ArrayList();
        AffineTransform pattt = pp.getPatternTransform();
        pattt.translate(rect.getWidth() + rect.getX(), rect.getHeight() + rect.getY());
        double[] flatmatrix = new double[6];
        pattt.getMatrix(flatmatrix);
        translate.add(new Double(flatmatrix[0]));
        translate.add(new Double(flatmatrix[1]));
        translate.add(new Double(flatmatrix[2]));
        translate.add(new Double(flatmatrix[3]));
        translate.add(new Double(flatmatrix[4]));
        translate.add(new Double(flatmatrix[5]));

        /** @todo see if pdfDoc and res can be linked here,
        (currently res <> PDFDocument's resources) so addFonts() 
        can be moved to PDFDocument class */
        res.addFonts(pdfDoc, fontInfo);

        PDFPattern myPat = pdfDoc.getFactory().makePattern(
                                resourceContext, 1, res, 1, 1, bbox,
                                rect.getWidth(), rect.getHeight(),
                                translate, null, pattStream.getBuffer());

        currentStream.write(myPat.getColorSpaceOut(fill));

        PDFAnnotList annots = context.getAnnotations();
        if (annots != null) {
            this.pdfDoc.addObject(annots);
        }

        if (outputStream != null) {
            try {
                this.pdfDoc.output(outputStream);
            } catch (IOException ioe) {
                // ignore exception, will be thrown again later
            }
        }
    }

    /**
     * Apply the stroke to the PDF.
     * This takes the java stroke and outputs the appropriate settings
     * to the PDF so that the stroke attributes are handled.
     *
     * @param stroke the java stroke
     */
    protected void applyStroke(Stroke stroke) {
        if (stroke instanceof BasicStroke) {
            BasicStroke bs = (BasicStroke)stroke;

            float[] da = bs.getDashArray();
            if (da != null) {
                currentStream.write("[");
                for (int count = 0; count < da.length; count++) {
                    if (((int)da[count]) == 0) {
                        // the dasharray units in pdf are (whole) numbers
                        // in user space units, cannot be 0
                        currentStream.write("1");
                    } else {
                        currentStream.write("" + ((int)da[count]));
                    }
                    if (count < da.length - 1) {
                        currentStream.write(" ");
                    }
                }
                currentStream.write("] ");
                float offset = bs.getDashPhase();
                currentStream.write(((int)offset) + " d\n");
            }
            int ec = bs.getEndCap();
            switch (ec) {
            case BasicStroke.CAP_BUTT:
                currentStream.write(0 + " J\n");
                break;
            case BasicStroke.CAP_ROUND:
                currentStream.write(1 + " J\n");
                break;
            case BasicStroke.CAP_SQUARE:
                currentStream.write(2 + " J\n");
                break;
            }

            int lj = bs.getLineJoin();
            switch (lj) {
            case BasicStroke.JOIN_MITER:
                currentStream.write(0 + " j\n");
                break;
            case BasicStroke.JOIN_ROUND:
                currentStream.write(1 + " j\n");
                break;
            case BasicStroke.JOIN_BEVEL:
                currentStream.write(2 + " j\n");
                break;
            }
            float lw = bs.getLineWidth();
            currentStream.write(PDFNumber.doubleOut(lw) + " w\n");

            float ml = bs.getMiterLimit();
            currentStream.write(PDFNumber.doubleOut(ml) + " M\n");
        }
    }

    /**
     * Renders a {@link RenderedImage},
     * applying a transform from image
     * space into user space before drawing.
     * The transformation from user space into device space is done with
     * the current <code>Transform</code> in the <code>Graphics2D</code>.
     * The specified transformation is applied to the image before the
     * transform attribute in the <code>Graphics2D</code> context is applied.
     * The rendering attributes applied include the <code>Clip</code>,
     * <code>Transform</code>, and <code>Composite</code> attributes. Note
     * that no rendering is done if the specified transform is
     * noninvertible.
     * @param img the image to be rendered
     * @param xform the transformation from image space into user space
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        //System.out.println("drawRenderedImage");
    }

    /**
     * Renders a
     * {@link RenderableImage},
     * applying a transform from image space into user space before drawing.
     * The transformation from user space into device space is done with
     * the current <code>Transform</code> in the <code>Graphics2D</code>.
     * The specified transformation is applied to the image before the
     * transform attribute in the <code>Graphics2D</code> context is applied.
     * The rendering attributes applied include the <code>Clip</code>,
     * <code>Transform</code>, and <code>Composite</code> attributes. Note
     * that no rendering is done if the specified transform is
     * noninvertible.
     * <p>
     * Rendering hints set on the <code>Graphics2D</code> object might
     * be used in rendering the <code>RenderableImage</code>.
     * If explicit control is required over specific hints recognized by a
     * specific <code>RenderableImage</code>, or if knowledge of which hints
     * are used is required, then a <code>RenderedImage</code> should be
     * obtained directly from the <code>RenderableImage</code>
     * and rendered using
     * {@link #drawRenderedImage(RenderedImage, AffineTransform) drawRenderedImage}.
     * @param img the image to be rendered
     * @param xform the transformation from image space into user space
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     * @see #drawRenderedImage
     */
    public void drawRenderableImage(RenderableImage img,
                                    AffineTransform xform) {
        //System.out.println("drawRenderableImage");
    }

    /**
     * Renders the text specified by the specified <code>String</code>,
     * using the current <code>Font</code> and <code>Paint</code> attributes
     * in the <code>Graphics2D</code> context.
     * The baseline of the first character is at position
     * (<i>x</i>,&nbsp;<i>y</i>) in the User Space.
     * The rendering attributes applied include the <code>Clip</code>,
     * <code>Transform</code>, <code>Paint</code>, <code>Font</code> and
     * <code>Composite</code> attributes. For characters in script systems
     * such as Hebrew and Arabic, the glyphs can be rendered from right to
     * left, in which case the coordinate supplied is the location of the
     * leftmost character on the baseline.
     * @param s the <code>String</code> to be rendered
     * @param x the coordinate where the <code>String</code>
     * should be rendered
     * @param y the coordinate where the <code>String</code>
     * should be rendered
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see java.awt.Graphics#setFont
     * @see #setTransform
     * @see #setComposite
     * @see #setClip
     */
    public void drawString(String s, float x, float y) {
        // System.out.println("drawString(String)");

        Font fontState;
        if (ovFontState == null) {
            java.awt.Font gFont = getFont();
            String n = gFont.getFamily();
            if (n.equals("sanserif")) {
                n = "sans-serif";
            }
            int siz = gFont.getSize();
            String style = gFont.isItalic() ? "italic" : "normal";
            int weight = gFont.isBold() ? Font.BOLD : Font.NORMAL;
            String fname = fontInfo.fontLookup(n, style, weight);
            FontMetrics metrics = fontInfo.getMetricsFor(fname);
            fontState = new Font(fname, metrics, siz * 1000);
        } else {
            FontMetrics metrics = fontInfo.getMetricsFor(ovFontState.getFontName());
            fontState = new Font(ovFontState.getFontName(),
                                      metrics, ovFontState.getFontSize());
            ovFontState = null;
        }
        String name;
        float size;
        name = fontState.getFontName();
        size = (float)fontState.getFontSize() / 1000f;

        if ((!name.equals(this.currentFontName))
                || (size != this.currentFontSize)) {
            this.currentFontName = name;
            this.currentFontSize = size;
            currentStream.write("/" + name + " " + size + " Tf\n");

        }

        currentStream.write("q\n");

        Shape imclip = getClip();
        writeClip(imclip);
        Color c = getColor();
        applyColor(c, true);
        applyPaint(getPaint(), true);
        int salpha = c.getAlpha();

        if (salpha != 255) {
            Map vals = new java.util.HashMap();
            vals.put(PDFGState.GSTATE_ALPHA_NONSTROKE, new Float(salpha / 255f));
            PDFGState gstate = pdfDoc.getFactory().makeGState(
                    vals, graphicsState.getGState());
            resourceContext.addGState(gstate);
            currentStream.write("/" + gstate.getName() + " gs\n");
        }

        currentStream.write("BT\n");

        Map kerning = null;
        boolean kerningAvailable = false;

        kerning = fontState.getKerning();
        if (kerning != null && !kerning.isEmpty()) {
            kerningAvailable = true;
        }

        // This assumes that *all* CIDFonts use a /ToUnicode mapping
        boolean useMultiByte = false;
        org.apache.fop.fonts.Typeface f =
            (org.apache.fop.fonts.Typeface)fontInfo.getFonts().get(name);
        if (f instanceof LazyFont) {
            if (((LazyFont) f).getRealFont() instanceof CIDFont) {
                useMultiByte = true;
            }
        } else if (f instanceof CIDFont) {
            useMultiByte = true;
        }

        // String startText = useMultiByte ? "<FEFF" : "(";
        String startText = useMultiByte ? "<" : "(";
        String endText = useMultiByte ? "> " : ") ";

        AffineTransform trans = getTransform();
        trans.translate(x, y);
        double[] vals = new double[6];
        trans.getMatrix(vals);

        currentStream.write(PDFNumber.doubleOut(vals[0]) + " "
                            + PDFNumber.doubleOut(vals[1]) + " "
                            + PDFNumber.doubleOut(vals[2]) + " "
                            + PDFNumber.doubleOut(vals[3]) + " "
                            + PDFNumber.doubleOut(vals[4]) + " "
                            + PDFNumber.doubleOut(vals[5]) + " cm\n");
        currentStream.write("1 0 0 -1 0 0 Tm [" + startText);

        int l = s.length();

        for (int i = 0; i < l; i++) {
            char ch = fontState.mapChar(s.charAt(i));

            if (!useMultiByte) {
                if (ch > 127) {
                    currentStream.write("\\");
                    currentStream.write(Integer.toOctalString((int)ch));
                } else {
                    switch (ch) {
                    case '(':
                    case ')':
                    case '\\':
                        currentStream.write("\\");
                        break;
                    }
                    currentStream.write(ch);
                }
            } else {
                currentStream.write(PDFText.toUnicodeHex(ch));
            }

            if (kerningAvailable && (i + 1) < l) {
                addKerning(currentStream, (new Integer((int)ch)),
                           (new Integer((int)fontState.mapChar(s.charAt(i + 1)))),
                           kerning, startText, endText);
            }

        }
        currentStream.write(endText);


        currentStream.write("] TJ\n");

        currentStream.write("ET\n");
        currentStream.write("Q\n");
    }

    private void addKerning(StringWriter buf, Integer ch1, Integer ch2,
                            Map kerning, String startText,
                            String endText) {
        Map kernPair = (Map)kerning.get(ch1);

        if (kernPair != null) {
            Integer width = (Integer)kernPair.get(ch2);
            if (width != null) {
                currentStream.write(endText + (-width.intValue()) + " " + startText);
            }
        }
    }

    /**
     * Renders the text of the specified iterator, using the
     * <code>Graphics2D</code> context's current <code>Paint</code>. The
     * iterator must specify a font
     * for each character. The baseline of the
     * first character is at position (<i>x</i>,&nbsp;<i>y</i>) in the
     * User Space.
     * The rendering attributes applied include the <code>Clip</code>,
     * <code>Transform</code>, <code>Paint</code>, and
     * <code>Composite</code> attributes.
     * For characters in script systems such as Hebrew and Arabic,
     * the glyphs can be rendered from right to left, in which case the
     * coordinate supplied is the location of the leftmost character
     * on the baseline.
     * @param iterator the iterator whose text is to be rendered
     * @param x the coordinate where the iterator's text is to be
     * rendered
     * @param y the coordinate where the iterator's text is to be
     * rendered
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #setTransform
     * @see #setComposite
     * @see #setClip
     */
    public void drawString(AttributedCharacterIterator iterator, float x,
                           float y) {
        System.err.println("drawString(AttributedCharacterIterator)");

        Font fontState = null;

        Shape imclip = getClip();
        writeClip(imclip);
        Color c = getColor();
        applyColor(c, true);
        applyPaint(getPaint(), true);

        boolean fill = true;
        boolean stroke = false;
        if (true) {
            Stroke currentStroke = getStroke();
            stroke = true;
            applyStroke(currentStroke);
            applyColor(c, false);
            applyPaint(getPaint(), false);
        }

        currentStream.write("BT\n");

        // set text rendering mode:
        // 0 - fill, 1 - stroke, 2 - fill then stroke
        int textr = 0;
        if (fill && stroke) {
            textr = 2;
        } else if (stroke) {
            textr = 1;
        }
        currentStream.write(textr + " Tr\n");

        AffineTransform trans = getTransform();
        trans.translate(x, y);
        double[] vals = new double[6];
        trans.getMatrix(vals);

        for (char ch = iterator.first(); ch != CharacterIterator.DONE;
                ch = iterator.next()) {
            //Map attr = iterator.getAttributes();

            String name = fontState.getFontName();
            int size = fontState.getFontSize();
            if ((!name.equals(this.currentFontName))
                    || (size != this.currentFontSize)) {
                this.currentFontName = name;
                this.currentFontSize = size;
                currentStream.write("/" + name + " " + (size / 1000)
                                    + " Tf\n");

            }

            currentStream.write(PDFNumber.doubleOut(vals[0]) + " "
                                + PDFNumber.doubleOut(vals[1]) + " "
                                + PDFNumber.doubleOut(vals[2]) + " "
                                + PDFNumber.doubleOut(vals[3]) + " "
                                + PDFNumber.doubleOut(vals[4]) + " "
                                + PDFNumber.doubleOut(vals[5]) + " Tm (" + ch
                                + ") Tj\n");
        }

        currentStream.write("ET\n");
    }

    /**
     * Fills the interior of a <code>Shape</code> using the settings of the
     * <code>Graphics2D</code> context. The rendering attributes applied
     * include the <code>Clip</code>, <code>Transform</code>,
     * <code>Paint</code>, and <code>Composite</code>.
     * @param s the <code>Shape</code> to be filled
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public void fill(Shape s) {
        // System.err.println("fill");
        Color c;
        c = getBackground();
        if (c.getAlpha() == 0) {
            c = getColor();
            if (c.getAlpha() == 0) {
                return;
            }
        }
        Shape imclip = getClip();
        boolean newState = graphicsState.checkClip(imclip);

        if (newState) {
            currentStream.write("q\n");
            graphicsState.push();
            writeClip(imclip);
            graphicsState.setClip(imclip);
        }

        if (c.getAlpha() != 255) {
            Map vals = new java.util.HashMap();
            vals.put(PDFGState.GSTATE_ALPHA_NONSTROKE, new Float(c.getAlpha() / 255f));
            PDFGState gstate = pdfDoc.getFactory().makeGState(
                        vals, graphicsState.getGState());
            resourceContext.addGState(gstate);
            currentStream.write("/" + gstate.getName() + " gs\n");
        }

        c = getColor();
        if (graphicsState.setColor(c)) {
            applyColor(c, true);
        }
        c = getBackground();
        if (graphicsState.setBackColor(c)) {
            applyColor(c, false);
        }

        Paint paint = getPaint();
        if (graphicsState.setPaint(paint)) {
            applyPaint(paint, true);
        }

        PathIterator iter = s.getPathIterator(getTransform());
        while (!iter.isDone()) {
            double vals[] = new double[6];
            int type = iter.currentSegment(vals);
            switch (type) {
            case PathIterator.SEG_CUBICTO:
                currentStream.write(PDFNumber.doubleOut(vals[0], 5) + " "
                                    + PDFNumber.doubleOut(vals[1], 5) + " "
                                    + PDFNumber.doubleOut(vals[2], 5) + " "
                                    + PDFNumber.doubleOut(vals[3], 5) + " "
                                    + PDFNumber.doubleOut(vals[4], 5) + " "
                                    + PDFNumber.doubleOut(vals[5], 5) + " c\n");
                break;
            case PathIterator.SEG_LINETO:
                currentStream.write(PDFNumber.doubleOut(vals[0], 5) + " "
                                    + PDFNumber.doubleOut(vals[1], 5) + " l\n");
                break;
            case PathIterator.SEG_MOVETO:
                currentStream.write(PDFNumber.doubleOut(vals[0], 5) + " "
                                    + PDFNumber.doubleOut(vals[1], 5) + " m\n");
                break;
            case PathIterator.SEG_QUADTO:
                currentStream.write(PDFNumber.doubleOut(vals[0], 5) + " "
                                    + PDFNumber.doubleOut(vals[1], 5) + " "
                                    + PDFNumber.doubleOut(vals[2], 5) + " "
                                    + PDFNumber.doubleOut(vals[3], 5) + " y\n");
                break;
            case PathIterator.SEG_CLOSE:
                currentStream.write("h\n");
                break;
            default:
                break;
            }
            iter.next();
        }
        doDrawing(true, false,
                  iter.getWindingRule() == PathIterator.WIND_EVEN_ODD);
        if (newState) {
            currentStream.write("Q\n");
            graphicsState.pop();
        }
    }

    /**
     * Do the PDF drawing command.
     * This does the PDF drawing command according to fill
     * stroke and winding rule.
     *
     * @param fill true if filling the path
     * @param stroke true if stroking the path
     * @param nonzero true if using the non-zero winding rule
     */
    protected void doDrawing(boolean fill, boolean stroke, boolean nonzero) {
        if (fill) {
            if (stroke) {
                if (nonzero) {
                    currentStream.write("B*\n");
                } else {
                    currentStream.write("B\n");
                }
            } else {
                if (nonzero) {
                    currentStream.write("f*\n");
                } else {
                    currentStream.write("f\n");
                }
            }
        } else {
            // if (stroke)
            currentStream.write("S\n");
        }
    }

    /**
     * Returns the device configuration associated with this
     * <code>Graphics2D</code>.
     *
     * @return the PDF graphics configuration
     */
    public GraphicsConfiguration getDeviceConfiguration() {
        return new PDFGraphicsConfiguration();
    }

    /**
     * Used to create proper font metrics
     */
    private Graphics2D fmg;

    {
        BufferedImage bi = new BufferedImage(1, 1,
                                             BufferedImage.TYPE_INT_ARGB);

        fmg = bi.createGraphics();
    }

    /**
     * Gets the font metrics for the specified font.
     * @return    the font metrics for the specified font.
     * @param     f the specified font
     * @see       java.awt.Graphics#getFont
     * @see       java.awt.FontMetrics
     * @see       java.awt.Graphics#getFontMetrics()
     */
    public java.awt.FontMetrics getFontMetrics(java.awt.Font f) {
        return fmg.getFontMetrics(f);
    }

    /**
     * Sets the paint mode of this graphics context to alternate between
     * this graphics context's current color and the new specified color.
     * This specifies that logical pixel operations are performed in the
     * XOR mode, which alternates pixels between the current color and
     * a specified XOR color.
     * <p>
     * When drawing operations are performed, pixels which are the
     * current color are changed to the specified color, and vice versa.
     * <p>
     * Pixels that are of colors other than those two colors are changed
     * in an unpredictable but reversible manner; if the same figure is
     * drawn twice, then all pixels are restored to their original values.
     * @param     c1 the XOR alternation color
     */
    public void setXORMode(Color c1) {
        //System.out.println("setXORMode");
    }


    /**
     * Copies an area of the component by a distance specified by
     * <code>dx</code> and <code>dy</code>. From the point specified
     * by <code>x</code> and <code>y</code>, this method
     * copies downwards and to the right.  To copy an area of the
     * component to the left or upwards, specify a negative value for
     * <code>dx</code> or <code>dy</code>.
     * If a portion of the source rectangle lies outside the bounds
     * of the component, or is obscured by another window or component,
     * <code>copyArea</code> will be unable to copy the associated
     * pixels. The area that is omitted can be refreshed by calling
     * the component's <code>paint</code> method.
     * @param       x the <i>x</i> coordinate of the source rectangle.
     * @param       y the <i>y</i> coordinate of the source rectangle.
     * @param       width the width of the source rectangle.
     * @param       height the height of the source rectangle.
     * @param       dx the horizontal distance to copy the pixels.
     * @param       dy the vertical distance to copy the pixels.
     */
    public void copyArea(int x, int y, int width, int height, int dx,
                         int dy) {
        //System.out.println("copyArea");
    }

}