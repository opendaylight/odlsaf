/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.setup;

import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.yaml.snakeyaml.Yaml;

public final class ConfigLoader {
    private static final Yaml YAML = new Yaml();

    private ConfigLoader() {
        // NOOP
    }

    public static Config fromFile(Path configFile) throws IOException {
        try (InputStream is = Files.newInputStream(configFile)) {
            return fromInputStream(is);
        }
    }

    public static Config loadDefault() throws IOException {
        try (InputStream is = Resources.getResource(ConfigLoader.class, "/default-config.yaml").openStream()) {
            return fromInputStream(is);
        }
    }

    public static void updateTokens(ConfigurableBeanFactory beanFactory, Config config) {
        for (Entry<String, String> entry : config.getTokens().entrySet()) {
            try {
                entry.setValue(beanFactory.resolveEmbeddedValue("${" + entry.getKey() + "}"));
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
    }

    public static void replaceTokens(Config config) {
        config.getSteps().stream().forEach(cs -> {
            cs.getProperties().entrySet().forEach(entry -> {
                String value = String.valueOf(entry.getValue());
                config.getTokens()
                        .entrySet()
                        .forEach(token -> {
                            // value = value.replace("%" + token.getKey() + "%", token.getValue());
                            entry.setValue(entry.getValue());
                        });
            });
        });
    }

    @SuppressWarnings("unchecked")
    private static Config fromInputStream(InputStream is) {
        final Config cfg = new Config();
        final Map<String, Object> map = YAML.load(is);
        if (map.containsKey(SetupConstants.PROP_TOKENS)) {
            cfg.setTokens((Map<String, String>) map.get(SetupConstants.PROP_TOKENS));
        }
        if (map.containsKey(SetupConstants.PROP_STEPS)) {
            final List<Map<String, Map<String, String>>> steps = (List<Map<String, Map<String, String>>>) map
                    .get(SetupConstants.PROP_STEPS);
            cfg.setSteps(steps.stream()
                    .map(Map::entrySet)
                    .map(Iterables::getOnlyElement)
                    .map(entry -> new Config.Step(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList()));
        }
        return cfg;
    }
}
