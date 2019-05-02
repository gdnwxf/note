### thrift demo 了解

* 编写 HelloWorldService.thrift 文件 语法

  >thrift安装 <https://www.cnblogs.com/peterpanzsy/p/4210127.html>
  >
  >接口定义 <https://thrift.apache.org/docs/idl>
  >
  >类型系统<https://thrift.apache.org/docs/types>

如下所示的文件

```properties
namespace java com.hzins.thrift.demo

service HelloWorldService {
     string sayHello(1:string username)
}
```

通过如下命令编译 生成HelloWorldService.java的接口文件

```shell
 thrift -r --gen java HelloWorld.thrift
```

* HelloWorldService 的实现类

```java
package org.xtwy.thriftrpc;
import org.apache.thrift.TException;

import com.hzins.thrift.demo.HelloWorldService;
public class HelloServiceImpl implements HelloWorldService.Iface{

	@Override
	public String sayHello(String username) throws TException {
		System.out.println(username);
		return "ok";
	}

}
```



* thrift server端

```java
package org.xtwy.thriftrpc;

import com.hzins.thrift.demo.HelloWorldService;
import com.hzins.thrift.demo.HelloWorldService.Processor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;


public class ThriftServer    {



	public static void startServer(int port) throws Exception{
		TProcessor processor = new Processor<HelloWorldService.Iface>(new HelloServiceImpl());
		TServerSocket transport = new TServerSocket(port);
		TServer.Args args = new TServer.Args(transport);
		args.processor(processor);
		args.protocolFactory(new TBinaryProtocol.Factory());
		TServer server =  new TSimpleServer(args);
		server.serve();
	}

	public static void main(String[] args) throws Exception {
		startServer(8081);
	}

 
}
```

* thrift client端

```java
package org.xtwy.thriftrpc;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import com.hzins.thrift.demo.HelloWorldService;

public class ThriftClient {

	public static void startClient(int port) throws Exception{
		TTransport transport = new TSocket("localhost", port);
		TProtocol protocol = new TBinaryProtocol(transport);
		HelloWorldService.Client client = new HelloWorldService.Client(protocol);
		transport.open();
		String result = client.sayHello("zhangsan");
		System.out.println(result);

	}

	public static void main(String[] args) throws Exception {
		startClient(8081);
	}
}

```

* 启动调用结果

```java
Connected to the target VM, address: '127.0.0.1:62373', transport: 'socket'
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
ok
Disconnected from the target VM, address: '127.0.0.1:62373', transport: 'socket'

Process finished with exit code 0

```

