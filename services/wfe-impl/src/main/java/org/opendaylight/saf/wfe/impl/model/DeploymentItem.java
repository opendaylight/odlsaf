/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@EqualsAndHashCode
@ToString
@Getter
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public final class DeploymentItem {
    public static final String DEP_PREFIX = "deployment://";
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentItem.class);
    private static final XPathExpression SCRIPT_XPATH;
    // 1 minute should be enough for deployment to become ready (all scripts files present)
    private static final long DEPLOYMENT_DEADLINE_MILLIS = 60_000;

    static {
        try {
            SCRIPT_XPATH = XPathFactory.newInstance().newXPath().compile("//*[local-name()='scriptTask']");
        } catch (XPathExpressionException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Path mainFile;
    private final ActionType actionType;
    private final Set<Path> scripts;
    private final long started;
    private final boolean failed;

    public boolean isReady() {
        return ActionType.CREATE != actionType || scriptsPresent();
    }

    public boolean isFailed() {
        return failed || System.currentTimeMillis() > started + DEPLOYMENT_DEADLINE_MILLIS;
    }

    private boolean scriptsPresent() {
        return scripts.stream().map(Path::toFile).allMatch(File::exists);
    }

    private DeploymentItem(ActionType actionType, Path mainFile, Set<Path> scripts, boolean failed) {
        this.mainFile = mainFile;
        this.actionType = actionType;
        this.scripts = scripts;
        this.started = System.currentTimeMillis();
        this.failed = failed;
    }

    public static DeploymentItem create(Path bpmnFile) {
        try (InputStream is = Files.newInputStream(bpmnFile)) {
            return new DeploymentItem(ActionType.CREATE, bpmnFile,
                    parseBpmnResources(is, DEP_PREFIX).stream()
                            .map(file -> bpmnFile.getParent().resolve(file))
                            .collect(Collectors.toSet()),
                    false);
        } catch (IllegalStateException | IOException e) {
            LOG.warn("Failed to process BPMN file {}, marking as failure", bpmnFile, e);
            return new DeploymentItem(ActionType.CREATE, bpmnFile, Collections.emptySet(), true);
        }
    }

    public static DeploymentItem delete(Path bpmnFile) {
        return new DeploymentItem(ActionType.DELETE, bpmnFile, Collections.emptySet(), false);
    }

    @NonNull
    public static Set<Path> parseBpmnResources(InputStream is, String prefix) {
        return parseBpmnResources(is).stream()
                .filter(res -> res.startsWith(prefix))
                .map(res -> res.substring(prefix.length()))
                .map(Paths::get)
                .collect(Collectors.toSet());
    }

    @NonNull
    public static Set<String> parseBpmnResources(InputStream is) {
        try {
            final Set<String> result = new HashSet<>();
            final Document doc = UntrustedXML.newDocumentBuilder().parse(is);
            final NodeList nodes = (NodeList) SCRIPT_XPATH.evaluate(doc, XPathConstants.NODESET);
            LOG.debug("Size : {}", nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                Optional.ofNullable(nodes.item(i).getAttributes().getNamedItem("camunda:resource"))
                        .ifPresent(node -> result.add(node.getNodeValue()));
            }
            return result;
        } catch (SAXException | IOException | XPathExpressionException e) {
            throw new IllegalStateException("Unable to parse BPMN file", e);
        }
    }
}