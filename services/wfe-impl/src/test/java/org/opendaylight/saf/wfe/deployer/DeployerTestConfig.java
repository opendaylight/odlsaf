/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.deployer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.support.TestPropertySourceUtils;

@Configuration
@ComponentScan(basePackageClasses = ChangeDeployer.class)
public class DeployerTestConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Bean
    public RepositoryService mockRepositoryService() {
        RepositoryService svc = mock(RepositoryService.class);

        ProcessDefinitionQuery pdq = mock(ProcessDefinitionQuery.class);
        when(pdq.deploymentId(anyString())).thenReturn(pdq);
        when(pdq.list()).thenReturn(new ArrayList<>());
        when(svc.createProcessDefinitionQuery()).thenReturn(pdq);
        return svc;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                    "workspace=" + Files.createTempDirectory("wfe"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
