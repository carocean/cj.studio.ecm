# ECM: The Foundation for Connection-Oriented Programming

ECM (Electronic Chip Model) is a revolutionary development toolkit designed to redefine modern distributed systems. By introducing "connection-oriented programming," ECM goes beyond traditional paradigms like object-oriented programming (OOP). Unlike frameworks such as Spring, OSGi, or Node.js, ECM is entirely independent, yet it encompasses and surpasses their core functionalities in a unified, scalable, and dynamic architecture. ECM provides a robust platform with service containers, modular design, and cross-language support, tailored for cloud-native and distributed environments.

---

## Key Features of ECM

### 1. Independent Framework
ECM operates independently of other frameworks while integrating the best of their functionalities:
- **Service Containers**: ECM includes its own service container that supports dependency injection, dynamic service configurations, and runtime adaptability.
- **Modular Architecture**: It features a "chip"-based modular design, enabling dynamic loading, unloading, and extension of components.
- **Lightweight Service Models**: ECM optimizes resource usage and performance for large-scale, high-concurrency systems.

---

### 2. Connection-Oriented Programming
ECM shifts focus from static structures to dynamic interactions between events ("things") and entities ("objects"). This philosophy emphasizes:
- **Flexible Decoupling**: Connections between components are dynamically adjustable.
- **Real-Time Adaptability**: Systems can evolve and adapt based on changing requirements without being restricted by static dependencies.

---

### 3. Comprehensive Distributed Ecosystem
ECM comes with built-in tools tailored for distributed systems:
- **Gateway**: Handles service routing, registration, discovery, and fault tolerance.
- **OpenPorts**: Simplifies external API exposure and remote service calls.
- **MIC (Microservice Management)**: Manages microservices across clusters, supporting multi-protocol environments like HTTP, TCP, UDP, and UDT.

---

### 4. JSS Services (Java-Based JavaScript Services)
ECM supports JavaScript services (JSS) that seamlessly integrate with Java environments:
- **Node.js-Like Flexibility**: JSS offers similar syntax and usability as Node.js.
- **Stability and Robustness**: Unlike Node.js, errors in JSS services do not cause process termination, ensuring smoother operation.
- **Cross-Language Integration**: JSS and Java services can work together, leveraging JavaScript's flexibility with Java's stability.

---

## How to Use ECM

### Service Containers
ECM supports defining services using annotations, XML, or JSON.

#### Example: Defining a Service with Annotations
```java
import cj.studio.ecm.annotation.CjService;

@CjService(name = "myService")
public class MyService {
    public String sayHello(String name) {
        return "Hello, " + name + "!";
    }
}
```
Accessing the Service
```java
IAssembly assembly = Assembly.load("/path/to/your/module.jar");
assembly.start();
MyService myService = (MyService) assembly.workbin().part("myService");
System.out.println(myService.sayHello("ECM"));
assembly.close();

```

Dynamic Modules

ECM’s modular design allows for runtime adjustments to system components.

Example: Loading a Module
```java
IAssembly assembly = Assembly.load("/path/to/module.jar");
assembly.start();
IServiceSite site = assembly.workbin();
Object myService = site.part("yourService");
System.out.println(myService.toString());
assembly.close();
```


JSS Services

Develop JavaScript-based services for seamless integration with Java.

Example: A JSS Service
```js
// JSS File: helloService.js
exports.sayHello = function(name) {
    return "Hello, " + name + "!";
};

```

Accessing JSS Services
```java
Object result = service.call("sayHello", "ECM");
System.out.println(result);

```

Distributed Communication

ECM supports efficient communication across distributed environments using various protocols.

Example: TCP Server and Client

Server:
```java
TcpNettyServer server = new TcpNettyServer();
server.start("localhost", 8080);
server.buildNetGraph().netoutput().plug("sink", (frame, circuit, plug) -> {
    System.out.println("Received: " + frame.content().toString());
});

```

Client:
```java
TcpNettyClient client = new TcpNettyClient();
client.connect("localhost", 8080, null);
Frame frame = new Frame("content=Hello ECM!");
client.buildNetGraph().netinput().flow(frame, new Circuit("circuit"));

```

Advantages of ECM

	1.	High Stability: ECM ensures smooth execution, isolating errors to prevent system crashes.
	2.	Dynamic Expansion: Modules are hot-pluggable, allowing systems to adjust without downtime.
	3.	Efficiency: ECM optimizes performance and resource usage, even in high-concurrency scenarios.
	4.	Unified Development: Simplifies distributed system design and deployment, reducing development complexity.

Conclusion

ECM redefines distributed system development with its independent, connection-oriented approach. It offers unparalleled flexibility, stability, and efficiency, making it a cornerstone for cloud-native architectures, IoT, and large-scale distributed environments. Whether building future-ready systems or simplifying existing architectures, ECM is the ideal choice for developers seeking innovation and reliability.

# Assembly

An assembly contains a chip, and the assembly uses logical chips to organize and isolate services or types.

## Features

- Modularization
- OSGi
- IOC containers

## Important Note

When developing your assembly project, **do not launch the main function** within this assembly.  
Otherwise, the type boundaries that belong to the assembly will be loaded into the application startup context, 
compromising the encapsulation of the assembly.

If a main function is required, encapsulation demands that the caller must be **external** to the assembly.