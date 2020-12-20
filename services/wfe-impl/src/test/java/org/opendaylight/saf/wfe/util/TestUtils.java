/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.util;

import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

/**
 * Test utilities.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jun 15, 2019
 */
public final class TestUtils {

    private TestUtils() {
        // utility class constructor
    }

    /**
     * Remove given path recursively. Any {@link IOException}s are ignored.
     *
     * @param directory path to remove
     */
    public static void removeDirectoryRecursive(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // NOOP
        }
    }

    /**
     * Copy resource from classpath into given directory.
     *
     * @param name name of resource to copy
     * @param path destination directory
     * @throws IllegalStateException when I/O error occur
     */
    public static void copyResource(String name, Path path) {
        try (InputStream is = Resources.getResource(name).openStream()) {
            is.transferTo(Files.newOutputStream(path.resolve(Paths.get(name).getFileName())));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Read given resource as UTF-8 encoded string.
     *
     * @param name name of resource to read
     * @return content of resource as text
     */
    public static String readResourceAsText(String name) {
        try (InputStream is = Resources.getResource(name).openStream()) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is.transferTo(baos);
            return baos.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Map<String, List<String>> readHeaderAsMap(String name) {
        String headerString = readResourceAsText(name);
        Type type = new TypeToken<Map<String, List<String>>>() {
            private static final long serialVersionUID = 1L;
        }.getType();
        return TestConstants.GSON.fromJson(headerString, type);
    }
}
