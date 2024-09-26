package com.sctech.opcua.web;

import com.sctech.opcua.service.OpcUaClientService;
import com.sctech.opcua.service.SubscribeThread;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.client.UaStackClient;
import org.eclipse.milo.opcua.stack.client.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;

public class OpcKepController {


     SecurityPolicy getSecurityPolicy() {
        return SecurityPolicy.Basic128Rsa15;
    }

    public OpcUaClient getClient() throws  Exception{

        Path securityTempDir = Paths.get("D:/temp/opcua", "client", "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("路径不存在: " + securityTempDir);
        }

        File pkiDir = securityTempDir.resolve("pki").toFile();

        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);
        DefaultTrustListManager trustListManager = new DefaultTrustListManager(pkiDir);
        DefaultClientCertificateValidator certificateValidator =
                new DefaultClientCertificateValidator(trustListManager);

        List<EndpointDescription> endpoints =
                DiscoveryClient.getEndpoints("opc.tcp://localhost:49320").get();
        EndpointDescription endpoint = null;
        for(EndpointDescription ed: endpoints){
            if(ed.getSecurityPolicyUri().equals(getSecurityPolicy().getUri())){
                endpoint = ed;
                break;
            }
        }
        if(endpoint==null) throw new UaException(StatusCodes.Bad_ConfigurationError, "找不到服务端");
        OpcUaClientConfigBuilder builder = OpcUaClientConfig.builder()
                .setEndpoint(endpoint);

        OpcUaClientConfig config =  builder
                .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
                .setApplicationUri("urn:eclipse:milo:examples:client")
                .setKeyPair(loader.getClientKeyPair())
                .setCertificate(loader.getClientCertificate())
                .setCertificateChain(loader.getClientCertificateChain())
                .setCertificateValidator(certificateValidator)
                // todo
                .setIdentityProvider(new UsernameProvider("zhangsan","123456"))
                .setRequestTimeout(uint(5000))
                .build();
        UaStackClient stackClient = UaStackClient.create(config);
        return new OpcUaClient(config, stackClient);

    }


    /**
     * 遍历所有的
     */
    public void listNodes() throws Exception{
        OpcUaClient client = getClient();
        client.connect().get();
        OpcUaClientService opcUaClientService = new OpcUaClientService();

        // 遍历所有节点

        opcUaClientService.listNode(client, null);


    }

    public void writeNodeValue() throws Exception{
        OpcUaClient client = getClient();
        client.connect().get();
        OpcUaClientService opcUaClientService = new OpcUaClientService();
        // 遍历所有节点
        NodeId nodeId = NodeId.parse("ns=2;s=hss.hss.v1");
        //创建数据对象,此处的数据对象一定要定义类型，不然会出现类型错误，导致无法写入
        DataValue newValue = new DataValue(new Variant(2034.11), null, null);
        opcUaClientService.writeNodeValue(client, nodeId, newValue);
    }

    /**
     * 遍历所有的
     */
    public void readNodeValue() throws Exception{
        OpcUaClient client = getClient();
        client.connect().get();
        OpcUaClientService opcUaClientService = new OpcUaClientService();
        // 遍历所有节点
        NodeId nodeId = NodeId.parse("ns=2;s=hss.hss.v1");
        opcUaClientService.readNodeValue(client, nodeId);
    }


    public void subscribe2() throws Exception{
        // 与OPC UA服务端建立连接，并返回客户端实例
        OpcUaClient client = getClient();
        client.connect().get();
        OpcUaClientService opcUaClientService = new OpcUaClientService();
        List<NodeId> nodeIdList = new ArrayList<>();
        NodeId nodeId1 = NodeId.parse("ns=2;s=hss.hss.v1");
        NodeId nodeId2 = NodeId.parse("ns=2;s=hss.hss.v2");
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
                new Variant[]{new Variant(9.0)}
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
        // 与OPC UA服务端建立连接，并返回客户端实例
        OpcUaClient client = getClient();
        client.connect().get();
        OpcUaClientService opcUaClientService = new OpcUaClientService();

        HistoryReadDetails historyReadDetails = new ReadRawModifiedDetails(
                false,
                DateTime.MIN_VALUE,
                DateTime.now(),
                uint(0),
                true
        );
        // 查询的节点id
        NodeId nodeId = NodeId.parse("ns=2;s=hss.hss.v1");
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

            //new OpcKepController().listNodes();
            new OpcKepController().readNodeValue();
            //new OpcKepController().writeNodeValue();
            //new OpcKepController().subscribe2();
            /*
            Double aDouble = new OpcKepController().callMethod().get();
            System.out.println(aDouble);
            */
//            new OpcKepController().historyData();

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
