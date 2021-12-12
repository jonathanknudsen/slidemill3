import com.drew.metadata.*;
import com.drew.imaging.*;

import java.awt.image.*;
import java.io.IOException;
import java.nio.file.*;
import java.text.*;
import java.util.Date;

import javax.imageio.*;

public class Photo {
    public static void main(String... args) throws IOException {
        Photo p = Photo.load(Path.of("tv-test-album/IMG_0296.jpg"));
        Log.log("Photo.main(): timestamp = " + p.getTimestamp());
        Log.log("Photo.main(): caption = " + p.getCaption());
    }

    public static Photo load(Path path) throws IOException {
        //Log.log("Photo.load(): " + path);
        return new Photo(path);
    }

    private Path mPath;
    private BufferedImage mBufferedImage;
    private String mTimestamp;
    private String mCaption;

    protected Photo(Path path) throws IOException {
        mPath = path;
        loadImage(path);
        loadMetadata(path);
    }

    public Path getPath() { return mPath; }
    public BufferedImage getBufferedImage() { return mBufferedImage; }
    public String getTimestamp() { return mTimestamp; }
    public String getCaption() { return mCaption; }

    private void loadImage(Path path) throws IOException {
        try { mBufferedImage = ImageIO.read(Files.newInputStream(path)); }
        catch (Throwable t) {
            Log.log("Photo.loadImage(): " + t + ": " + path);
            System.gc();
            try { mBufferedImage = ImageIO.read(Files.newInputStream(path)); }
            catch (Throwable t2) {
                Log.log("Photo.loadImage(): second attempt: " + t2 + ": " + path);
            }
        }
    }

    private void loadMetadata(Path path) throws IOException {
        try {
            Metadata md = ImageMetadataReader.readMetadata(Files.newInputStream(path));

            //[Exif IFD0] Image Description: Donald and the kids
            mCaption = getStringMetadata(md, "Exif IFD0", "Image Description");
            // [IPTC] - Caption/Abstract = Kristen & Jonathan - Wedding Day
            if (mCaption == null)
                mCaption = getStringMetadata(md, "IPTC", "mCaption/Abstract");

            //[Exif IFD0] Date/Time: 2005:05:02 10:52:13
            mTimestamp = getDateMetadata(md, "Exif IFD0", "Date/Time", "yyyy:MM:dd");

            // [XMP] - Create Date = 1993-08-07T17:54:01
            if (mTimestamp == null)
                mTimestamp = getDateMetadata(md, "XMP", "Date/Time", "yyyy-MM-dd");
            // [XMP] Metadata Date: 2012-05-01T17:20:40-04:00
            if (mTimestamp == null)
                mTimestamp = getDateMetadata(md, "XMP", "Metadata Date", "yyyy-MM-dd");
            // [File] File Modified Date: Sat May 28 15:19:14 +00:00 2016
            // [File] File Modified Date: Sat Dec 07 19:53:00 +00:00 2013
            if (mTimestamp == null)
                mTimestamp = getDateMetadata(md, "File", "File Modified Date",
                "EEE MMM dd HH:mm:ss XXX yyyy");
        }
        catch (Throwable t) {
            Log.log("Photo.loadMetadata(): " + t);
        }
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
}