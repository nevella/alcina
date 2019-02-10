/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.servlet.servlet;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.ui.SuggestOracle.Response;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.UnexpectedException;
import com.google.gwt.user.server.rpc.impl.LegacySerializationPolicy;
import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.ActionResult;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.RemoteActionPerformer;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.KnownsDelta;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaResult;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaSpec;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DeltaApplicationRecordSerializerImpl;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.permissions.ReadOnlyException;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceExt;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.actions.RequiresHttpSession;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.TransformConflicts;
import cc.alcina.framework.entity.domaintransform.TransformConflicts.TransformConflictsFromOfflineSupport;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.ServerValidatorHandler;
import cc.alcina.framework.entity.entityaccess.WrappedObject;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetricData;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetrics;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;
import cc.alcina.framework.entity.util.DataFolderProvider;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType.BoundSuggestOracleModel;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType.BoundSuggestOracleSuggestion;
import cc.alcina.framework.servlet.CookieHelper;
import cc.alcina.framework.servlet.ServletLayerObjects;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.ServletLayerValidatorHandler;
import cc.alcina.framework.servlet.SessionHelper;
import cc.alcina.framework.servlet.SessionProvider;
import cc.alcina.framework.servlet.Sx;
import cc.alcina.framework.servlet.authentication.AuthenticationException;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.knowns.KnownsDeltaRequestHandler;

/**
 *
 * Tests (todo) for transform persistence: invalid clientauth multiple
 * simultaneous (identical clientinstance, non-) cross-server-restart
 *
 * <p>
 * Readonly: most checks happen of simple methods happen at the persistence
 * layer so not needed here
 * </p>
 *
 * @author nick@alcina.cc
 *
 */
public abstract class CommonRemoteServiceServlet extends RemoteServiceServlet
        implements CommonRemoteServiceExt {
    public static final String UA_NULL_SERVER = "null/server";

    public static final String THRD_LOCAL_RPC_RQ = "THRD_LOCAL_RPC_RQ";

    public static final String THRD_LOCAL_RPC_PAYLOAD = "THRD_LOCAL_RPC_PAYLOAD";

    public static final String CONTEXT_USE_WRAPPER_USER_WHEN_PERSISTING_OFFLINE_TRANSFORMS = CommonRemoteServiceServlet.class
            .getName() + "."
            + "CONTEXT_USE_WRAPPER_USER_WHEN_PERSISTING_OFFLINE_TRANSFORMS";

    public static final String CONTEXT_REUSE_IUSER_HOLDER = CommonRemoteServiceServlet.class
            .getName() + ".CONTEXT_REUSE_IUSER_HOLDER";

    public static final String CONTEXT_RPC_USER_ID = CommonRemoteServiceServlet.class
            .getName() + ".CONTEXT_RPC_USER_ID";

    public static final String CONTEXT_OVERRIDE_CONTEXT = CommonRemoteServiceServlet.class
            .getName() + ".CONTEXT_OVERRIDE_CONTEXT";

    public static final String CONTEXT_THREAD_LOCAL_HTTP_REQUEST = CommonRemoteServiceServlet.class
            .getName() + ".CONTEXT_THREAD_LOCAL_HTTP_REQUEST";

    public static final String CONTEXT_NO_ACTION_LOG = CommonRemoteServiceServlet.class
            .getName() + ".CONTEXT_NO_ACTION_LOG";

    public static final String PUSH_TRANSFORMS_AT_END_OF_REUQEST = CommonRemoteServiceServlet.class
            .getName() + ".PUSH_TRANSFORMS_AT_END_OF_REUQEST";

    public static boolean DUMP_STACK_TRACE_ON_OOM = true;

    public static HttpServletRequest getContextThreadLocalRequest() {
        return LooseContext.get(
                CommonRemoteServiceServlet.CONTEXT_THREAD_LOCAL_HTTP_REQUEST);
    }

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private int actionCount = 0;

    private ThreadLocal<Integer> looseContextDepth = new ThreadLocal<>();

    @Override
    @WebMethod(readonlyPermitted = true)
    public void dumpData(String data) {
        if (!ResourceUtilities.getBoolean(CommonRemoteServiceServlet.class,
                "dumpPermitted")) {
            throw new RuntimeException("Dump not permitted");
        }
        String key = String.valueOf(System.currentTimeMillis());
        File dir = getDataDumpsFolder();
        String path = CommonUtils.formatJ("%s/%s.dat", dir.getPath(), key);
        File file = new File(path);
        try {
            ResourceUtilities.writeStringToFile(data, file);
            System.out.println("Client db dumped - key: " + key);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @WebMethod(readonlyPermitted = true)
    public <T extends HasIdAndLocalId> T getItemById(String className, Long id)
            throws WebException {
        try {
            Class<T> clazz = (Class<T>) Class.forName(className);
            return Registry.impl(CommonPersistenceProvider.class)
                    .getCommonPersistence().getItemById(clazz, id, true, false);
        } catch (Exception e) {
            throw new WebException(e.getMessage());
        }
    }

    @Override
    public KnownsDelta getKnownsDelta(long since) {
        return new KnownsDeltaRequestHandler().getDelta(since,
                Ax.format("client-instance:%s",
                        PermissionsManager.get().getClientInstance().getId()));
    }

    @Override
    @WebMethod(readonlyPermitted = true)
    public List<ActionLogItem> getLogsForAction(RemoteAction action,
            Integer count) {
        checkAnnotatedPermissions(action);
        return Registry.impl(CommonPersistenceProvider.class)
                .getCommonPersistence()
                .listLogItemsForClass(action.getClass().getName(), count);
    }

    @Override
    @WebMethod(readonlyPermitted = true)
    public List<ObjectDeltaResult> getObjectDelta(List<ObjectDeltaSpec> specs)
            throws WebException {
        try {
            return Registry.impl(CommonPersistenceProvider.class)
                    .getCommonPersistence().getObjectDelta(specs);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebException(e);
        }
    }

    public void initUserStateWithCookie(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        new CookieHelper().getIid(httpServletRequest, httpServletResponse);
        Registry.impl(SessionHelper.class).initUserState(httpServletRequest,
                httpServletResponse);
        String userName = new CookieHelper()
                .getRememberedUserName(httpServletRequest, httpServletResponse);
        if (userName != null && !PermissionsManager.get().isLoggedIn()) {
            try {
                LoginResponse lrb = new LoginResponse();
                lrb.setOk(true);
                processValidLogin(lrb, userName, httpServletRequest,
                        httpServletResponse);
            } catch (AuthenticationException e) {
                // ignore
            }
        }
    }

    @Override
    @WebMethod(readonlyPermitted = true)
    public List<String> listRunningJobs() {
        return JobRegistry.get().getRunningJobs();
    }

    @Override
    public String loadData(String key) {
        if (!ResourceUtilities.getBoolean(CommonRemoteServiceServlet.class,
                "loadDumpPermitted")) {
            throw new RuntimeException("Load dump not permitted");
        }
        File dir = getDataDumpsFolder();
        String path = CommonUtils.formatJ("%s/%s.dat", dir.getPath(), key);
        File file = new File(path);
        try {
            return ResourceUtilities.readFileToString(file);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    @WebMethod(readonlyPermitted = true, customPermission = @Permission(access = AccessLevel.EVERYONE))
    public Long logClientError(String exceptionToString) {
        return logClientError(exceptionToString,
                LogMessageType.CLIENT_EXCEPTION.toString());
    }

    @Override
    @WebMethod(readonlyPermitted = true, customPermission = @Permission(access = AccessLevel.EVERYONE))
    public Long logClientError(String exceptionToString, String exceptionType) {
        String remoteAddr = getRemoteAddress();
        try {
            exceptionToString = CommonUtils.nullToEmpty(exceptionToString)
                    .replace('\0', ' ');
            LooseContext.pushWithKey(
                    CommonPersistenceBase.CONTEXT_CLIENT_IP_ADDRESS,
                    remoteAddr);
            return Registry.impl(CommonPersistenceProvider.class)
                    .getCommonPersistence()
                    .log(exceptionToString, exceptionType);
        } finally {
            LooseContext.pop();
        }
    }

    @Override
    public void logClientRecords(String serializedLogRecords) {
        Converter<String, ClientLogRecords> converter = new Converter<String, ClientLogRecord.ClientLogRecords>() {
            @Override
            public ClientLogRecords convert(String original) {
                try {
                    return new AlcinaBeanSerializerS().deserialize(original);
                } catch (Exception e) {
                    System.out.format(
                            "problem deserializing clientlogrecord:\n%s\n",
                            original);
                    e.printStackTrace();
                    if (ResourceUtilities.getBoolean(
                            CommonRemoteServiceServlet.class,
                            "throwLogClientRecordExceptions")) {
                        throw new WrappedRuntimeException(e);
                    }
                    return null;
                }
            }
        };
        List<String> lines = Arrays.asList(serializedLogRecords.split("\n"));
        List<ClientLogRecords> records = CollectionFilters.convert(lines,
                converter);
        while (records.remove(null)) {
        }
        String remoteAddr = getRemoteAddress();
        for (ClientLogRecords r : records) {
            for (ClientLogRecord clr : r.getLogRecords()) {
                clr.setIpAddress(remoteAddr);
                sanitiseClrString(clr);
            }
        }
        Registry.impl(CommonPersistenceProvider.class).getCommonPersistence()
                .persistClientLogRecords(records);
    }

    public void logRpcException(Exception ex) {
        logRpcException(ex, LogMessageType.RPC_EXCEPTION.toString());
    }

    public void logRpcException(Exception ex, String exceptionType) {
        String remoteAddr = getRemoteAddress();
        try {
            LooseContext.pushWithKey(
                    CommonPersistenceBase.CONTEXT_CLIENT_IP_ADDRESS,
                    remoteAddr);
            RPCRequest rpcRequest = getThreadRpcRequest();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String msg = "RPC exception:\n";
            if (rpcRequest != null) {
                msg = describeRpcRequest(rpcRequest, msg);
            }
            msg += "\nStacktrace:\t " + sw.toString();
            System.out.println(msg);
            CommonPersistenceLocal cpl = Registry
                    .impl(CommonPersistenceProvider.class)
                    .getCommonPersistence();
            cpl.log(msg, exceptionType);
        } finally {
            LooseContext.pop();
        }
    }

    /**
     * Note - don't (normally) call this server-side, particularly in a loop,
     * since it spawns a potentially unlimited number of performers
     */
    @Override
    public String performAction(final RemoteAction action) {
        checkAnnotatedPermissions(action);
        final RemoteActionPerformer performer = (RemoteActionPerformer) Registry
                .get().instantiateSingle(RemoteActionPerformer.class,
                        action.getClass());
        // because we're spawning the thread, we use this pattern to allow for
        // getting to the countDown() in the spawned thread before the await()
        // in the launcher
        ActionLauncherAsync async = new ActionLauncherAsync(
                performer.getClass().getSimpleName() + " - " + (++actionCount),
                action);
        JobTracker tracker = async.launchAndWaitForTracker();
        return tracker.getId();
    }

    @Override
    public ActionLogItem performActionAndWait(final RemoteAction action)
            throws WebException {
        return new ActionLauncher().performActionAndWait(action).actionLogItem;
    }

    public <T> ActionResult<T> performActionAndWaitForObject(
            final RemoteAction action) throws WebException {
        return new ActionLauncher().performActionAndWait(action);
    }

    @Override
    public <G extends WrapperPersistable> Long persist(G gwpo)
            throws WebException {
        try {
            Long id = Registry.impl(CommonPersistenceProvider.class)
                    .getCommonPersistence().persist(gwpo);
            ServletLayerTransforms.get().handleWrapperTransforms();
            return id;
        } catch (Exception e) {
            logger.warn("Exception in persist wrappable", e);
            throw new WebException(e.getMessage());
        }
    }

    // TODO - well, lock sync
    @Override
    public void persistOfflineTransforms(
            List<DeltaApplicationRecord> uncommitted) throws WebException {
        persistOfflineTransforms(uncommitted, logger, true, false);
    }

    public int persistOfflineTransforms(
            List<DeltaApplicationRecord> uncommitted, Logger logger)
            throws WebException {
        return persistOfflineTransforms(uncommitted, logger, null, false);
    }

    public int persistOfflineTransforms(List<DeltaApplicationRecord> records,
            Logger logger, Boolean useWrapperUser,
            boolean throwPersistenceExceptions) throws WebException {
        CommonPersistenceLocal cp = Registry
                .impl(CommonPersistenceProvider.class).getCommonPersistence();
        boolean persistAsOneTransaction = persistOfflineTransformsAsOneTransaction();
        try {
            // save a copy of the records
            {
                String folderName = Ax.format("cli_%s_time_%s",
                        records.get(0).getClientInstanceId(), CommonUtils
                                .formatDate(new Date(), DateStyle.TIMESTAMP));
                File offlineDir = DataFolderProvider.get()
                        .getChildFile("offlineTransforms-partial");
                File saveDir = SEUtilities.getChildFile(offlineDir, folderName);
                saveDir.mkdirs();
                DeltaApplicationRecordSerializerImpl recordSerializer = new DeltaApplicationRecordSerializerImpl();
                for (DeltaApplicationRecord record : records) {
                    int id = record.getRequestId();
                    long clientInstanceId = record.getClientInstanceId();
                    String fileName = String.format("%s_%s_ser.txt",
                            clientInstanceId, id);
                    File out = SEUtilities.getChildFile(saveDir, fileName);
                    ResourceUtilities.write(recordSerializer.write(record),
                            out);
                }
                logger.info("Wrote {} offline/bulk records to {}",
                        records.size(), saveDir);
            }
            Class<? extends ClientInstance> clientInstanceClass = cp
                    .getImplementation(ClientInstance.class);
            Class<? extends DomainTransformRequestPersistent> dtrClass = cp
                    .getImplementation(DomainTransformRequestPersistent.class);
            long currentClientInstanceId = 0;
            int committed = 0;
            LooseContext.getContext().pushWithKey(
                    TransformConflicts.CONTEXT_OFFLINE_SUPPORT,
                    new TransformConflictsFromOfflineSupport());
            ReuseIUserHolder reuseIUserHolder = LooseContext
                    .get(CONTEXT_REUSE_IUSER_HOLDER);
            IUser wrapperUser = reuseIUserHolder == null ? null
                    : reuseIUserHolder.iUser;
            long idCounter = 1;
            List<DomainTransformRequest> toCommit = new ArrayList<>();
            for (int idx = 0; idx < records.size(); idx++) {
                DeltaApplicationRecord deltaRecord = records.get(idx);
                long clientInstanceId = deltaRecord.getClientInstanceId();
                int requestId = deltaRecord.getRequestId();
                DomainTransformRequest alreadyWritten = cp
                        .getItemByKeyValueKeyValue(dtrClass,
                                "clientInstance.id", clientInstanceId,
                                "requestId", requestId);
                if (alreadyWritten != null) {
                    if (logger != null) {
                        logger.warn(CommonUtils.formatJ(
                                "Request [%s/%s] already written", requestId,
                                clientInstanceId));
                    }
                    continue;
                }
                DomainTransformRequest rq = DomainTransformRequest.fromString(
                        deltaRecord.getText(),
                        deltaRecord.getChunkUuidString());
                ClientInstance clientInstance = clientInstanceClass
                        .newInstance();
                clientInstance.setAuth(deltaRecord.getClientInstanceAuth());
                clientInstance.setId(deltaRecord.getClientInstanceId());
                rq.setClientInstance(clientInstance);
                if (useWrapperUser == null) {
                    useWrapperUser = PermissionsManager.get().isAdmin()
                            && LooseContext.getContext().getBoolean(
                                    CONTEXT_USE_WRAPPER_USER_WHEN_PERSISTING_OFFLINE_TRANSFORMS)
                            && deltaRecord.getUserId() != PermissionsManager
                                    .get().getUserId();
                }
                DomainTransformLayerWrapper transformLayerWrapper;
                // TODO - perhaps allow facility to persist multi-user
                // transforms. but...perhaps better not (keep as is)
                // NOTE - at the mo, if all are pushed as transactional, just
                // the last clientInstance is used
                rq.setRequestId(deltaRecord.getRequestId());
                rq.setTag(deltaRecord.getTag());
                // necessary because event id is used by transformpersister
                // for
                // pass control etc
                for (DomainTransformEvent event : rq.getEvents()) {
                    event.setEventId(idCounter++);
                    event.setCommitType(CommitType.TO_STORAGE);
                }
                try {
                    if (useWrapperUser) {
                        if (!PermissionsManager.get().isAdmin()) {
                            if (!cp.validateClientInstance(
                                    deltaRecord.getClientInstanceId(),
                                    deltaRecord.getClientInstanceAuth())) {
                                throw new RuntimeException(
                                        "invalid wrapper authentication");
                            }
                        }
                        if (wrapperUser != null && wrapperUser
                                .getId() == deltaRecord.getUserId()) {
                        } else {
                            wrapperUser = Registry
                                    .impl(CommonPersistenceProvider.class)
                                    .getCommonPersistence().getCleanedUserById(
                                            deltaRecord.getUserId());
                            if (reuseIUserHolder != null) {
                                reuseIUserHolder.iUser = wrapperUser;
                            }
                        }
                        PermissionsManager.get().pushUser(wrapperUser,
                                LoginState.LOGGED_IN);
                    } else {
                        rq.getClientInstance()
                                .setUser(PermissionsManager.get().getUser());
                    }
                    boolean last = idx == records.size() - 1;
                    if (!persistAsOneTransaction || last) {
                        if (last) {
                            rq.getPriorRequestsWithoutResponse()
                                    .addAll(toCommit);
                        }
                        transformLayerWrapper = ServletLayerTransforms.get()
                                .transform(rq, true, true, true);
                        ThreadlocalTransformManager.cast().resetTltm(null);
                        if (logger != null) {
                            logger.info(CommonUtils.formatJ(
                                    "Request [%s::%s] : %s transforms written, %s ignored",
                                    requestId, clientInstanceId,
                                    transformLayerWrapper.response
                                            .getTransformsProcessed(),
                                    transformLayerWrapper.ignored));
                        }
                        if (throwPersistenceExceptions
                                && !transformLayerWrapper.response
                                        .getTransformExceptions().isEmpty()) {
                            throw (transformLayerWrapper.response
                                    .getTransformExceptions().get(0));
                        }
                    } else {
                        toCommit.add(rq);
                    }
                } finally {
                    if (useWrapperUser) {
                        PermissionsManager.get().popUser();
                    }
                }
                committed++;
            }
            return committed;
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebException(e);
        } finally {
            LooseContext.getContext().pop();
        }
    }

    @Override
    public void ping() {
    }

    @Override
    public JobTracker pollJobStatus(String id, boolean cancel) {
        if (cancel) {
            JobRegistry.get().cancel(id);
        }
        JobTracker tracker = JobRegistry.get().getTracker(id);
        if (tracker == null) {
            return null;
        }
        return JobRegistry.exportableForm(tracker);
    }

    @Override
    public String processCall(String payload) throws SerializationException {
        RPCRequest rpcRequest = null;
        String threadName = Thread.currentThread().getName();
        try {
            LooseContext.push();
            initUserStateWithCookie(getThreadLocalRequest(),
                    getThreadLocalResponse());
            LooseContext.set(CONTEXT_THREAD_LOCAL_HTTP_REQUEST,
                    getThreadLocalRequest());
            LooseContext.set(CommonPersistenceBase.CONTEXT_CLIENT_IP_ADDRESS,
                    ServletLayerUtils
                            .robustGetRemoteAddr(getThreadLocalRequest()));
            LooseContext.set(CommonPersistenceBase.CONTEXT_CLIENT_INSTANCE_ID,
                    SessionHelper.getAuthenticatedSessionClientInstanceId(
                            getThreadLocalRequest()));
            rpcRequest = RPC.decodeRequest(payload, this.getClass(), this);
            if (rpcRequest
                    .getSerializationPolicy() instanceof LegacySerializationPolicy) {
                throw new IncompatibleRemoteServiceException();
            }
            getThreadLocalRequest().setAttribute(THRD_LOCAL_RPC_RQ, rpcRequest);
            getThreadLocalRequest().setAttribute(THRD_LOCAL_RPC_PAYLOAD,
                    payload);
            String name = rpcRequest.getMethod().getName();
            RPCRequest f_rpcRequest = rpcRequest;
            Thread.currentThread().setName(Ax.format("gwt-rpc:%s", name));
            onAfterAlcinaAuthentication(name);
            LooseContext.set(CONTEXT_RPC_USER_ID,
                    PermissionsManager.get().getUserId());
            InternalMetrics.get().startTracker(rpcRequest,
                    () -> describeRpcRequest(f_rpcRequest, ""),
                    InternalMetricTypeAlcina.client,
                    Thread.currentThread().getName());
            Method method;
            try {
                method = this.getClass().getMethod(name,
                        rpcRequest.getMethod().getParameterTypes());
                if (method.isAnnotationPresent(WebMethod.class)) {
                    WebMethod webMethod = method.getAnnotation(WebMethod.class);
                    AnnotatedPermissible ap = new AnnotatedPermissible(
                            webMethod.customPermission());
                    if (!PermissionsManager.get().isPermissible(ap)) {
                        WebException wex = new WebException(
                                "Action not permitted: " + name);
                        logRpcException(wex,
                                LogMessageType.PERMISSIONS_EXCEPTION
                                        .toString());
                        return RPC.encodeResponseForFailure(null, wex);
                    }
                    if (!webMethod.readonlyPermitted()) {
                        AppPersistenceBase.checkNotReadOnly();
                    }
                }
            } catch (SecurityException ex) {
                RPC.encodeResponseForFailure(null, ex);
            } catch (NoSuchMethodException ex) {
                RPC.encodeResponseForFailure(null, ex);
            }
            return invokeAndEncodeResponse(rpcRequest);
        } catch (IncompatibleRemoteServiceException ex) {
            getServletContext().log(
                    "An IncompatibleRemoteServiceException was thrown while processing this call.",
                    ex);
            return RPC.encodeResponseForFailure(null, ex);
        } catch (UnexpectedException ex) {
            logRpcException(ex);
            throw ex;
        } catch (OutOfMemoryError e) {
            handleOom(payload, e);
            throw e;
        } catch (RuntimeException rex) {
            logRpcException(rex);
            throw rex;
        } finally {
            Thread.currentThread().setName(threadName);
            InternalMetrics.get().endTracker(rpcRequest);
            if (TransformManager.hasInstance()) {
                if (CommonUtils.bv((Boolean) getThreadLocalRequest()
                        .getAttribute(PUSH_TRANSFORMS_AT_END_OF_REUQEST))) {
                    Sx.commit();
                }
                ThreadlocalTransformManager.cast().resetTltm(null);
                LooseContext.pop();
            } else {
                try {
                    LooseContext.pop();
                } catch (Exception e) {// squelch, probably webapp undeployed
                }
            }
        }
    }

    @Override
    public SearchResultsBase search(SearchDefinition def, int pageNumber) {
        return Registry.impl(CommonPersistenceProvider.class)
                .getCommonPersistence().search(def, pageNumber);
    }

    @Override
    public Response suggest(BoundSuggestOracleRequest request) {
        try {
            Class<? extends BoundSuggestOracleResponseType> clazz = (Class<? extends BoundSuggestOracleResponseType>) Class
                    .forName(request.targetClassName);
            return Registry.impl(BoundSuggestOracleRequestHandler.class, clazz)
                    .handleRequest(clazz, request, request.hint);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public DomainTransformResponse transform(DomainTransformRequest request)
            throws DomainTransformRequestException {
        return ServletLayerTransforms.get().transform(request, false, false,
                true).response;
    }

    @Override
    public List<ServerValidator> validateOnServer(
            List<ServerValidator> validators) throws WebException {
        List<ServerValidator> entityLayer = new ArrayList<ServerValidator>();
        List<ServerValidator> results = new ArrayList<ServerValidator>();
        for (ServerValidator validator : validators) {
            Class clazz = Registry.get().lookupSingle(ServerValidator.class,
                    validator.getClass());
            ServerValidatorHandler handler = null;
            if (ServerValidatorHandler.class.isAssignableFrom(clazz)) {
                handler = (ServerValidatorHandler) Registry.get()
                        .instantiateSingle(ServerValidator.class,
                                validator.getClass());
            }
            if (handler instanceof ServletLayerValidatorHandler) {
                handler.handle(validator, null);
                results.add(validator);
            } else {
                results.addAll(Registry.impl(CommonPersistenceProvider.class)
                        .getCommonPersistence()
                        .validate(Collections.singletonList(validator)));
            }
        }
        return results;
    }

    @Override
    @WebMethod(readonlyPermitted = true, customPermission = @Permission(access = AccessLevel.EVERYONE))
    public DomainUpdate waitForTransforms(
            DomainTransformCommitPosition position)
            throws PermissionsException {
        if (!waitForTransformsEnabled()) {
            throw new PermissionsException();
        }
        Long clientInstanceId = Registry.impl(SessionHelper.class)
                .getAuthenticatedClientInstanceId(getThreadLocalRequest());
        if (clientInstanceId == null) {
            throw new PermissionsException();
        }
        return new TransformCollector().waitForTransforms(position,
                clientInstanceId);
    }

    private File getDataDumpsFolder() {
        File dataFolder = ServletLayerObjects.get().getDataFolder();
        File dir = new File(
                dataFolder.getPath() + File.separator + "client-dumps");
        dir.mkdirs();
        return dir;
    }

    private void sanitiseClrString(ClientLogRecord clr) {
        clr.setMessage(
                CommonUtils.nullToEmpty(clr.getMessage()).replace('\0', ' '));
    }

    protected void checkAnnotatedPermissions(Object o) {
        WebMethod ara = o.getClass().getAnnotation(WebMethod.class);
        if (ara != null) {
            if (!PermissionsManager.get().isPermissible(
                    new AnnotatedPermissible(ara.customPermission()))) {
                WrappedRuntimeException e = new WrappedRuntimeException(
                        "Permission denied for action " + o,
                        SuggestedAction.NOTIFY_WARNING);
                EntityLayerUtils.log(LogMessageType.TRANSFORM_EXCEPTION,
                        "Domain transform permissions exception", e);
                throw e;
            }
        }
    }

    @Override
    protected void doUnexpectedFailure(Throwable e) {
        if (e.getClass().getName()
                .equals("org.apache.catalina.connector.ClientAbortException")) {
            getLogger().debug("Client RPC call aborted by client");
            return;
        }
        if (e instanceof ReadOnlyException) {
            try {
                HttpServletResponse response = getThreadLocalResponse();
                response.reset();
                ServletContext servletContext = getServletContext();
                response.setContentType("text/plain");
                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getOutputStream()
                        .write(e.toString().getBytes("UTF-8"));
            } catch (Exception e2) {
                throw new WrappedRuntimeException(e2);
            }
        } else {
            e.printStackTrace();
            super.doUnexpectedFailure(e);
        }
    }

    protected Logger getLogger() {
        return logger;
    }

    protected String getRemoteAddress() {
        return getThreadLocalRequest() == null ? null
                : getThreadLocalRequest().getRemoteAddr();
    }

    protected HttpSession getSession() {
        return getSession(getThreadLocalRequest());
    }

    protected HttpSession getSession(HttpServletRequest request) {
        return getSession(request, getThreadLocalResponse());
    }

    protected HttpSession getSession(HttpServletRequest request,
            HttpServletResponse response) {
        return Registry.impl(SessionProvider.class).getSession(request,
                response);
    }

    protected RPCRequest getThreadRpcRequest() {
        return getThreadLocalRequest() == null ? null
                : (RPCRequest) getThreadLocalRequest()
                        .getAttribute(THRD_LOCAL_RPC_RQ);
    }

    protected String getUserAgent() {
        return getUserAgent(getThreadLocalRequest());
    }

    protected String getUserAgent(HttpServletRequest rq) {
        return rq == null ? UA_NULL_SERVER : rq.getHeader("User-Agent");
    }

    protected void handleOom(String payload, OutOfMemoryError e) {
        if (DUMP_STACK_TRACE_ON_OOM) {
            System.out.println("Payload:");
            System.out.println(payload);
            e.printStackTrace();
            SEUtilities.threadDump();
        }
    }

    protected String invokeAndEncodeResponse(RPCRequest rpcRequest)
            throws SerializationException {
        return RPC.invokeAndEncodeResponse(this, rpcRequest.getMethod(),
                rpcRequest.getParameters(),
                rpcRequest.getSerializationPolicy());
    }

    protected boolean isPersistOfflineTransforms() {
        return true;
    }

    protected int nextTransformRequestId() {
        return Registry.impl(ServletLayerTransforms.class)
                .nextTransformRequestId();
    }

    protected void onAfterAlcinaAuthentication(String methodName) {
    }

    @Override
    protected void onAfterResponseSerialized(String serializedResponse) {
        LooseContext.confirmDepth(looseContextDepth.get());
        PermissionsManager.get().setUser(null);
        super.onAfterResponseSerialized(serializedResponse);
    }

    protected void onAfterSpawnedThreadRun(Map properties) {
    }

    @Override
    protected void onBeforeRequestDeserialized(String serializedRequest) {
        super.onBeforeRequestDeserialized(serializedRequest);
        looseContextDepth.set(LooseContext.depth());
        getThreadLocalResponse().setHeader("Cache-Control", "no-cache");
    }

    protected void onBeforeSpawnedThreadRun(Map properties) {
    }

    protected boolean persistOfflineTransformsAsOneTransaction() {
        return true;
    }

    protected abstract void processValidLogin(LoginResponse lrb,
            String userName, HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse)
            throws AuthenticationException;

    protected void setLogger(Logger logger) {
        this.logger = logger;
    }

    protected boolean waitForTransformsEnabled() {
        return false;
    }

    String describeRemoteAction(RemoteAction remoteAction, String msg) {
        msg += "Clazz: " + remoteAction.getClass().getName() + "\n";
        msg += "User: " + PermissionsManager.get().getUserString() + "\n";
        msg += "\nParameters: \n";
        try {
            msg += new JacksonJsonObjectSerializer().withIdRefs()
                    .withMaxLength(1000000).serializeNoThrow(remoteAction);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return msg;
    }

    String describeRpcRequest(RPCRequest rpcRequest, String msg) {
        msg += "Method: " + rpcRequest.getMethod().getName() + "\n";
        msg += "User: " + PermissionsManager.get().getUserString() + "\n";
        msg += "Types: " + CommonUtils.joinWithNewlineTab(
                Arrays.asList(rpcRequest.getMethod().getParameters()));
        msg += "\nParameters: \n";
        Object[] parameters = rpcRequest.getParameters();
        if (rpcRequest.getMethod().getName().equals("transform")) {
        } else {
            try {
                msg += new JacksonJsonObjectSerializer().withIdRefs()
                        .withMaxLength(100000).serializeNoThrow(parameters);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return msg;
    }

    public class ActionLauncher<T> {
        private JobTracker actionTracker;

        TopicListener<JobTracker> startListener = new TopicListener<JobTracker>() {
            boolean processed = false;

            @Override
            public void topicPublished(String key, JobTracker message) {
                if (processed) {
                } else {
                    processed = true;
                    actionTracker = message;
                }
            }
        };

        protected ActionLogItem trackerToResult(final RemoteAction action) {
            ActionLogItem logItem = Registry
                    .impl(CommonPersistenceProvider.class)
                    .getCommonPersistenceExTransaction()
                    .getNewImplementationInstance(ActionLogItem.class);
            logItem.setActionClass(action.getClass());
            logItem.setActionDate(new Date());
            logItem.setShortDescription(CommonUtils
                    .trimToWsChars(actionTracker.getJobResult(), 220));
            if (!LooseContext.is(CONTEXT_NO_ACTION_LOG)) {
                logItem.setActionLog(actionTracker.getLog());
            }
            return logItem;
        }

        protected ActionResult<T> trackerToResult(final RemoteAction action,
                boolean nonPersistent) {
            ActionResult<T> result = new ActionResult<T>();
            if (actionTracker != null) {
                ActionLogItem logItem = trackerToResult(action);
                if (!actionTracker.provideIsRoot() || nonPersistent) {
                } else {
                    try {
                        Registry.impl(CommonPersistenceProvider.class)
                                .getCommonPersistence().logActionItem(logItem);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
                result.actionLogItem = logItem;
                result.resultObject = (T) actionTracker.getJobResultObject();
            }
            return result;
        }

        ActionResult<T> performActionAndWait(final RemoteAction action)
                throws WebException {
            checkAnnotatedPermissions(action);
            RemoteActionPerformer performer = (RemoteActionPerformer) Registry
                    .get().instantiateSingle(RemoteActionPerformer.class,
                            action.getClass());
            if (performer instanceof RequiresHttpSession) {
                RequiresHttpSession rhs = (RequiresHttpSession) performer;
                rhs.setHttpSession(getSession());
            }
            boolean nonPersistent = LooseContext
                    .is(JobRegistry.CONTEXT_NON_PERSISTENT);
            TransformManager transformManager = TransformManager.get();
            try {
                if (transformManager instanceof ThreadlocalTransformManager) {
                    ThreadlocalTransformManager.get().resetTltm(null);
                }
                LooseContext.push();
                if (!LooseContext.has(CONTEXT_THREAD_LOCAL_HTTP_REQUEST)) {
                    ActionPerformerMetricFilter filter = Registry
                            .impl(ActionPerformerMetricFilter.class);
                    InternalMetrics.get().startTracker(action,
                            () -> describeRemoteAction(action, ""),
                            InternalMetricTypeAlcina.service,
                            action.getClass().getSimpleName());
                }
                LooseContext.getContext().addTopicListener(
                        JobRegistry.TOPIC_JOB_STARTED, startListener);
                performer.performAction(action);
                return trackerToResult(action, nonPersistent);
            } catch (Throwable t) {
                Exception e = (Exception) ((t instanceof Exception) ? t
                        : new WrappedRuntimeException(t));
                if (actionTracker != null && !actionTracker.isComplete()) {
                    JobRegistry.get().jobError(e);
                    trackerToResult(action, nonPersistent);
                }
                boolean log = true;
                if (e instanceof WrappedRuntimeException) {
                    WrappedRuntimeException ire = (WrappedRuntimeException) e;
                    log = ire
                            .getSuggestedAction() != SuggestedAction.EXPECTED_EXCEPTION;
                }
                if (log) {
                    if (CommonUtils.extractCauseOfClass(e,
                            CancelledException.class) != null) {
                    } else {
                        logRpcException(e);
                    }
                }
                throw new WebException(e);
            } finally {
                if (!LooseContext.has(CONTEXT_THREAD_LOCAL_HTTP_REQUEST)) {
                    InternalMetrics.get().endTracker(action);
                }
                LooseContext.pop();
                if (transformManager instanceof ThreadlocalTransformManager) {
                    ThreadlocalTransformManager.get().resetTltm(null);
                }
            }
        }
    }

    @RegistryLocation(registryPoint = ActionPerformerMetricFilter.class, implementationType = ImplementationType.SINGLETON)
    public static class ActionPerformerMetricFilter
            implements Predicate<InternalMetricData> {
        @Override
        public boolean test(InternalMetricData imd) {
            return DomainStore.stores().writableStore().instrumentation()
                    .isLockedByThread(imd.thread);
        }
    }

    public static abstract class BoundSuggestOracleRequestHandler<T extends BoundSuggestOracleResponseType> {
        public Response handleRequest(Class<T> clazz,
                BoundSuggestOracleRequest request, String hint) {
            Response response = new Response();
            List<T> responses = getResponses(request.getQuery(), request.model,
                    hint);
            response.setSuggestions(responses.stream()
                    .map(BoundSuggestOracleSuggestion::new)
                    .limit(getSuggestionLimit()).collect(Collectors.toList()));
            if (offerNullSuggestion()) {
                ((List) response.getSuggestions()).add(0,
                        BoundSuggestOracleSuggestion.nullSuggestion());
            }
            return GraphProjections.defaultProjections().project(response);
        }

        protected abstract List<T> getResponses(String query,
                BoundSuggestOracleModel model, String hint);

        protected long getSuggestionLimit() {
            return 50;
        }

        protected boolean offerNullSuggestion() {
            return true;
        }
    }

    public static class ReuseIUserHolder {
        public IUser iUser;
    }

    class ActionLauncherAsync extends AlcinaChildRunnable {
        private CountDownLatch latch;

        private RemoteAction action;

        private TopicListener startListener;

        volatile JobTracker tracker;

        private Map properties = new LinkedHashMap();

        ActionLauncherAsync(String name, RemoteAction action) {
            super(name);
            this.latch = new CountDownLatch(2);
            this.action = action;
            this.startListener = new TopicListener<JobTracker>() {
                @Override
                public void topicPublished(String key, JobTracker tracker) {
                    ActionLauncherAsync.this.tracker = tracker;
                    latch.countDown();
                }
            };
        }

        public JobTracker launchAndWaitForTracker() {
            Thread thread = new Thread(this);
            onBeforeSpawnedThreadRun(properties);
            thread.start();
            latch.countDown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return tracker;
        }

        @Override
        protected void run0() throws Exception {
            onAfterSpawnedThreadRun(properties);
            LooseContext.getContext().addTopicListener(
                    JobRegistry.TOPIC_JOB_STARTED, startListener);
            performActionAndWait(this.action);
        }
    }

    static class IsWrappedObjectDteFilter
            implements CollectionFilter<DomainTransformEvent> {
        Class clazz = Registry.impl(CommonPersistenceProvider.class)
                .getCommonPersistenceExTransaction()
                .getImplementation(WrappedObject.class);

        @Override
        public boolean allow(DomainTransformEvent o) {
            return o.getObjectClass() == clazz;
        }
    }
}
