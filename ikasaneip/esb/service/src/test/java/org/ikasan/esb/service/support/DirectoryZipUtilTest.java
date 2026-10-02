package org.ikasan.esb.service.support;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

/**
 * Test class for DirectoryZipUtil.
 */
public class DirectoryZipUtilTest {

    private Path tempDir;
    private List<Path> createdFiles;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("zip-test-");
        createdFiles = new ArrayList<>();
    }

    @After
    public void tearDown() throws IOException {
        // Clean up created files
        for (Path file : createdFiles) {
            if (Files.exists(file)) {
                Files.delete(file);
            }
        }

        // Clean up temp directory and its contents
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }

    @Test
    public void test_zipDirectory_with_single_file() throws IOException {
        // Create a test directory with a single file
        Path testDir = Files.createDirectory(tempDir.resolve("test-dir"));
        Path testFile = testDir.resolve("test.txt");
        Files.write(testFile, "Hello World".getBytes());

        Path zipPath = tempDir.resolve("output.zip");
        createdFiles.add(zipPath);

        // Zip the directory
        DirectoryZipUtil.zipDirectory(testDir.toString(), zipPath.toString());

        // Verify zip was created
        assertTrue("Zip file should exist", Files.exists(zipPath));
        assertTrue("Zip file should have content", Files.size(zipPath) > 0);

        // Verify zip contents
        List<String> zipEntries = getZipEntries(zipPath);
        assertTrue("Should contain directory entry", zipEntries.contains("test-dir/"));
        assertTrue("Should contain file entry", zipEntries.contains("test-dir/test.txt"));

        // Verify file content
        String content = extractFileContent(zipPath, "test-dir/test.txt");
        assertEquals("File content should match", "Hello World", content);
    }

    @Test
    public void test_zipDirectory_with_multiple_files() throws IOException {
        // Create a test directory with multiple files
        Path testDir = Files.createDirectory(tempDir.resolve("multi-file-dir"));
        Files.write(testDir.resolve("file1.txt"), "Content 1".getBytes());
        Files.write(testDir.resolve("file2.txt"), "Content 2".getBytes());
        Files.write(testDir.resolve("file3.txt"), "Content 3".getBytes());

        Path zipPath = tempDir.resolve("multi-output.zip");
        createdFiles.add(zipPath);

        // Zip the directory
        DirectoryZipUtil.zipDirectory(testDir.toString(), zipPath.toString());

        // Verify zip contents
        List<String> zipEntries = getZipEntries(zipPath);
        assertEquals("Should have 4 entries (1 dir + 3 files)", 4, zipEntries.size());
        assertTrue("Should contain file1", zipEntries.contains("multi-file-dir/file1.txt"));
        assertTrue("Should contain file2", zipEntries.contains("multi-file-dir/file2.txt"));
        assertTrue("Should contain file3", zipEntries.contains("multi-file-dir/file3.txt"));
    }

    @Test
    public void test_zipDirectory_with_subdirectories() throws IOException {
        // Create a test directory with subdirectories
        Path testDir = Files.createDirectory(tempDir.resolve("parent-dir"));
        Path subDir1 = Files.createDirectory(testDir.resolve("sub-dir-1"));
        Path subDir2 = Files.createDirectory(testDir.resolve("sub-dir-2"));

        Files.write(testDir.resolve("root.txt"), "Root file".getBytes());
        Files.write(subDir1.resolve("sub1.txt"), "Sub dir 1 file".getBytes());
        Files.write(subDir2.resolve("sub2.txt"), "Sub dir 2 file".getBytes());

        Path zipPath = tempDir.resolve("nested-output.zip");
        createdFiles.add(zipPath);

        // Zip the directory
        DirectoryZipUtil.zipDirectory(testDir.toString(), zipPath.toString());

        // Verify zip contents
        List<String> zipEntries = getZipEntries(zipPath);
        assertTrue("Should contain parent directory", zipEntries.contains("parent-dir/"));
        assertTrue("Should contain sub-dir-1", zipEntries.contains("parent-dir/sub-dir-1/"));
        assertTrue("Should contain sub-dir-2", zipEntries.contains("parent-dir/sub-dir-2/"));
        assertTrue("Should contain root file", zipEntries.contains("parent-dir/root.txt"));
        assertTrue("Should contain sub1 file", zipEntries.contains("parent-dir/sub-dir-1/sub1.txt"));
        assertTrue("Should contain sub2 file", zipEntries.contains("parent-dir/sub-dir-2/sub2.txt"));
    }

    @Test
    public void test_zipDirectory_with_large_file() throws IOException {
        // Create a test directory with a large file (> 8KB to test buffering)
        Path testDir = Files.createDirectory(tempDir.resolve("large-file-dir"));
        Path largeFile = testDir.resolve("large.txt");

        // Create a 1MB file
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }
        Files.write(largeFile, largeContent);

        Path zipPath = tempDir.resolve("large-output.zip");
        createdFiles.add(zipPath);

        // Zip the directory
        DirectoryZipUtil.zipDirectory(testDir.toString(), zipPath.toString());

        // Verify zip was created and is smaller than original (compression)
        assertTrue("Zip file should exist", Files.exists(zipPath));
        assertTrue("Zip should be smaller than 1MB due to compression", Files.size(zipPath) < 1024 * 1024);

        // Verify content
        byte[] extractedContent = extractFileBytes(zipPath, "large-file-dir/large.txt");
        assertArrayEquals("File content should match", largeContent, extractedContent);
    }

    @Test
    public void test_zipDirectory_to_output_stream() throws IOException {
        // Create a test directory
        Path testDir = Files.createDirectory(tempDir.resolve("stream-dir"));
        Files.write(testDir.resolve("stream-test.txt"), "Stream content".getBytes());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Zip to output stream
        DirectoryZipUtil.zipDirectory(testDir.toString(), baos);

        // Verify output stream has content
        assertTrue("Output stream should have content", baos.size() > 0);

        // Verify we can read the zip from the byte array
        try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
             ZipInputStream zis = new ZipInputStream(bais)) {

            List<String> entries = new ArrayList<>();
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
                zis.closeEntry();
            }

            assertTrue("Should contain file entry", entries.contains("stream-dir/stream-test.txt"));
        }
    }

    @Test
    public void test_zipFile_single_file() throws IOException {
        // Create a single test file
        Path testFile = tempDir.resolve("single-file.txt");
        Files.write(testFile, "Single file content".getBytes());

        Path zipPath = tempDir.resolve("single-file-output.zip");
        createdFiles.add(zipPath);

        // Zip the file
        DirectoryZipUtil.zipFile(testFile.toString(), zipPath.toString());

        // Verify zip was created
        assertTrue("Zip file should exist", Files.exists(zipPath));

        // Verify zip contents
        List<String> zipEntries = getZipEntries(zipPath);
        assertEquals("Should have 1 entry", 1, zipEntries.size());
        assertTrue("Should contain the file", zipEntries.contains("single-file.txt"));

        // Verify content
        String content = extractFileContent(zipPath, "single-file.txt");
        assertEquals("File content should match", "Single file content", content);
    }

    @Test
    public void test_zipDirectory_empty_directory() throws IOException {
        // Create an empty directory
        Path emptyDir = Files.createDirectory(tempDir.resolve("empty-dir"));

        Path zipPath = tempDir.resolve("empty-output.zip");
        createdFiles.add(zipPath);

        // Zip the empty directory
        DirectoryZipUtil.zipDirectory(emptyDir.toString(), zipPath.toString());

        // Verify zip was created
        assertTrue("Zip file should exist", Files.exists(zipPath));

        // Verify zip contains the directory entry
        List<String> zipEntries = getZipEntries(zipPath);
        assertEquals("Should have 1 directory entry", 1, zipEntries.size());
        assertTrue("Should contain directory entry", zipEntries.contains("empty-dir/"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_zipDirectory_non_existent_directory() throws IOException {
        Path nonExistent = tempDir.resolve("does-not-exist");
        Path zipPath = tempDir.resolve("output.zip");

        DirectoryZipUtil.zipDirectory(nonExistent.toString(), zipPath.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_zipDirectory_with_file_path() throws IOException {
        // Create a file instead of directory
        Path testFile = tempDir.resolve("not-a-directory.txt");
        Files.write(testFile, "content".getBytes());

        Path zipPath = tempDir.resolve("output.zip");

        DirectoryZipUtil.zipDirectory(testFile.toString(), zipPath.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_zipFile_non_existent_file() throws IOException {
        Path nonExistent = tempDir.resolve("does-not-exist.txt");
        Path zipPath = tempDir.resolve("output.zip");

        DirectoryZipUtil.zipFile(nonExistent.toString(), zipPath.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_zipFile_with_directory_path() throws IOException {
        // Create a directory
        Path testDir = Files.createDirectory(tempDir.resolve("is-directory"));

        Path zipPath = tempDir.resolve("output.zip");

        DirectoryZipUtil.zipFile(testDir.toString(), zipPath.toString());
    }

    @Test
    public void test_zipDirectory_with_deep_nesting() throws IOException {
        // Create a deeply nested directory structure
        Path testDir = Files.createDirectory(tempDir.resolve("level1"));
        Path level2 = Files.createDirectory(testDir.resolve("level2"));
        Path level3 = Files.createDirectory(level2.resolve("level3"));
        Path level4 = Files.createDirectory(level3.resolve("level4"));
        Path level5 = Files.createDirectory(level4.resolve("level5"));

        Files.write(level5.resolve("deep-file.txt"), "Deep content".getBytes());

        Path zipPath = tempDir.resolve("deep-output.zip");
        createdFiles.add(zipPath);

        // Zip the directory
        DirectoryZipUtil.zipDirectory(testDir.toString(), zipPath.toString());

        // Verify the deeply nested file exists in the zip
        List<String> zipEntries = getZipEntries(zipPath);
        assertTrue("Should contain deeply nested file",
            zipEntries.contains("level1/level2/level3/level4/level5/deep-file.txt"));

        // Verify content
        String content = extractFileContent(zipPath, "level1/level2/level3/level4/level5/deep-file.txt");
        assertEquals("File content should match", "Deep content", content);
    }

    @Test
    public void test_zipDirectory_with_special_characters_in_filename() throws IOException {
        // Create a directory with files containing special characters
        Path testDir = Files.createDirectory(tempDir.resolve("special-chars-dir"));
        Files.write(testDir.resolve("file with spaces.txt"), "Content 1".getBytes());
        Files.write(testDir.resolve("file-with-dashes.txt"), "Content 2".getBytes());
        Files.write(testDir.resolve("file_with_underscores.txt"), "Content 3".getBytes());

        Path zipPath = tempDir.resolve("special-chars-output.zip");
        createdFiles.add(zipPath);

        // Zip the directory
        DirectoryZipUtil.zipDirectory(testDir.toString(), zipPath.toString());

        // Verify all files are included
        List<String> zipEntries = getZipEntries(zipPath);
        assertTrue("Should contain file with spaces",
            zipEntries.contains("special-chars-dir/file with spaces.txt"));
        assertTrue("Should contain file with dashes",
            zipEntries.contains("special-chars-dir/file-with-dashes.txt"));
        assertTrue("Should contain file with underscores",
            zipEntries.contains("special-chars-dir/file_with_underscores.txt"));
    }

    @Test
    public void test_zipFileContents_with_single_file() throws IOException {
        Map<String, String> fileContents = new HashMap<>();
        fileContents.put("test.txt", "Test content");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Zip file contents to output stream
        DirectoryZipUtil.zipFileContents(fileContents, baos);

        // Verify output stream has content
        assertTrue("Output stream should have content", baos.size() > 0);

        // Verify we can read the zip
        try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
             ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry = zis.getNextEntry();
            assertNotNull("Should have an entry", entry);
            assertEquals("Entry name should match", "test.txt", entry.getName());

            ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = zis.read(buffer)) >= 0) {
                contentStream.write(buffer, 0, length);
            }

            assertEquals("Content should match", "Test content", contentStream.toString());
            zis.closeEntry();

            assertNull("Should have no more entries", zis.getNextEntry());
        }
    }

    @Test
    public void test_zipFileContents_with_multiple_files() throws IOException {
        Map<String, String> fileContents = new HashMap<>();
        fileContents.put("file1.txt", "Content 1");
        fileContents.put("file2.txt", "Content 2");
        fileContents.put("file3.txt", "Content 3");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Zip file contents to output stream
        DirectoryZipUtil.zipFileContents(fileContents, baos);

        // Verify output stream has content
        assertTrue("Output stream should have content", baos.size() > 0);

        // Verify we can read all files from the zip
        Map<String, String> extractedContents = new HashMap<>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
             ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = zis.read(buffer)) >= 0) {
                    contentStream.write(buffer, 0, length);
                }
                extractedContents.put(entry.getName(), contentStream.toString());
                zis.closeEntry();
            }
        }

        assertEquals("Should have 3 files", 3, extractedContents.size());
        assertEquals("File1 content should match", "Content 1", extractedContents.get("file1.txt"));
        assertEquals("File2 content should match", "Content 2", extractedContents.get("file2.txt"));
        assertEquals("File3 content should match", "Content 3", extractedContents.get("file3.txt"));
    }

    @Test
    public void test_zipFileContents_with_nested_paths() throws IOException {
        Map<String, String> fileContents = new HashMap<>();
        fileContents.put("dir1/file1.txt", "Content 1");
        fileContents.put("dir1/dir2/file2.txt", "Content 2");
        fileContents.put("dir3/file3.txt", "Content 3");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Zip file contents to output stream
        DirectoryZipUtil.zipFileContents(fileContents, baos);

        // Verify we can read all files with paths
        Map<String, String> extractedContents = new HashMap<>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
             ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = zis.read(buffer)) >= 0) {
                    contentStream.write(buffer, 0, length);
                }
                extractedContents.put(entry.getName(), contentStream.toString());
                zis.closeEntry();
            }
        }

        assertEquals("Should have 3 files", 3, extractedContents.size());
        assertEquals("File1 content should match", "Content 1", extractedContents.get("dir1/file1.txt"));
        assertEquals("File2 content should match", "Content 2", extractedContents.get("dir1/dir2/file2.txt"));
        assertEquals("File3 content should match", "Content 3", extractedContents.get("dir3/file3.txt"));
    }

    @Test
    public void test_zipFileContents_with_large_content() throws IOException {
        Map<String, String> fileContents = new HashMap<>();

        // Create a large string (100KB)
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("This is line ").append(i).append("\n");
        }

        fileContents.put("large-file.txt", largeContent.toString());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Zip file contents to output stream
        DirectoryZipUtil.zipFileContents(fileContents, baos);

        // Verify output stream has content and is compressed
        assertTrue("Output stream should have content", baos.size() > 0);
        assertTrue("Compressed size should be less than original",
            baos.size() < largeContent.length());

        // Verify we can read the large content back
        try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
             ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry = zis.getNextEntry();
            assertNotNull("Should have an entry", entry);
            assertEquals("Entry name should match", "large-file.txt", entry.getName());

            ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = zis.read(buffer)) >= 0) {
                contentStream.write(buffer, 0, length);
            }

            assertEquals("Content should match", largeContent.toString(), contentStream.toString());
        }
    }

    @Test
    public void test_zipFileContents_with_empty_content() throws IOException {
        Map<String, String> fileContents = new HashMap<>();
        fileContents.put("empty.txt", "");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Zip file contents to output stream
        DirectoryZipUtil.zipFileContents(fileContents, baos);

        // Verify output stream has content (zip structure even with empty file)
        assertTrue("Output stream should have content", baos.size() > 0);

        // Verify we can read the empty file
        try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
             ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry = zis.getNextEntry();
            assertNotNull("Should have an entry", entry);
            assertEquals("Entry name should match", "empty.txt", entry.getName());

            ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = zis.read(buffer)) >= 0) {
                contentStream.write(buffer, 0, length);
            }

            assertEquals("Content should be empty", "", contentStream.toString());
        }
    }

    @Test
    public void test_zipFileContents_with_special_characters() throws IOException {
        Map<String, String> fileContents = new HashMap<>();
        fileContents.put("file with spaces.txt", "Content with spaces");
        fileContents.put("file-with-dashes.txt", "Content with dashes");
        fileContents.put("file_with_underscores.txt", "Content with underscores");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Zip file contents to output stream
        DirectoryZipUtil.zipFileContents(fileContents, baos);

        // Verify we can read all files
        Map<String, String> extractedContents = new HashMap<>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
             ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = zis.read(buffer)) >= 0) {
                    contentStream.write(buffer, 0, length);
                }
                extractedContents.put(entry.getName(), contentStream.toString());
                zis.closeEntry();
            }
        }

        assertEquals("Should have 3 files", 3, extractedContents.size());
        assertTrue("Should contain file with spaces", extractedContents.containsKey("file with spaces.txt"));
        assertTrue("Should contain file with dashes", extractedContents.containsKey("file-with-dashes.txt"));
        assertTrue("Should contain file with underscores", extractedContents.containsKey("file_with_underscores.txt"));
    }

    @Test
    public void test_zipFileContents_with_unicode_content() throws IOException {
        Map<String, String> fileContents = new HashMap<>();
        fileContents.put("unicode.txt", "Hello 世界 🌍 Здравствуй مرحبا");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Zip file contents to output stream
        DirectoryZipUtil.zipFileContents(fileContents, baos);

        // Verify we can read the unicode content back correctly
        try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
             ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry = zis.getNextEntry();
            assertNotNull("Should have an entry", entry);

            ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = zis.read(buffer)) >= 0) {
                contentStream.write(buffer, 0, length);
            }

            assertEquals("Unicode content should match", "Hello 世界 🌍 Здравствуй مرحبا",
                contentStream.toString());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_zipFileContents_with_null_map() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DirectoryZipUtil.zipFileContents(null, baos);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_zipFileContents_with_empty_map() throws IOException {
        Map<String, String> fileContents = new HashMap<>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DirectoryZipUtil.zipFileContents(fileContents, baos);
    }

    // Helper methods

    private List<String> getZipEntries(Path zipPath) throws IOException {
        List<String> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
                zis.closeEntry();
            }
        }
        return entries;
    }

    private String extractFileContent(Path zipPath, String entryName) throws IOException {
        byte[] bytes = extractFileBytes(zipPath, entryName);
        return new String(bytes);
    }

    private byte[] extractFileBytes(Path zipPath, String entryName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = zis.read(buffer)) >= 0) {
                        baos.write(buffer, 0, length);
                    }
                    return baos.toByteArray();
                }
                zis.closeEntry();
            }
        }
        throw new IOException("Entry not found: " + entryName);
    }
}
