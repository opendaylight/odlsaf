/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import static org.opendaylight.saf.devicedb.impl.Util.getHandlerUri;

import com.google.common.collect.Iterators;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.Collection;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.opendaylight.jsonrpc.impl.JsonRpcPathBuilder;
import org.opendaylight.jsonrpc.model.JSONRPCArg;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.saf.governance.client.GovernanceClient;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeviceMounter extends AbstractDeviceHelper implements DOMDataTreeChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceMounter.class);
    private static final TypeToken<Set<String>> MODULE_TT = new TypeToken<>() {
    };
    @Autowired
    private GovernanceClient governanceClient;

    @Autowired
    private DeviceHandlerProvider deviceHandlerProvider;

    @Autowired
    private OdlClient lscClient;

    @Autowired
    private OpstatePublisher statePublisher;

    private ListenerRegistration<DeviceMounter> listenerRegistration;

    @PostConstruct
    public void init() {
        final JsonObject path = JsonRpcPathBuilder.newBuilder("saf-device-database:devices")
                .container("device")
                .build();
        final DOMDataTreeIdentifier dti = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                pathCodec.deserialize(path));
        final DOMDataTreeChangeService dtcs = (DOMDataTreeChangeService) domDataBroker.getExtensions()
                .get(DOMDataTreeChangeService.class);

        listenerRegistration = dtcs.registerDataTreeChangeListener(dti, this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeCandidate> changes) {
        LOG.debug("Changes to process : {}", changes);
        for (DataTreeCandidate dtc : changes) {
            final ModificationType type = dtc.getRootNode().getModificationType();
            LOG.debug("[DTC][{}] {} => {}", type, dtc.getRootPath(), dtc.getRootNode());
            switch (type) {
                case WRITE:
                case APPEARED:
                    dtc.getRootNode()
                            .getChildNodes()
                            .stream()
                            .filter(node -> node.getDataAfter().isPresent())
                            .forEach(this::processMount);
                    break;

                case SUBTREE_MODIFIED:
                    // item was removed from list
                    dtc.getRootNode()
                            .getChildNodes()
                            .stream()
                            .filter(node -> node.getModificationType() == ModificationType.DELETE)
                            .map(node -> (NodeIdentifierWithPredicates) node.getIdentifier())
                            .map(DeviceMounter::pathArgument2Name)
                            .forEach(this::processUnMount);
                    // new item was added to list or existing item was updated
                    dtc.getRootNode()
                            .getChildNodes()
                            .stream()
                            .filter(node -> node.getModificationType() == ModificationType.WRITE)
                            .forEach(this::processMount);
                    break;

                case DELETE:
                case DISAPPEARED:
                    dtc.getRootNode()
                            .getChildNodes()
                            .stream()
                            .map(DataTreeCandidateNode::getIdentifier)
                            .filter(pa -> pa instanceof NodeIdentifierWithPredicates
                                    && ((NodeIdentifierWithPredicates) pa).size() > 0)
                            .map(pa -> (NodeIdentifierWithPredicates) pa)
                            .map(DeviceMounter::pathArgument2Name)
                            .forEach(this::processUnMount);
                    break;

                default:
                    LOG.debug("Modification ignored : [{}] [{}]", type, dtc.getRootPath());
                    // NOOP
                    break;
            }
        }
    }

    @CanIgnoreReturnValue
    private boolean processMount(DataTreeCandidateNode node) {
        LOG.debug("Mount data : {}", node);
        final String device = pathArgument2Name((NodeIdentifierWithPredicates) node.getIdentifier());
        LOG.info("Mounting device '{}'", device);
        final JsonObject deviceObj = getDevice(device, node.getDataAfter().get());
        statePublisher.publish(device, "mounting");
        final String uri = getHandlerUri(deviceObj, deviceHandlerProvider.get(device));
        LOG.debug("Handler for device '{}' : '{}'", device, uri);
        governanceClient.set(device, uri);
        statePublisher.publish(device, "completed");
        return lscClient.mount(device, getDeviceModules(deviceObj));
    }

    @CanIgnoreReturnValue
    private boolean processUnMount(String device) {
        LOG.info("Unmounting device '{}'", device);
        governanceClient.unset(device);
        statePublisher.unpublish(device);
        return lscClient.unmount(device);
    }

    private static String pathArgument2Name(NodeIdentifierWithPredicates path) {
        return Iterators.getOnlyElement(path.values().iterator()).toString();
    }

    private JsonObject getDevice(String device, NormalizedNode<?, ?> node) {
        final YangInstanceIdentifier yii = Util.yiiForDevice(pathCodec, device, LogicalDatastoreType.CONFIGURATION);
        final JSONRPCArg converted = jsonConverter.toBus(yii, node);
        return converted.getData().getAsJsonObject();
    }

    private Set<String> getDeviceModules(JsonObject data) {
        return gson.fromJson(data.get("modules"), MODULE_TT.getType());
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        listenerRegistration.close();
    }
}
