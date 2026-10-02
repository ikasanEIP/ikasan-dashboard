package org.ikasan.esb.service.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for creating zip files from directory contents.
 * This class is designed to be memory efficient by streaming files
 * directly to the zip output stream rather than loading them into memory.
 */
public class DirectoryZipUtil {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryZipUtil.class);

    private static final int BUFFER_SIZE = 8192; // 8KB buffer for streaming

    /**
     * Creates a zip file from all files in the specified directory.
     * Files are streamed to minimize memory usage, making this suitable for large files and directories.
     *
     * @param sourceDirectoryPath the path to the directory to zip
     * @param outputZipPath the path where the zip file will be created
     * @throws IOException if an I/O error occurs during zipping
     * @throws IllegalArgumentException if the source directory doesn't exist or is not a directory
     */
    public static void zipDirectory(String sourceDirectoryPath, String outputZipPath) throws IOException {
        Path sourcePath = Paths.get(sourceDirectoryPath);

        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("Source directory does not exist: " + sourceDirectoryPath);
        }

        if (!Files.isDirectory(sourcePath)) {
            throw new IllegalArgumentException("Source path is not a directory: " + sourceDirectoryPath);
        }

        logger.info("Creating zip file from directory: {} to output: {}", sourceDirectoryPath, outputZipPath);

        try (FileOutputStream fos = new FileOutputStream(outputZipPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zipOut = new ZipOutputStream(bos)) {

            zipDirectory(sourcePath.toFile(), sourcePath.getFileName().toString(), zipOut);

            logger.info("Successfully created zip file: {}", outputZipPath);
        }
    }

    /**
     * Creates a zip file from all files in the specified directory and writes to the provided output stream.
     * Files are streamed to minimize memory usage, making this suitable for large files and directories.
     * The caller is responsible for closing the output stream.
     *
     * @param sourceDirectoryPath the path to the directory to zip
     * @param outputStream the output stream to write the zip file to
     * @throws IOException if an I/O error occurs during zipping
     * @throws IllegalArgumentException if the source directory doesn't exist or is not a directory
     */
    public static void zipDirectory(String sourceDirectoryPath, OutputStream outputStream) throws IOException {
        Path sourcePath = Paths.get(sourceDirectoryPath);

        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("Source directory does not exist: " + sourceDirectoryPath);
        }

        if (!Files.isDirectory(sourcePath)) {
            throw new IllegalArgumentException("Source path is not a directory: " + sourceDirectoryPath);
        }

        logger.info("Creating zip file from directory: {}", sourceDirectoryPath);

        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            zipDirectory(sourcePath.toFile(), sourcePath.getFileName().toString(), zipOut);
            logger.info("Successfully created zip file from directory: {}", sourceDirectoryPath);
        }
    }

    /**
     * Recursively zips the contents of a directory into a ZipOutputStream.
     * Files are read in chunks using a buffer to minimize memory usage.
     *
     * @param fileToZip the File representing the file or directory to be zipped
     * @param fileName the name of the entry in the zip file
     * @param zipOutputStream the ZipOutputStream to write the zipped data to
     * @throws IOException if an I/O error occurs during zipping
     */
    private static void zipDirectory(File fileToZip, String fileName, ZipOutputStream zipOutputStream) throws IOException {
        if (fileToZip.isHidden()) {
            logger.debug("Skipping hidden file: {}", fileToZip.getName());
            return;
        }

        if (fileToZip.isDirectory()) {
            String entryName = fileName.endsWith("/") ? fileName : fileName + "/";
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.closeEntry();

            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipDirectory(childFile, fileName + "/" + childFile.getName(), zipOutputStream);
                }
            }
            return;
        }

        // Handle regular files - stream in chunks to minimize memory usage
        logger.debug("Adding file to zip: {}", fileName);

        try (FileInputStream fis = new FileInputStream(fileToZip);
             BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE)) {

            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOutputStream.putNextEntry(zipEntry);

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = bis.read(buffer)) >= 0) {
                zipOutputStream.write(buffer, 0, length);
            }

            zipOutputStream.closeEntry();
        }
    }

    /**
     * Creates a zip file from a map of file names to file contents and writes to the provided output stream.
     * This method is memory efficient by streaming the content directly to the zip output stream.
     * The caller is responsible for closing the output stream.
     *
     * @param fileContentsMap a map where the key is the filename and the value is the file contents
     * @param outputStream the output stream to write the zip file to
     * @throws IOException if an I/O error occurs during zipping
     * @throws IllegalArgumentException if the map is null or empty
     */
    public static void zipFileContents(Map<String, String> fileContentsMap, OutputStream outputStream) throws IOException {
        if (fileContentsMap == null || fileContentsMap.isEmpty()) {
            throw new IllegalArgumentException("File contents map cannot be null or empty");
        }

        logger.info("Creating zip file from {} file entries", fileContentsMap.size());

        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            for (Map.Entry<String, String> entry : fileContentsMap.entrySet()) {
                String filename = entry.getKey();
                String contents = entry.getValue();

                logger.debug("Adding file to zip: {}", filename);

                ZipEntry zipEntry = new ZipEntry(filename);
                zipOut.putNextEntry(zipEntry);

                byte[] bytes = contents.getBytes();
                zipOut.write(bytes, 0, bytes.length);

                zipOut.closeEntry();
            }

            logger.info("Successfully created zip file with {} entries", fileContentsMap.size());
        }
    }

    /**
     * Zips a single file to the specified output path.
     *
     * @param sourceFilePath the path to the file to zip
     * @param outputZipPath the path where the zip file will be created
     * @throws IOException if an I/O error occurs during zipping
     * @throws IllegalArgumentException if the source file doesn't exist or is a directory
     */
    public static void zipFile(String sourceFilePath, String outputZipPath) throws IOException {
        Path sourcePath = Paths.get(sourceFilePath);

        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("Source file does not exist: " + sourceFilePath);
        }

        if (Files.isDirectory(sourcePath)) {
            throw new IllegalArgumentException("Source path is a directory, use zipDirectory instead: " + sourceFilePath);
        }

        logger.info("Creating zip file from: {} to output: {}", sourceFilePath, outputZipPath);

        try (FileOutputStream fos = new FileOutputStream(outputZipPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zipOut = new ZipOutputStream(bos);
             FileInputStream fis = new FileInputStream(sourcePath.toFile());
             BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE)) {

            ZipEntry zipEntry = new ZipEntry(sourcePath.getFileName().toString());
            zipOut.putNextEntry(zipEntry);

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = bis.read(buffer)) >= 0) {
                zipOut.write(buffer, 0, length);
            }

            zipOut.closeEntry();
            logger.info("Successfully created zip file: {}", outputZipPath);
        }
    }
}
