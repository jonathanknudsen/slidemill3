import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.text.*;

public class Log {
  private static String kDateFormat = "yyyy-MM-dd'T'HH:mm:ss";
  private static Path sPath = null;
  private static List<String> sLinesSinceReshuffle;

  public static void log(String s) {
    //prepend a timestamp
    Date now = new Date();
    DateFormat df = new SimpleDateFormat(kDateFormat);
    String ds = df.format(now);
    String logString = ds + " " + s;

    // Update internal list.
    updateLine(logString);

    // Dump to stdout and exit early if we don't have a path.
    if (sPath == null) {
      System.out.println(logString);
      return;
    }
    // Append to log file.
    try {
      OutputStream out = Files.newOutputStream(sPath,
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      PrintWriter pw = new PrintWriter(out);
      pw.println(logString);
      pw.close(); // We must do this to flush output.
    }
    catch (IOException ioe) {
      sPath = null;
      log("[panic] log failed: " + ioe);
    }
  }

  public static void setPath(Path p) throws IOException {
    sPath = p;

    if (Files.exists(p) == false)
      return;
    
    // Load past entries.
    BufferedReader in = Files.newBufferedReader(sPath);
    String line;
    while ((line = in.readLine()) != null) {
      updateLine(line);
    }
  }

  private static void updateLine(String line) {
    if (sLinesSinceReshuffle == null)
      sLinesSinceReshuffle = new ArrayList<String>();

    int showIndex = line.indexOf("[show]");
    if (line.indexOf(showIndex) != -1) {
      String imgPath = line.substring(showIndex + 7);
      hasAlreadyHappened(imgPath);
      sLinesSinceReshuffle.add(line);
    }
    if (line.indexOf("[panic]") != -1) {
      sLinesSinceReshuffle.add(line);
    }
    if (line.indexOf("[reshuffle]") != -1) {
      sLinesSinceReshuffle.clear();
    }
  }

  public static boolean hasAlreadyHappened(String imgPath) {
    //log("Log.hasAlreadyHappened(): sLinesSinceReshuffle.size() = " + sLinesSinceReshuffle.size());
    boolean hasAlreadyHappened = false;
    for (int i = 0; i < sLinesSinceReshuffle.size(); i++) {
      String logEntry = sLinesSinceReshuffle.get(i);
      if (logEntry.indexOf("[show]") != -1) {
        if (logEntry.indexOf(imgPath) != -1) {
          log("[panic] Already showed " + imgPath);
          hasAlreadyHappened = true;
        }
      }
    }
    return hasAlreadyHappened;
  }

  public static int countPanics() {
    int panics = 0;
    for (int i = 0; i < sLinesSinceReshuffle.size(); i++) {
      String logEntry = sLinesSinceReshuffle.get(i);
      if (logEntry.indexOf("[panic]") != -1) {
        panics++;
      }
    }
    return panics;
  }
}
