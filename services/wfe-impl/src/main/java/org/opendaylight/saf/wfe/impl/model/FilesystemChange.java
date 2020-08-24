/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl.model;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Simple abstraction for easier mocking of {@link WatchEvent}.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jul 30, 2019
 */
@Data
@AllArgsConstructor
public class FilesystemChange {
    private final Path path;
    private final WatchEvent.Kind<Path> kind;
}
