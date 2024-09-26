package com.sctech.opcua.service;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class SubscribeThread  extends TimerTask {

    private static final AtomicInteger atomic = new AtomicInteger();

    private OpcUaClient client;
    private List<NodeId> nodeIdList;

    public SubscribeThread(OpcUaClient client, List<NodeId> list){
        this.client = client;
        this.nodeIdList = list;
    }

    @Override
    public void run() {
        try {
            //创建发布间隔1000ms的订阅对象
            client
                    .getSubscriptionManager()
                    .createSubscription(1000.0)
                    .thenAccept(t -> {
                        List<MonitoredItemCreateRequest> requests = new ArrayList<>();
                        if(nodeIdList!=null && nodeIdList.size()>0){
                            for(NodeId nodeId : nodeIdList){
                                ReadValueId readValueId = new ReadValueId(nodeId, AttributeId.Value.uid(), null, null);
                                //创建监控的参数
                                MonitoringParameters parameters = new MonitoringParameters(UInteger.valueOf(atomic.getAndIncrement()), 1000.0, null, UInteger.valueOf(10), true);
                                //创建监控项请求
                                //该请求最后用于创建订阅。
                                MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);
                                requests.add(request);
                            }
                        }
                        if(requests.size()>0){
                            //创建监控项，并且注册变量值改变时候的回调函数。
                            t.createMonitoredItems(
                                    TimestampsToReturn.Both,
                                    requests,
                                    (item, id) -> item.setValueConsumer((it, val) -> {
                                        System.out.println("nodeid :" + it.getReadValueId().getNodeId());
                                        System.out.println("value :" + val.getValue().getValue());
                                    })
                            );
                        }

                    }).get();
            Thread.sleep(5000L);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
