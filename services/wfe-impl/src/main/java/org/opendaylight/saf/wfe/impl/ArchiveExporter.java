/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import static org.opendaylight.saf.wfe.impl.model.DeploymentItem.parseBpmnResources;

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.Resource;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExportInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExportOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
    justification = "In this setup Path#resolve won't return NULL")
public class ArchiveExporter {
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveExporter.class);
    private static final String BUILTIN_DEPLOYMENT_NAME = "wfe";
    private final Path exportDirectory;

    @Autowired
    private RepositoryService repositoryService;

    public ArchiveExporter(@Value("${workspace}") Path workspace) throws IOException {
        exportDirectory = workspace.getParent().resolve("exports");
        Files.createDirectories(exportDirectory);
        LOG.info("BPMN archives will be created under {}", exportDirectory);
    }

    public ExportOutput export(ExportInput input) {
        boolean includeBuiltin = Optional.ofNullable(input.getIncludeDefault()).orElse(false);
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String ts = sdf.format(new Date());
        final Path zipFile = exportDirectory.resolve("WFDsArchive_" + ts + ".zip");
        String exportedFile = zipFile.getFileName().toString();
        LOG.info("Exporting BPMN archive to {}", zipFile);
        final Set<String> exported = new HashSet<>();
        final List<Deployment> deployments = repositoryService.createDeploymentQuery()
                .list()
                .stream()
                .filter(d -> !d.getName().equals(BUILTIN_DEPLOYMENT_NAME))
                .collect(Collectors.toList());
        // assume error first
        boolean success = false;
        Optional<String> error = Optional.empty();
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (Deployment deployment : deployments) {
                exportDeployment(input, exported, zos, deployment);
            }
            if (includeBuiltin) {
                final Deployment builtinDeployment = repositoryService.createDeploymentQuery()
                        .deploymentName(BUILTIN_DEPLOYMENT_NAME)
                        .singleResult();
                exportDeployment(null, exported, zos, builtinDeployment);
            }
            success = true;
        } catch (IOException e) {
            LOG.error("Error while exporting the zip file with Bpmn files", e);
            error = Optional.ofNullable(e.getMessage());
        }

        if (exported.isEmpty()) {
            success = false;
            exportedFile = "";
            error = Optional.of("No process definition matched search filter");
            LOG.warn("Filter options '{}' didn't yield any results and thus archive file will be empty."
                    + "Archive file will be remmoved", input.getFilter());
            try {
                Files.delete(zipFile);
            } catch (IOException e) {
                LOG.warn("Unable to remove empty archive, ignoring", e);
            }
        }

        return ExportOutput.builder()
                .success(success)
                .error(error.orElse(""))
                .filename(exportedFile)
                .build();
    }

    private void exportDeployment(ExportInput input, final Set<String> exported, ZipOutputStream zos,
            Deployment deployment) throws IOException {
        LOG.info("Deployment : {}:{}", deployment.getName(), deployment.getId());
        final String deploymentId = deployment.getId();
        final List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .list()
                .stream()
                .filter(filterProcessDefinitions(input))
                .collect(Collectors.toList());

        for (ProcessDefinition processDefinition : processDefinitions) {
            LOG.info(" Process defnition : {}", processDefinition.getId());
            appendProcessResources(exported, zos, processDefinition);
        }
    }

    private static Predicate<ProcessDefinition> filterProcessDefinitions(ExportInput input) {
        if (input == null || Strings.isNullOrEmpty(input.getFilter())) {
            return t -> true;
        }
        return pd -> Pattern.compile(input.getFilter()).matcher(pd.getKey()).find();
    }

    private void appendProcessResources(Set<String> exported, ZipOutputStream zos, ProcessDefinition def)
            throws IOException {

        // get name of scripts that are mentioned in BPMN
        // these are part of deployment only in user-supplied process definitions
        // for built-in process definitions, these are just on classpath
        final Set<String> parsed = parseBpmnResources(repositoryService.getProcessModel(def.getId())).stream()
                .filter(path -> !exported.contains(path))
                .collect(Collectors.toSet());

        LOG.debug("Parsed resources : {}", parsed);

        for (String resource : parsed) {
            if (resource.startsWith("deployment://")) {
                final String name = resource.split("deployment://")[1];
                if (!exported.contains(name)) {
                    processDeploymentResource(exported, zos, name, def.getDeploymentId());
                }
            } else {
                if (!exported.contains(resource)) {
                    processClasspathResource(exported, zos, resource);
                }
            }
        }

        // get name of resources in deployment
        final List<Resource> resources = repositoryService.getDeploymentResources(def.getDeploymentId());
        for (Resource resource : resources) {
            if (exported.contains(resource.getName())) {
                continue;
            }
            processDeploymentResource(exported, zos, resource.getName(), def.getDeploymentId());
        }
    }

    private void appendFromStream(Set<String> exported, ZipOutputStream zos, String name, InputStream stream)
            throws IOException {
        LOG.info("  File {}", name);
        zos.putNextEntry(new ZipEntry(name));
        stream.transferTo(zos);
        zos.closeEntry();
        exported.add(name);
    }

    private void processClasspathResource(Set<String> exported, ZipOutputStream zos, String name) throws IOException {
        try (InputStream is = Resources.getResource(name).openStream()) {
            appendFromStream(exported, zos, name, is);
        }
    }

    private void processDeploymentResource(Set<String> exported, ZipOutputStream zos, String name, String deploymentId)
            throws IOException {
        try (InputStream is = repositoryService.getResourceAsStream(deploymentId, name)) {
            appendFromStream(exported, zos, name, is);
        }
    }
}
