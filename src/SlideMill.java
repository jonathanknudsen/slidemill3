import com.drew.metadata.*;
import com.drew.imaging.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.text.*;

import javax.imageio.*;
import javax.imageio.stream.*;
import javax.imageio.metadata.*;
import javax.swing.*;


public class SlideMill extends Container implements Runnable {
  private static int kDefaultDelay = 10000; // 10s
  
  public static void main(String... args) {
    String fullscreenS = args[0];
    String directory = args[1];
    String delayS = args[2];

    boolean fullscreen = !fullscreenS.equals("no");
    
    int delay = kDefaultDelay;
    
    try { delay = Integer.parseInt(delayS); }
    catch (NumberFormatException nfe) {
      Log.log("SlideMill.main(): could not parse delay " + delayS);      
    }
    
    SlideMill sm = new SlideMill(directory, delay);
    //sm.getNextFilename();
    setupGraphics(sm, fullscreen);
    
    Thread t = new Thread(sm);
    t.start();
  }
  
  private static void setupGraphics(SlideMill sm, boolean fullscreen) {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getScreenDevices()[0];
    if (gd == null) {
      Log.log("SlideMill.main(): Could not locate GraphicsDevice.");
      System.exit(1);
    }

    JFrame f = new JFrame("SlideMill");
    f.setBounds(200, 200, 600, 600);
    f.setContentPane(sm);

    // Go to full screen.
    if (fullscreen)
      gd.setFullScreenWindow(f);

    // Set a transparent cursor.
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    Point hotSpot = new Point(0,0);
    BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT);
    Cursor invisibleCursor = toolkit.createCustomCursor(cursorImage, hotSpot, "InvisibleCursor");
    f.setCursor(invisibleCursor);

    f.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent ke) {
        System.exit(0);
      }
    });

    f.setVisible(true);
  }

  // Directories for images.
  private static String kNew =  "1new";
  private static String kPool = "2pool";
  private static String kDone = "3done";
  private static String kTmp = "4tmp";
  
  private String mBase;
  private int mDelay;
  
  private Random mRandom;

  private String mFilename, mPrefetchedFilename;
  private String mTimestamp, mPrefetchedTimestamp;
  private String mCaption, mPrefetchedCaption;
  private BufferedImage mImage, mPrefetchedImage;

  public SlideMill(String base, int delay) {
    mBase = base;
    mDelay = delay;
    
    mRandom = new Random();
  }

  public void paint(Graphics g) {
    if (mImage != null && mCaption == null)
      Log.log("SlideMill.paint(): no caption: " + mFilename);
    if (mImage != null && mTimestamp == null)
      Log.log("SlideMill.paint(): no timestamp: " + mFilename);

    Graphics2D g2 = (Graphics2D)g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    Dimension d = getSize();
    int sw = d.width;
    int sh = d.height;

    g2.setPaint(Color.black);
    g2.fillRect(0, 0, d.width, d.height);

    if (mImage != null) {
      int iwfull = mImage.getWidth();
      int ihfull = mImage.getHeight();
      double scale = 1.0;
      double scalew = (double)sw / (double)iwfull;
      double scaleh = (double)sh / (double)ihfull;
      scale = Math.min(scalew, scaleh); // * 1.10;
      int iw = (int)(scale * (double)iwfull);
      int ih = (int)(scale * (double)ihfull);
      int ix = (sw - iw) / 2;
      int iy = (sh - ih) / 2;
      g2.drawImage(mImage, ix, iy, iw, ih, Color.black, null);
    }

    drawMetadata(g2, sh);
  }

  private void drawMetadata(Graphics2D g2, int sh) {
    // Choose a font scaled for the screen size.
    int pointSize = sh / 22;
    Font font = new Font("Serif", Font.PLAIN, pointSize);
    g2.setFont(font);

    FontRenderContext frc = g2.getFontRenderContext();

    // Figure out line height.
    LineMetrics lm = font.getLineMetrics("Any string", frc);
    float fh = lm.getHeight();
    float fd = lm.getDescent();
    float fl = lm.getLeading();

    double radius = 48;
    double xpadding = 12;
    double ypadding = 4;

    int tx = (int)xpadding;
    int ty = sh - (int)fh - (int)fl - (int)fd;
    int cx = (int)xpadding;
    int cy = sh - (int)fl - (int)fd;

    if (mTimestamp != null)
      drawStringShadow(g2, mTimestamp, tx, ty, xpadding, ypadding, radius);
    if (mCaption != null)
      drawStringShadow(g2, mCaption, cx, cy, xpadding, ypadding, radius);
    g2.setPaint(Color.white);
    if (mTimestamp != null)
      g2.drawString(mTimestamp, tx, ty);
    if (mCaption != null)
      g2.drawString(mCaption, cx, cy);
  }

  private void drawStringShadow(Graphics2D g2, String s, int x, int y,
      double xpadding, double ypadding, double radius) {
    Font font = g2.getFont();
    FontRenderContext frc = g2.getFontRenderContext();
    Rectangle2D bounds = font.getStringBounds(s, frc);
    RoundRectangle2D rr = new RoundRectangle2D.Double();
    rr.setRoundRect(bounds.getX() - xpadding, bounds.getY() - ypadding,
        bounds.getWidth() + xpadding * 2, bounds.getHeight() + ypadding * 2,
        radius, radius);
    AffineTransform tx = AffineTransform.getTranslateInstance(x, y);
    Shape translatedBounds = tx.createTransformedShape(rr);
    
    //Composite previousComposite = g2.getComposite();
    //g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f));
    g2.setPaint(Color.black);
    g2.fill(translatedBounds);
    //g2.setComposite(previousComposite);
  }

  public void run() {
    while (true) {
      long beginTime = System.currentTimeMillis();

      if (mPrefetchedImage == null) {
        loadNext();
      }

      // Make it so.
      mFilename = mPrefetchedFilename;
      mImage = mPrefetchedImage;
      mTimestamp = mPrefetchedTimestamp;
      mCaption = mPrefetchedCaption;
      mPrefetchedFilename = null;
      mPrefetchedImage = null;
      mPrefetchedTimestamp = null;
      mPrefetchedCaption = null;
      //Log.log("Displaying image " + mCaption);
      //Log.log("  " + mTimestamp);
      //Log.log("  " + mCaption);
      repaint();

      // Load up the next one.
      loadNext();

      // Sleep for the rest of the time, if any time is left.
      long now = System.currentTimeMillis();
      long remainder = mDelay - (now - beginTime);
      if (remainder > 0) {
        try { Thread.sleep(remainder); } catch (InterruptedException ie) {}
      }
      else {
        Log.log("SlideMill.run(): remainder = " + remainder + " for image " + mPrefetchedFilename);
      }
    }
  }
  
  private File getRandomFile(File directory) {
    String[] fs = directory.list();
    if (fs.length == 0)
      return null;
    
    int i = mRandom.nextInt(fs.length);
    return new File(directory.getPath() + File.separator + fs[i]);
  }

  private String getNextFilename() {
    File newDirectory = new File(mBase + File.separator + kNew);
    File poolDirectory = new File(mBase + File.separator + kPool);
    File doneDirectory = new File(mBase + File.separator + kDone);
    
    File target;
    
    //Log.log("SlideMill.getNextFilename(): arrived");

    target = getRandomFile(newDirectory);
    if (target == null) {
      target = getRandomFile(poolDirectory);
    }

    // If 1new and 2pool are both empty, swap 2pool and 3done.
    if (target == null) {
      File tmpDirectory = new File(mBase + File.separator + kTmp);
      File originalPool = new File(mBase + File.separator + kPool);
      File originalDone = new File(mBase + File.separator + kDone);
      poolDirectory.renameTo(tmpDirectory);
      doneDirectory.renameTo(originalPool);
      tmpDirectory.renameTo(originalDone);
      return getNextFilename();
    }
    
    // Move the selected file to 3done.
    File destination = new File(doneDirectory.getPath() + File.separator + target.getName());
    boolean success = target.renameTo(destination);
    if (success == false) {
      Log.log("SlideMill.getNextFilename(): renameTo() failed");
    }
    
    // Return the 3done location of the file.
    return destination.getPath();
  }
  
  private void loadNext() {
    String filename = getNextFilename();
    //Log.log("SlideMill.loadNext(): filename = " + filename);

    String timestamp = null;
    String caption = null;

    // Retrieve metadata.
    try {
      File file = new File(filename);
      Metadata md = ImageMetadataReader.readMetadata(file);

      //[Exif IFD0] Image Description: Donald and the kids
      caption = getStringMetadata(md, "Exif IFD0", "Image Description");
      // [IPTC] - Caption/Abstract = Kristen & Jonathan - Wedding Day
      if (caption == null)
        caption = getStringMetadata(md, "IPTC", "Caption/Abstract");

      //[Exif IFD0] Date/Time: 2005:05:02 10:52:13
      timestamp = getDateMetadata(md, "Exif IFD0", "Date/Time", "yyyy:MM:dd");

      // [XMP] - Create Date = 1993-08-07T17:54:01
      if (timestamp == null)
        timestamp = getDateMetadata(md, "XMP", "Date/Time", "yyyy-MM-dd");
      // [XMP] Metadata Date: 2012-05-01T17:20:40-04:00
      if (timestamp == null)
        timestamp = getDateMetadata(md, "XMP", "Metadata Date", "yyyy-MM-dd");
      // [File] File Modified Date: Sat May 28 15:19:14 +00:00 2016
      // [File] File Modified Date: Sat Dec 07 19:53:00 +00:00 2013
      if (timestamp == null)
        timestamp = getDateMetadata(md, "File", "File Modified Date",
            "EEE MMM dd HH:mm:ss XXX yyyy");
    }
    catch (Exception e) { Log.log("SlideMill.loadNext(): " + e); }

    System.gc();

    // Now load the image.
    BufferedImage image = loadImage(filename);
    if (image != null) image.flush();

    System.gc();

    mPrefetchedFilename = filename;
    mPrefetchedImage = image;
    mPrefetchedTimestamp = timestamp;
    mPrefetchedCaption = caption;
  }

  private String getStringMetadata(Metadata m, String directory, String tag) {
    for (Directory d : m.getDirectories()) {
      if (d.getName().equals(directory)) {
        for (Tag t : d.getTags()) {
          if (t.getTagName().equals(tag))
            return t.getDescription();
        }
      }
    }
    return null;
  }

  private String getDateMetadata(Metadata m, String directory, String tag,
      String format) throws ParseException {
    for (Directory d : m.getDirectories()) {
      if (d.getName().equals(directory)) {
        for (Tag t : d.getTags()) {
          if (t.getTagName().equals(tag)) {
            String dateString = t.getDescription();
            DateFormat df = new SimpleDateFormat(format);
            DateFormat ff = new SimpleDateFormat("MMMM d, YYYY");
            Date theDate = df.parse(dateString);
            return ff.format(theDate);
          }
        }
      }
    }
    return null;
  }

  private BufferedImage loadImage(String path) {
    // Now load the image.
    BufferedImage img = null;
    try { img = ImageIO.read(new File(path)); }
    catch (Throwable t) {
      Log.log("SlideMill.loadImage(): " + t + ": " + path);
      System.gc();
      try { img = ImageIO.read(new File(path)); }
      catch (Throwable t2) {
        Log.log("SlideMill.loadImage(): second attempt: " + t2 + ": " + path);
      }
    }
    return img;
  }

  private static final long serialVersionUID = 1L; // Eliminates compile warning.
}
