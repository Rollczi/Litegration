# Litegration

```kts
plugins {
    id("dev.rollczi.litegration.paper") version "0.1.0"
}

tasks.testPaper {
    eula = true
    plugin = file("../build/libs/ChatFormatter v${project.version}.jar")
    serverVersion = "1.21.7"
}
```

```java
class MyE2ETest {

    @LitegrationTest
    void test() {
        Client client = McProtocolLibClient.connected("Rollczi");
        Player rollczi = Bukkit.getPlayer(client.getName());
        ChatFormatterApi chatFormatterApi = ChatFormatterApiProvider.get();
        String format = chatFormatterApi.getPlaceholderRegistry().format("test", rollczi);
        Bukkit.broadcastMessage(format);
    }

}
```