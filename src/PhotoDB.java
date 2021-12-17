import java.io.IOException;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.stream.Stream;

/** Manages an on-disk database of photos.
  * 
  * All photos are contained in a single source directory, which
  * this class does not change.
  *
  * This class maintains symbolic links in three subdirectories,
  * where each source image has a link in one of the subdirectories.
  *
  * Calling next() returns one of the paths in the source directory and
  * updates the database.
  */
public class PhotoDB {
    public static void main(String... args) throws IOException {
        Log.log("PhotoDB.main()");

        Path srcPath = Path.of("tv-test-album");
        Path dbPath = Path.of("photodb");

        PhotoDB db = new PhotoDB(srcPath, dbPath);
        Path p = db.getNext();
        Log.log("PhotoDB.main(): p = " + p);
    }

    /* Diagnostics */
    private String mDiagnosticString;
    public String getDiagnosticString() { return mDiagnosticString; }

    // Directories for images.
    private static String kNew =  "1new";
    private static String kPool = "2pool";
    private static String kDone = "3done";
    private static String kTmp = "4tmp";

    private Path mSourcePath, mDBPath;
    private Random mRandom;

    public PhotoDB(Path source, Path db) throws IOException {
        mSourcePath = source;
        mDBPath = db;

        // If source doesn't exist, that's an error.
        if (Files.exists(mSourcePath) == false) {
            Log.log("PhotoDB.PhotoDB(): source does not exist");
            throw new IllegalArgumentException("Image source path must exist.");
        }

        // Create db if necessary.
        Files.createDirectories(mDBPath.resolve(kNew));
        Files.createDirectories(mDBPath.resolve(kPool));
        Files.createDirectories(mDBPath.resolve(kDone));

        clean();

        mRandom = new Random();
    }

    // Pull a link from 1new if available, otherwise pull a link
    // from 2pool. Move the link to 3done and return the path of
    // the actual image file from the source.
    public Path getNext() throws IOException {
        List<Path> contents;
        Path linkToMove = null;
        int numberAvailablePaths = 0;
        
        // First, make sure the db makes sense.
        clean();

        // Get the first one in kNew if available.
        contents = list(mDBPath.resolve(kNew));
        numberAvailablePaths += contents.size();
        if (contents.size() > 0) {
            linkToMove = contents.get(0);
        }

        // Get a random one out of kPool otherwise.
        contents = list(mDBPath.resolve(kPool));
        numberAvailablePaths += contents.size();
        if (linkToMove == null) {
            int i = mRandom.nextInt(contents.size());
            linkToMove = contents.get(i);
        }

        // Find the path of the actual file.
        Path realPath = Files.readSymbolicLink(linkToMove);

        // Move the link from its location to kDone.
        Files.move(linkToMove, mDBPath.resolve(kDone).resolve(linkToMove.getFileName()));

        // Swap kDone to kPool if necessary.
        if (numberAvailablePaths <= 1) {
            Log.log("PhotoDB.getNext(): [reshuffle]");
            Files.move(mDBPath.resolve(kDone), mDBPath.resolve(kTmp));
            Files.move(mDBPath.resolve(kPool), mDBPath.resolve(kDone));
            Files.move(mDBPath.resolve(kTmp), mDBPath.resolve(kPool));
        }

        try { Runtime.getRuntime().exec("sync"); }
        catch (Throwable t) {
            Log.log("[panic] Could not run sync");
        }

        Log.log("PhotoDB.getNext(): [show] " + realPath);

        return realPath;
    }

    // Prune nonlink and broken links from db.
    // Go through source directory. Anything that doesn't have a corresponding
    // link will be added to 1new.
    private void clean() throws IOException {
        List<Path> validPaths = new ArrayList<Path>();

        // Aggregate a list of valid file names from the db directories.
        List<Path> newPaths  = cleanLinkDirectory(mDBPath.resolve(kNew));
        List<Path> poolPaths = cleanLinkDirectory(mDBPath.resolve(kPool));
        List<Path> donePaths = cleanLinkDirectory(mDBPath.resolve(kDone));
        validPaths.addAll(newPaths);
        validPaths.addAll(poolPaths);
        validPaths.addAll(donePaths);

        // Look through the source directory. For any photo without a
        // corresponding link, create a new link in 1new.
        List<Path> allPaths = findNewPhotos(validPaths);

        // Build diagnostic string.
        NumberFormat nf = NumberFormat.getInstance();
        int countNew  = list(mDBPath.resolve(kNew)).size();
        int countPool = list(mDBPath.resolve(kPool)).size();
        int countDone = list(mDBPath.resolve(kDone)).size();
        int countAll = list(mSourcePath).size();

        if (countNew + countPool + countDone != countAll)
            Log.log("PhotoDB.clean(): [not-quite-panic] db counts do not equal picture count");
        
        mDiagnosticString = nf.format(countNew) + " + " +
                            nf.format(countPool) + " + " +
                            nf.format(countDone) + " = " +
                            nf.format(countAll);
        //Log.log("[diag] " + mDiagnosticString);
    }

    private List<Path> cleanLinkDirectory(Path p) throws IOException {
        List<Path> validPaths = new ArrayList<Path>();
        List<Path> contents = list(p);

        for (Path e: contents) {
            if (Files.isSymbolicLink(e) == false) {
                Log.log("PhotoDB:cleanLinkDirectory(): removing non-link " + e);
                Files.delete(e);
            }
            else if (Files.exists(Files.readSymbolicLink(e)) == false) {
                Log.log("PhotoDB:cleanLinkDirectory(): removing broken link " + e);
                Files.delete(e);
            }
            else {
                //Log.log("PhotoDB.cleanLinkDirectory(" + p + "): adding " + e.genfileName());
                validPaths.add(e.getFileName());
            }
        }

        return validPaths;
    }

    private List<Path> findNewPhotos(List<Path> v) throws IOException {
        List<Path> contents = list(mSourcePath);

        for (Path e: contents) {
            // If we don't already know about it, put it in kNew.
            // Also, skip files that begin with a dot, although we shouldn't have to do that here.
            if (v.contains(e.getFileName()) == false && e.getFileName().toString().indexOf(".") != 0) {
                String name = e.getFileName().toString();
                Path linkPath = mDBPath.resolve(kNew).resolve(name);
                Path realPath = mSourcePath.resolve(name).toAbsolutePath();
                Log.log("PhotoDB.findNewPhotos(): link " + linkPath + " -> " + realPath);
                Files.createSymbolicLink(linkPath, realPath);
            }
        }
        return contents;
    }

    private List<Path> list(Path directory) throws IOException {
        List<Path> contents = new ArrayList<Path>();
        try (Stream<Path> filepath = Files.list(directory)) {
            filepath.forEach(e -> contents.add(e));
        }
        return contents;
    }
}