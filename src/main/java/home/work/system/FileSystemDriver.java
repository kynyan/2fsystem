package home.work.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

/**
 * Class representing client for access to file system.
 */
public class FileSystemDriver {
    private static Logger logger = LoggerFactory.getLogger(FileSystemDriver.class);
    private final FileSystem fileSystem;

    public FileSystemDriver(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    /**
     * Creates empty file in a filesystem
     *
     * @param  filename
     *         Name of the file to create
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public void createFile(String filename) throws IOException {
        createFile(filename, new byte[0]);
    }

    /**
     * Checks if there is enough space in the filesystem.
     * Creates file with filename and content
     *
     * @param  filename
     *         Name of the file to create
     *
     * @param  content
     *         Bytes of the file content
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @throws  IllegalArgumentException
     *          In case there is not enough space
     */
    public void createFile(String filename, byte[] content) throws IOException {
        File file = new File(filename, content);
        checkThereIsEnoughSpace(file.getTotalLength());
        fileSystem.writeFileToFileSystem(file);
    }

    /**
     * Copies existing file to a filesystem
     *
     * @param  pathToFile
     *         Absolute path to the file
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @throws  IllegalArgumentException
     *          In case there is not enough space, or file is not found
     */
    public void copyExistingFile(String pathToFile) throws IOException {
        java.io.File original = new java.io.File(pathToFile);
        checkFileExists(original);
        checkThereIsEnoughSpace(original.length());
        fileSystem.writeFileToFileSystem(original);
    }

    /**
     * Downloads file and writes it to the file system with the specified filename.
     * Connection may not return file size. Then we only check if filename fits available space.
     * If file content's size is larger than available space then exception will be thrown
     * at {@link FileSystem} level
     *
     * @param  uri
     *         URI to download file
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @throws  IllegalArgumentException
     *          In case url is malformed, connection returned
     *          anything other than 200, or if there is not enough space
     */
    public void downloadAndSaveFile(String uri) throws IOException {
        HttpURLConnection connection = openConnection(uri);
        int fileSize = connection.getContentLength();
        String filename = getFilename(connection, uri);
        if (fileSize < 1) {
            fileSize = filename.getBytes().length;
        }
        checkThereIsEnoughSpace(fileSize);
        logger.info("Started downloading file from " + uri);
        fileSystem.writeFileFromConnection(connection, filename);
        logger.info("Completed downloading file from " + uri);
    }

    /**
     * Opens HttpURLConnection to download file
     *
     * @param  uri
     *         URI to download file
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @throws  IllegalArgumentException
     *          In case url is malformed or connection
     *          returned anything other than 200
     */
    private HttpURLConnection openConnection(String uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
        int response = connection.getResponseCode();
        if (response != HttpURLConnection.HTTP_OK) {
            String errorMsg = String.format("Connection to %s returned %d", uri, response);
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        return connection;
    }

    /**
     * Gets filename from the connection "Content-Disposition" header
     * or from the URI if header doesn't exist
     *
     * @param  uri
     *         URI to download file
     */
    private String getFilename(HttpURLConnection connection, String uri) {
        String filename = "";
        String disposition = connection.getHeaderField("Content-Disposition");
        if (disposition != null) {
            // extracts file name from header field
            int index = disposition.indexOf("filename=");
            if (index > 0) {
                filename = disposition.substring(index + 10, disposition.length() - 1);
            }
        } else {
            // extracts file name from URL
            filename = uri.substring(uri.lastIndexOf("/") + 1);
        }
        return filename;
    }

    /**
     * Checks if there is enough space in the filesystem.
     * Overwrites file if it exists with the same filename, or
     * creates a new one.
     *
     * @param  filename
     *         Name of the file to create
     *
     * @param  content
     *         Bytes of the file content
     *
     * @throws  IllegalArgumentException
     *          In case there is not enough space
     */
    public void overwriteFile(String filename, byte[] content) throws IOException {
        File file = new File(filename, content);
        checkThereIsEnoughSpace(file.getTotalLength());
        fileSystem.overwriteFile(file);
    }

    /**
     * Checks if file with specified name exists in the filesystem.
     *
     * @return {@code true} if file exists, {@code false} if not
     *
     * @param  filename
     *         Filename to search for
     */
    public boolean fileExists(String filename) {
        return fileSystem.fileExists(filename);
    }

    /**
     * Returns list of filenames existing in the file system.
     *
     * @return list of existing filenames
     *
     */
    public List<String> listFiles() {
        return fileSystem.listFiles();
    }

    /**
     * Removes file with the specified name from the file system.
     *
     * @param  filename
     *         Filename to remove
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public void deleteFile(String filename) throws IOException {
        fileSystem.removeFileFromFileSystem(filename);
    }

    /**
     * Reads file content from the file system.
     *
     * @param  filename
     *         Filename to search for
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public byte[] readFromFile(String filename) throws IOException {
        File file = fileSystem.readFileFromFileSystem(filename);
        return file.getContent();
    }

    /**
     * Returns an initialized instance of {@link ReadOnlyFileChannel},
     * containing content of the file with specified filename.
     *
     * @param  filename
     *         Filename to search for
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @throws  java.io.FileNotFoundException
     *          If file with specified name was not found
     */
    public ReadOnlyFileChannel getReadOnlyFileChannel(String filename) throws IOException {
        return fileSystem.getReadOnlyFileChannel(filename);
    }

    /**
     * Removes all data (except file system size and current position)
     * from the filesystem
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public void formatFileSystem() throws IOException {
        fileSystem.formatFileSystem();
    }

    /**
     * Checks if file system has enough space to write specified number of bytes.
     * Metadata (filename and content's length, isRemovedFlag) size is taken into account.
     *
     * @throws  IllegalArgumentException
     *          If there is not enough space
     */
    private void checkThereIsEnoughSpace(long fileSize) {
        if (!fileSystem.isEnoughSpace(fileSize + 9)) {
            //9 is 8 bytes for filename and content lengths + 1 byte for isRemoved flag
            String errorMsg = String.format("Available space of %d kB is less then file size of %d kB",
                    fileSystem.getAvailableSpace() / 1024, (fileSize) / 1024);
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    private void checkFileExists(java.io.File file) {
        if (!file.exists() || !file.isFile()) {
            String errorMsg = String.format("Could not recognize file at %s", file.getPath());
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }
}
