package com.sctech.opcua.web;

import com.sctech.opcua.service.OpcUaClientService;
import com.sctech.opcua.service.SubscribeThread;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;

@RestController
@RequestMapping("/opcua")
public class OpcuaController {

    @RequestMapping("/test")
    public void run() throws Exception{
        OpcUaClientService opcUaClientService = new OpcUaClientService();

        // 与OPC UA服务端建立连接，并返回客户端实例
        OpcUaClient client = opcUaClientService.connectOpcUaServer("milo.digitalpetri.com", "62541", "/milo");

        // 遍历所有节点
//        opcUaClientService.listNode(client, null);

        // 读取指定节点的值
        opcUaClientService.readNodeValue(client, 2, "Dynamic/RandomInt32");
        opcUaClientService.readNodeValue(client, 2, "Dynamic/RandomInt64");

        // 向指定节点写入数据
//        opcUaClientService.writeNodeValue(client, 2, "Demo.1500PLC.D1", 6f);

        // 订阅指定节点
//        opcUaClientService.subscribe(client, 2, "Dynamic/RandomDouble");

        // 批量订阅多个节点
        List<String> identifiers = new ArrayList<>();
        identifiers.add("Dynamic/RandomDouble");
        identifiers.add("Dynamic/RandomFloat");

        opcUaClientService.setBatchNamespaceIndex(2);
        opcUaClientService.setBatchIdentifiers(identifiers);

//        opcUaClientService.subscribeBatch(client);
        opcUaClientService.subscribeBatchWithReconnect(client);
    }

    /**
     * 遍历所有的
     */
    public void listNodes() throws Exception{
        OpcUaClientService opcUaClientService = new OpcUaClientService();
        // 与OPC UA服务端建立连接，并返回客户端实例
        OpcUaClient client = opcUaClientService.connectOpcUaServer("milo.digitalpetri.com", "62541", "/milo");

        // 遍历所有节点
        opcUaClientService.listNode(client, null);
    }

    public void writeNodeValue() throws Exception{
        OpcUaClientService opcUaClientService = new OpcUaClientService();
        // 与OPC UA服务端建立连接，并返回客户端实例
        OpcUaClient client = opcUaClientService.connectOpcUaServer("milo.digitalpetri.com", "62541", "/milo");
        // 遍历所有节点
        NodeId nodeId = NodeId.parse("ns=2;s=CTT/References/Has3ForwardRefs_1/001");
        DataValue newValue = new DataValue(new Variant(234), null, null);
        opcUaClientService.writeNodeValue(client, nodeId, newValue);
    }

    /**
     * 遍历所有的
     */
    public void readNodeValue() throws Exception{
        OpcUaClientService opcUaClientService = new OpcUaClientService();
        // 与OPC UA服务端建立连接，并返回客户端实例
        OpcUaClient client = opcUaClientService.connectOpcUaServer("milo.digitalpetri.com", "62541", "/milo");

        // 遍历所有节点
        NodeId nodeId = NodeId.parse("ns=2;s=CTT/References/Has3ForwardRefs_1/001");
        opcUaClientService.readNodeValue(client, nodeId);
    }


    public void subscribe2() throws Exception{
        OpcUaClientService opcUaClientService = new OpcUaClientService();
        // 与OPC UA服务端建立连接，并返回客户端实例
        OpcUaClient client = opcUaClientService.connectOpcUaServer("milo.digitalpetri.com", "62541", "/milo");
        List<NodeId> nodeIdList = new ArrayList<>();
        NodeId nodeId1 = NodeId.parse("ns=2;s=CTT/References/Has3ForwardRefs_1/001");
        NodeId nodeId2 = NodeId.parse("ns=2;s=CTT/References/Has3ForwardRefs_1/002");
        nodeIdList.add(nodeId1);
        nodeIdList.add(nodeId2);

        Timer timer = new Timer();
        TimerTask thread = new SubscribeThread(client,nodeIdList);
        timer.schedule(thread,1000L);

    }

    /**
     * 方法调用
     * @return
     * @throws Exception
     */
    public CompletableFuture<Double> callMethod()  throws Exception{
        OpcUaClientService opcUaClientService = new OpcUaClientService();
        // 与OPC UA服务端建立连接，并返回客户端实例
        OpcUaClient client = opcUaClientService.connectOpcUaServer("milo.digitalpetri.com", "62541", "/milo");
        NodeId objectId = NodeId.parse("ns=2;s=Methods");
        NodeId methodId = NodeId.parse("ns=2;s=Methods/sqrt(x)");
        CallMethodRequest request = new CallMethodRequest(
                objectId,
                methodId,
                new Variant[]{new Variant(81.0)}
        );
        return client.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();

            if (statusCode.isGood()) {
                List<Variant> results = Arrays.asList(result.getOutputArguments());
                Double value = (Double)results.get(0).getValue();
                return CompletableFuture.completedFuture(value);
            }else{
                CompletableFuture<Double> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                return f;
            }

        });
    }

    public  void historyData() throws Exception{
        OpcUaClientService opcUaClientService = new OpcUaClientService();
        // 与OPC UA服务端建立连接，并返回客户端实例
        OpcUaClient client = opcUaClientService.connectOpcUaServer("milo.digitalpetri.com", "62541", "/milo");

        client.connect().get();

        HistoryReadDetails historyReadDetails = new ReadRawModifiedDetails(
                false,
                DateTime.MIN_VALUE,
                DateTime.now(),
                uint(0),
                true
        );
        // 查询的节点id
        NodeId nodeId = NodeId.parse("ns=2;s=CTT/References/Has3ForwardRefs_1/001");
        HistoryReadValueId historyReadValueId = new HistoryReadValueId(
                nodeId,
                null,
                QualifiedName.NULL_VALUE,
                ByteString.NULL_VALUE
        );

        List<HistoryReadValueId> nodesToRead = new ArrayList<>();
        nodesToRead.add(historyReadValueId);

        HistoryReadResponse historyReadResponse = client.historyRead(
                historyReadDetails,
                TimestampsToReturn.Both,
                false,
                nodesToRead
        ).get();


        HistoryReadResult[] historyReadResults = historyReadResponse.getResults();

        if (historyReadResults != null) {
            HistoryReadResult historyReadResult = historyReadResults[0];
            StatusCode statusCode = historyReadResult.getStatusCode();

            if (statusCode.isGood()) {
                HistoryData historyData = (HistoryData) historyReadResult.getHistoryData().decode(
                        client.getStaticSerializationContext()
                );

                List<DataValue> dataValues = l(historyData.getDataValues());

                dataValues.forEach(v -> System.out.println("value=" + v));
            } else {
                System.out.println("History read failed: " + statusCode);
            }
        }

        Thread.sleep(10000);

    }


    public static void main(String[] args){
        try {
            //new OpcuaController().listNodes();
            //new OpcuaController().readNodeValue();
            //new OpcuaController().writeNodeValue();
            //new OpcuaController().subscribe2();

            Double aDouble = new OpcuaController().callMethod().get();
            System.out.println(aDouble);

            //new OpcuaController().historyData();

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
