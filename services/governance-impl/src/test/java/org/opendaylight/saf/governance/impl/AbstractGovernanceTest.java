/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.governance.impl;

import com.google.common.io.ByteStreams;
import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class AbstractGovernanceTest {
    @Value("${yang-root}")
    protected Path yangRoot;
    @Autowired
    protected GovernanceHandler handler;

    @Before
    public void setUp() throws IOException {
        ClassPath.from(Thread.currentThread().getContextClassLoader())
                .getResources()
                .stream()
                .filter(ri -> ri.getResourceName().endsWith(".yang"))
                .forEach(ri -> {
                    final String name = ri.getResourceName().substring(ri.getResourceName().lastIndexOf('/') + 1);
                    try (InputStream is = ri.url().openStream();
                            OutputStream os = Files.newOutputStream(yangRoot.resolve(name))) {
                        ByteStreams.copy(is, os);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    @After
    public void tearDown() throws IOException {
        Files.walkFileTree(yangRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!yangRoot.equals(dir)) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
