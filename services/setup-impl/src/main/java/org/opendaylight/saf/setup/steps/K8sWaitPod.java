/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.setup.steps;

import com.google.common.util.concurrent.Uninterruptibles;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.opendaylight.saf.setup.ConfigStep;
import org.opendaylight.saf.setup.SetupConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wait for specific POD conditions in k8s. Step will repeat query until at least one POD match specified selectors.
 *
 * <p>
 * Example configuration:
 *
 * <p>
 *
 * <pre>
 *  k8s_wait_pod:
 *    field_selector: status.phase=Running
 *    label_selector: app=devicedb
 * </pre>
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 26, 2020
 */
public final class K8sWaitPod implements ConfigStep {
    private static final String NS_FILE = Config.SERVICEACCOUNT_ROOT + "/namespace";
    private static final Logger LOG = LoggerFactory.getLogger(K8sWaitPod.class);
    public static final K8sWaitPod INSTANCE = new K8sWaitPod();
    private String namespace;
    private boolean available;
    private ApiClient client;

    @SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
            justification = "k8s namespace file is at well known fixed location")
    private K8sWaitPod() {
        try {
            namespace = new String(Files.readAllBytes(Paths.get(NS_FILE)), StandardCharsets.UTF_8);
            client = Config.fromCluster();
            Configuration.setDefaultApiClient(client);
            LOG.info("Running in k8s in namespace '{}'", namespace);
            available = true;
        } catch (IOException e) {
            available = false;
            LOG.warn("This environment doesn't look like k8s, this step won't be available");
        }
    }

    @Override
    public String name() {
        return "k8s_wait_pod";
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void doStep(Map<String, String> properties) {
        if (!available) {
            throw new IllegalStateException("k8s client is not avalable");
        }
        for (;;) {
            final CoreV1Api api = new CoreV1Api(client);
            try {
                final V1PodList podList = api.listNamespacedPod(namespace, null, null, null,
                        properties.get(SetupConstants.PROP_FIELD_SELECTOR),
                        properties.get(SetupConstants.PROP_LABEL_SELECTOR), null, null, null, null);
                final List<V1Pod> pods = podList.getItems();
                LOG.debug("Got PODs {}", pods);
                if (!pods.isEmpty()) {
                    return;
                }
            } catch (Exception e) {
                LOG.warn("Unable to fetch POD details", e);
            }
            Uninterruptibles.sleepUninterruptibly(Duration.ofSeconds(2));
        }
    }
}
