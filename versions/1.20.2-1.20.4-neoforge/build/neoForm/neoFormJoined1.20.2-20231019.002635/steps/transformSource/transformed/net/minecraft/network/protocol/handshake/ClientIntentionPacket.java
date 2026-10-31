package net.minecraft.network.protocol.handshake;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientIntentionPacket(int protocolVersion, String hostName, int port, ClientIntent intention, String fmlVersion) implements Packet<ServerHandshakePacketListener> {
   private static final int MAX_HOST_LENGTH = 255;

   public ClientIntentionPacket {
      if (fmlVersion == null) {
         fmlVersion = net.neoforged.neoforge.network.NetworkHooks.getFMLVersion(hostName);
         hostName = hostName.split("\0")[0];
      }
   }

   @Deprecated
   public ClientIntentionPacket(int protocolVersion, String hostName, int port, ClientIntent intention) {
      this(protocolVersion, hostName, port, intention, net.neoforged.neoforge.network.NetworkConstants.NETVERSION);
   }

   public ClientIntentionPacket(FriendlyByteBuf p_179801_) {
      this(p_179801_.readVarInt(), p_179801_.readUtf(255), p_179801_.readUnsignedShort(), ClientIntent.byId(p_179801_.readVarInt()), null);
   }

   @Override
   public void write(FriendlyByteBuf p_134737_) {
      p_134737_.writeVarInt(this.protocolVersion);
      p_134737_.writeUtf(this.hostName + "\0" + this.fmlVersion);
      p_134737_.writeShort(this.port);
      p_134737_.writeVarInt(this.intention.id());
   }

   public void handle(ServerHandshakePacketListener p_134734_) {
      p_134734_.handleIntention(this);
   }

   @Override
   public ConnectionProtocol nextProtocol() {
      return this.intention.protocol();
   }

   public String getFMLVersion() {
      return this.fmlVersion;
   }
}
