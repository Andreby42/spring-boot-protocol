package com.github.netty.nrpc.client;

import com.github.netty.nrpc.api.HelloData;
import com.github.netty.springboot.NettyRpcClient;
import org.reactivestreams.Publisher;
import org.springframework.web.bind.annotation.RequestMapping;

@NettyRpcClient(serviceName = "nrpc-server",timeout = 100)
@RequestMapping("/hello")
public interface HelloAsyncClient{
    Publisher<HelloData> sayHello(String name, int id);
}
