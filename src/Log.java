import java.io.*;
import java.util.*;
import java.text.*;

public class Log {
  private static String kDateFormat = "yyyy-MM-dd'T'HH:mm:ss";

  public static void log(String s) {
    //prepend a timestamp
    Date now = new Date();
    DateFormat df = new SimpleDateFormat(kDateFormat);
    String ds = df.format(now);
    System.out.println(ds + " " + s);
  }

  public static Date findSecondToLast(String keyword) {
    // Ugly ugly ugly.
    // Hardcoded log file path.
    // Search entire file every time.
    String secondToLast = null;
    String last = null;
    String logFilename = "run.log";
    try {
      BufferedReader in = new BufferedReader(new FileReader(logFilename));
      String line;
      while ((line = in.readLine()) != null) {
        if (line.indexOf(keyword) != -1) {
          secondToLast = last;
          last = line;
        }
      }
    }
    catch (IOException ioe) {
      System.out.println("Log.findSecondToLast(): " + ioe);
    }

    //System.out.println("secondToLast = " + secondToLast);
    //System.out.println("last = " + last);
    DateFormat df = new SimpleDateFormat(kDateFormat);
    if (secondToLast == null)
      return null;

    Date d = null;
    try { d = df.parse(secondToLast); }
    catch (ParseException pe) {
      System.out.println("Log.findSecondToLast(): " + pe);
    }
    return d;
  }

  public static Date findLast(String keyword) {
    // Ugly ugly ugly.
    // Hardcoded log file path.
    // Search entire file every time.
    String secondToLast = null;
    String last = null;
    String logFilename = "run.log";
    try {
      BufferedReader in = new BufferedReader(new FileReader(logFilename));
      String line;
      while ((line = in.readLine()) != null) {
        if (line.indexOf(keyword) != -1) {
          secondToLast = last;
          last = line;
        }
      }
    }
    catch (IOException ioe) {
      System.out.println("Log.findSecondToLast(): " + ioe);
    }

    //System.out.println("secondToLast = " + secondToLast);
    //System.out.println("last = " + last);
    DateFormat df = new SimpleDateFormat(kDateFormat);
    if (last == null)
      return null;

    Date d = null;
    try { d = df.parse(last); }
    catch (ParseException pe) {
      System.out.println("Log.findLast(): " + pe);
    }
    return d;
  }
}
