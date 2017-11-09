
## Obtaining an MQTT Client Library

Although, you can use any MQTT Client library of your choice to connect to Solace, this tutorial uses the Paho Java Client library. Here are a few easy ways to get the Paho API. The instructions in the Building section assume you're using Gradle and pulling the jars from maven central. If your environment differs then adjust the build instructions appropriately.

### Get the API: Using Gradle

```
    compile("org.eclipse.paho:org.eclipse.paho.client.mqttv3:{{ site.paho_version }}")
```

### Get the API: Using Maven

```
<dependency>
  <groupId>org.eclipse.paho</groupId>
  <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
  <version>{{ site.paho_version }}</version>
</dependency>
```
