import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.text.*;

import javax.imageio.*;
import javax.imageio.stream.*;
import javax.imageio.metadata.*;
import javax.swing.*;


public class SlideMill extends Container implements Runnable {
  private static int kDefaultDelay = 10000; // 10s
  
  public static void main(String... args) throws IOException {
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

  private PhotoDB mPhotoDB;
  private Photo mPhoto, mNextPhoto;
  private int mDelay;

  public SlideMill(String base, int delay) throws IOException {
    Path srcPath = Path.of(base);
    Path dbPath = Path.of("photodb");

    mPhotoDB = new PhotoDB(srcPath, dbPath);

    mDelay = delay;
  }

  public void paint(Graphics g) {
    if (mPhoto != null && mPhoto.getCaption() == null)
      Log.log("SlideMill.paint(): no caption: " + mPhoto.getPath());
    if (mPhoto != null && mPhoto.getTimestamp() == null)
      Log.log("SlideMill.paint(): no timestamp: " + mPhoto.getPath());

    Graphics2D g2 = (Graphics2D)g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    Dimension d = getSize();
    int sw = d.width;
    int sh = d.height;

    g2.setPaint(Color.black);
    g2.fillRect(0, 0, d.width, d.height);

    if (mPhoto != null) {
      int iwfull = mPhoto.getBufferedImage().getWidth();
      int ihfull = mPhoto.getBufferedImage().getHeight();
      double scale = 1.0;
      double scalew = (double)sw / (double)iwfull;
      double scaleh = (double)sh / (double)ihfull;
      scale = Math.min(scalew, scaleh); // * 1.10;
      int iw = (int)(scale * (double)iwfull);
      int ih = (int)(scale * (double)ihfull);
      int ix = (sw - iw) / 2;
      int iy = (sh - ih) / 2;
      //Log.log("SlideMill.paint(): about to drawImage()");
      g2.drawImage(mPhoto.getBufferedImage(), ix, iy, iw, ih, Color.black, null);
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

    if (mPhoto == null) return;

    if (mPhoto.getTimestamp() != null)
      drawStringShadow(g2, mPhoto.getTimestamp(), tx, ty, xpadding, ypadding, radius);
    if (mPhoto.getCaption() != null)
      drawStringShadow(g2, mPhoto.getCaption(), cx, cy, xpadding, ypadding, radius);
    g2.setPaint(Color.white);
    if (mPhoto.getTimestamp() != null)
      g2.drawString(mPhoto.getTimestamp(), tx, ty);
    if (mPhoto.getCaption() != null)
      g2.drawString(mPhoto.getCaption(), cx, cy);
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

      if (mPhoto == null) {
        try {
          Path p = mPhotoDB.getNext();
          mPhoto = Photo.load(p);
        }
        catch (IOException ioe) {
          Log.log("SlideMill.run(): " + ioe);
        }
      }

      repaint();

      // Load up the next one.
      try {
        Path p = mPhotoDB.getNext();
        mNextPhoto = Photo.load(p);
      }
      catch (IOException ioe) {
        Log.log("SlideMill.run(): " + ioe);
      }

      // Sleep for the rest of the time, if any time is left.
      long now = System.currentTimeMillis();
      long remainder = mDelay - (now - beginTime);
      if (remainder > 0) {
        try { Thread.sleep(remainder); } catch (InterruptedException ie) {}
      }
      else {
        Log.log("SlideMill.run(): remainder = " + remainder + " for image " + mNextPhoto.getPath());
      }

      mPhoto = mNextPhoto;
    }
  }
  
  private static final long serialVersionUID = 1L; // Eliminates compile warning.
}
