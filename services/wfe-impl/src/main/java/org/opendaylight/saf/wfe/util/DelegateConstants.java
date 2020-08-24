/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.util;

public final class DelegateConstants {

    public static final String ARRAY_ELEMENT = "array-element";
    public static final String WORKFLOW_NAME = "workflow-name";
    public static final String WORKFLOW_INPUT = "workflow-input";
    public static final String WORKFLOW_INPUT_STRING = "workflow-input-string";

    public static final String YANG_PATH = "yang-path";
    public static final String DEVICE_ID = "device-id";
    public static final String GET_REQUEST = "GET";
    public static final String DELETE_REQUEST = "DELETE";
    public static final String PUT_REQUEST = "PUT";
    public static final String RPC_POST_REQUEST = "POST";
    public static final String PRECHECKS = "prechecks";
    public static final String POSTCHECKS = "postchecks";
    public static final String CONFIG_DATA = "config-data";
    public static final String MOUNTPOINT_URL_BASE = "restconf/config/jsonrpc:config/configured-endpoints/";
    public static final String RESTCONF_BASE = "restconf/";
    public static final String YANGEXT_MOUNT = "/yang-ext:mount/";
    public static final String JSONRPC_BASE_URI = "config/jsonrpc:config/configured-endpoints/";
    public static final String FILE_NAME = "filename";
    public static final String ENTITY = "entity";
    public static final String INVALID_YANGPATH = "Empty or Null Yang Path specified";
    public static final String ILLEGAL_ARG = "Invalid arguments";
    public static final String DATA_NOT_FOUND = "Data not found: Invalid value in Yang Path";
    public static final String LSC_USERNAME = "lsc_username";
    public static final String LSC_PASSWORD = "lsc_password";
    public static final String LSC_HOSTNAME = "lsc";
    public static final String BACKUP_SUCCESS_DEVICES = "backupSuccessDevices";
    public static final String USER_DATA = "user-data";

    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String CONTENT = "content";

    private DelegateConstants() {
        // utility class constructor
    }
}
