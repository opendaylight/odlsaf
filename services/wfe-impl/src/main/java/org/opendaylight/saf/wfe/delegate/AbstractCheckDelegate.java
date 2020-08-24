/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.delegate;

import com.google.gson.reflect.TypeToken;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.opendaylight.saf.wfe.impl.model.CheckArg;
import org.opendaylight.saf.wfe.impl.model.CheckRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractCheckDelegate extends AbstractDelegate implements JavaDelegate {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractCheckDelegate.class);
    private static final TypeToken<List<CheckRequest>> TT = new TypeToken<>() {
    };

    protected final Map<String, Predicate<CheckArg>> checks = new HashMap<>();

    @SuppressWarnings("squid:S1125")
    @Override
    public void execute(DelegateExecution execution) {
        final List<CheckRequest> items = getInput(execution, TT);
        boolean result = items.stream().map(this::check).allMatch(t -> t == true);
        execution.setVariable("CheckSuccess", result);
    }

    protected boolean check(CheckRequest request) {
        return Optional.ofNullable(getChecks(request))
                .orElse(Collections.emptyList())
                .stream()
                .filter(checks::containsKey)
                .map(check -> CheckArg.fromRequest(request, check))
                .map(this::performCheck)
                .allMatch(result -> result == true);
    }

    protected abstract List<String> getChecks(CheckRequest request);

    protected boolean performCheck(CheckArg arg) {
        LOG.info("Running check '{}' on path '{}' using config '{}'", arg.getCheck(), arg.getYangPath(),
                arg.getConfigData());
        return checks.get(arg.getCheck()).test(arg);
    }
}
