package dev.rollczi.litegration.client;

import dev.rollczi.litegration.Litegration;
import java.util.concurrent.CountDownLatch;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;

public class McProtocolLibClient implements Client {

    private final ClientSession client;
    private final String nick;

    private McProtocolLibClient(ClientSession client, String nick) {
        this.client = client;
        this.nick = nick;
    }

    public static McProtocolLibClient connected(String nick) {
        Litegration litegration = Litegration.getCurrent();
        return connected(nick, litegration.getAddress(), litegration.getPort());
    }

    public static McProtocolLibClient connected(String nick, String address, int port) {
        MinecraftProtocol protocol = new MinecraftProtocol(MinecraftCodec.CODEC, nick);
        ClientSession client = ClientNetworkSessionFactory.factory()
            .setAddress(address, port)
            .setProtocol(protocol)
            .create();
        McProtocolLibClient fakeClient = new McProtocolLibClient(client, nick);
        CountDownLatch connected = new CountDownLatch(1);
        client.addListener(new FakePlayerPacketHandler(connected));
        client.connect();
        try {
            connected.await();
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
        return fakeClient;
    }

    @Override
    public String getName() {
        return nick;
    }

    @Override
    public void quit() {
        client.disconnect(Component.text("Quitting"));
    }

    private static class FakePlayerPacketHandler extends SessionAdapter {

        private final CountDownLatch connected;

        private FakePlayerPacketHandler(CountDownLatch connected) {
            this.connected = connected;
        }

        @Override
        public void packetReceived(Session session, Packet packet) {
            if (packet instanceof ClientboundPlayerPositionPacket) {
                connected.countDown();
            }
        }

        @Override
        public void disconnected(DisconnectedEvent event) {
            if (event.getCause() != null) {
                event.getCause().printStackTrace();
            }
        }

    }
}