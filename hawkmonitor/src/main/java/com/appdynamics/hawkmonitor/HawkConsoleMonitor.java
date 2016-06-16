package com.appdynamics.hawkmonitor;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

import COM.TIBCO.hawk.utilities.misc.HawkConstants;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Created by trader on 5/12/16.
 */
public class HawkConsoleMonitor extends AManagedMonitor {

    private boolean isInitialized;
    private boolean isDisabled;
    private int retries;
    private boolean trackActivities;
    private String metricPathPrefix;
    private HawkConsoleMetrics metricManager;

    private static final String KEY_RETRIES = "retries";
    private static final String KEY_ENABLE_ACTIVITY = "enable-activity-tracking";
    private static final String KEY_METRIC_PFX = "metric-path-prefix";

    private static final String KEY_CONN_METHOD = "connection-method";

    private static final String KEY_HAWK_DOM = "hawk-domain";
    private static final String KEY_RV_SVC = "rv-service";
    private static final String KEY_RV_NET = "rv-network";
    private static final String KEY_RV_DAEMON = "rv-daemon";
    private static final String KEY_AS_LISTEN_URL = "as-listen-url";
    private static final String KEY_AS_DISCOVER_URL = "as-discover-url";
    private static final String KEY_AS_MEMBER_NAME = "as-member-name";
    private static final String KEY_AS_TRANS_TIMEOUT = "as-transport-timeout";
    private static final String KEY_AS_RCV_BUF_SZ = "as-rcv-buffer-size";
    private static final String KEY_AS_VNODE_CT = "as-vnode-count";
    private static final String KEY_AS_WORKER_CT = "as-worker-count";
    private static final String KEY_EMS_URL = "ems-url";
    private static final String KEY_EMS_USER_NAME = "ems-username";
    private static final String KEY_EMS_PASSWORD = "ems-password";

    private static final String VAL_RV = "rv";
    private static final String VAL_AS = "as";
    private static final String VAL_EMS = "ems";
    private static final String[] VALID_VALS = {VAL_RV, VAL_AS, VAL_EMS};

    private static final int RETRIES_DEFAULT = 20;

    private static final Logger logger = Logger.getLogger("com.singularity.TibcoHawkConsoleMonitor");

    public HawkConsoleMonitor() {
        isInitialized = false;
        isDisabled = false;
        retries = RETRIES_DEFAULT;
        metricManager = null;
    }

    public TaskOutput execute(Map<String, String> args, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {

        if (!isInitialized) {

            try {
                retries = Integer.parseInt(args.get(KEY_RETRIES));
            } catch (NumberFormatException nfe) {
                logger.error("Invalid value for " + KEY_RETRIES + ", using default value of " + retries);
            } catch (NullPointerException npe) {
                logger.error("Required value for " + KEY_RETRIES + " missing, using default value of " + retries);
            }

            trackActivities = Boolean.parseBoolean(args.get(KEY_ENABLE_ACTIVITY));

            metricPathPrefix = getRequiredVariable(args, KEY_METRIC_PFX);
            if (metricPathPrefix == null) {
                logger.error("Required value for " + KEY_METRIC_PFX + " missing, disabling");
                isDisabled = true;
            } else {
                Map<String, Object> connectParams = getConnectParams(args);
                if (connectParams == null) {
                    // error already logged
                    isDisabled = true;
                } else {
                    try {
                        metricManager = new HawkConsoleMetrics(connectParams);
                    } catch (Exception e) {
                        logger.error("Fatal error connecting to the Hawk Console, disabling Tibco Hawk Console monitor", e);
                        isDisabled = true;
                    }
                }
            }
        }

        metricManager.tick();

        if (isDisabled) {
            return new TaskOutput("Failure");
        } else {
            return new TaskOutput("Success");
        }
    }

    private String getRequiredVariable(Map<String, String> args, String key) {
        String result = args.get(key);
        if (result == null) {
            logger.error("Required value for " + key + " missing, disabling");
            isDisabled = true;
        }

        return result;
    }

    private Map<String, Object> getConnectParams(Map<String, String> args) {

        Map<String, Object> result = null;
        String hawkDomain = null;
        String connMethod = getRequiredVariable(args, KEY_CONN_METHOD);
        if (connMethod != null) hawkDomain = getRequiredVariable(args, KEY_HAWK_DOM);
        if (hawkDomain != null) {
            if (connMethod.equals(VAL_RV)) {
                result = getRVVals(args);
            } else if (connMethod.equals(VAL_AS)) {
                result = getASVals(args);
            } else if (connMethod.equals(VAL_EMS)) {
                result = getEMSVals(args);
            } else {
                StringBuilder sb = new StringBuilder("Illegal value for ");
                sb.append(KEY_CONN_METHOD);
                sb.append(", legal values are { ");
                for (int i = 0; i < VALID_VALS.length; ++i) {
                    sb.append(VALID_VALS[i]);
                    if (i < VALID_VALS.length - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(" }, disabling");
                logger.error(sb.toString());
            }

            if (result != null) {
                result.put(HawkConstants.HAWK_DOMAIN, hawkDomain);
            }
        }

        return result;
    }

    private Map<String, Object> getRVVals(Map<String, String> args) {

        Map<String, Object> result = null;

        String rvNetwork = null;
        String rvDaemon = null;

        String rvService = getRequiredVariable(args, KEY_RV_SVC);
        if (rvService != null) rvNetwork = getRequiredVariable(args, KEY_RV_NET);
        if (rvNetwork != null) rvDaemon = args.get(KEY_RV_DAEMON);
        if (rvDaemon != null) {
            result = new HashMap<String, Object>();
            result.put(HawkConstants.HAWK_TRANSPORT, HawkConstants.HAWK_TRANSPORT_TIBRV);
            result.put(HawkConstants.RV_SERVICE, rvService);
            result.put(HawkConstants.RV_NETWORK, rvNetwork);
            result.put(HawkConstants.RV_DAEMON, rvDaemon);
        }

        return result;
    }

    private Map<String, Object> getASVals(Map<String, String> args) {

        Map<String, Object> result = null;

        String listenURL = getRequiredVariable(args, KEY_AS_LISTEN_URL);
        if (listenURL != null) {
            result = new HashMap<String, Object>();
            result.put(HawkConstants.HAWK_TRANSPORT, HawkConstants.HAWK_TRANSPORT_TIBAS);
            result.put(HawkConstants.PROP_AS_LISTEN_URL, listenURL);

            // The rest of the props are optional
            String discoverURL = args.get(KEY_AS_DISCOVER_URL);
            if (discoverURL != null) result.put(HawkConstants.PROP_AS_DISCOVER_URL, discoverURL);
            String memberName = args.get(KEY_AS_MEMBER_NAME);
            if (memberName != null) result.put(HawkConstants.PROP_AS_MEMBER_NAME, memberName);

            // TODO: the docs are unclear on whether the integer-valued props
            // should have a String, int or long data type in the map
            String transportTimeoutStr = args.get(KEY_AS_TRANS_TIMEOUT);
            if (transportTimeoutStr != null) {
                try {
                    long timeoutVal = Long.parseLong(transportTimeoutStr);
                    result.put(HawkConstants.PROP_TRANSPORT_TIMEOUT, timeoutVal);
                } catch (NumberFormatException nfe) {
                    logger.error("Ignoring non-integer value for " + KEY_AS_TRANS_TIMEOUT + ": " + transportTimeoutStr);
                }
            }

            String rcvBufSzStr = args.get(KEY_AS_RCV_BUF_SZ);
            if (rcvBufSzStr != null) {
                try {
                    long bufSize = Long.parseLong(rcvBufSzStr);
                    result.put(HawkConstants.PROP_AS_RECEIVE_BUFFER_SIZE, bufSize);
                } catch (NumberFormatException nfe) {
                    logger.error("Ignoring non-integer value for " + KEY_AS_RCV_BUF_SZ + ": " + rcvBufSzStr);
                }
            }

            String vnodeCountStr = args.get(KEY_AS_VNODE_CT);
            if (vnodeCountStr != null) {
                try {
                    long vnodeCount = Long.parseLong(vnodeCountStr);
                    result.put(HawkConstants.PROP_AS_VIRTUAL_NODE_COUNT, vnodeCount);
                } catch (NumberFormatException nfe) {
                    logger.error("Ignoring non-integer value for " + KEY_AS_VNODE_CT + ": " + vnodeCountStr);
                }
            }

            String workerCountStr = args.get(KEY_AS_WORKER_CT);
            if (workerCountStr != null) {
                try {
                    long workerCount = Long.parseLong(workerCountStr);
                    result.put(HawkConstants.PROP_AS_WORKER_THREAD_COUNT, workerCount);
                } catch (NumberFormatException nfe) {
                    logger.error("Ignoring non-integer value for " + KEY_AS_WORKER_CT + ": " + workerCountStr);
                }
            }
        }

        return result;
    }

    private Map<String, Object> getEMSVals(Map<String, String> args) {

        Map<String, Object> result = null;

        String emsURL = getRequiredVariable(args, KEY_EMS_URL);
        if (emsURL != null) {
            result = new HashMap<String, Object>();
            result.put(HawkConstants.HAWK_TRANSPORT, HawkConstants.HAWK_TRANSPORT_TIBEMS);
            result.put(HawkConstants.HAWK_EMS_URL, emsURL);

            // The rest of the props are optional
            String userName = args.get(KEY_EMS_USER_NAME);
            if (userName != null) result.put(HawkConstants.HAWK_EMS_USERNAME, userName);
            String pwd = args.get(KEY_EMS_PASSWORD);
            if (pwd != null) result.put(HawkConstants.HAWK_EMS_PWD, pwd);
        }

        return result;
    }
}
