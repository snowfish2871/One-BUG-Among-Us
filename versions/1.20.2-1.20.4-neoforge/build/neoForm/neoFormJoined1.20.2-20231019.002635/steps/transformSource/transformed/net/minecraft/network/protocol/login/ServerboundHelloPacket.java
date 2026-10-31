package net.minecraft.network.protocol.login;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundHelloPacket(String name, UUID profileId) implements Packet<ServerLoginPacketListener> {
   public ServerboundHelloPacket(FriendlyByteBuf p_179827_) {
      this(p_179827_.readUtf(16), p_179827_.readUUID());
   }

   @Override
   public void write(FriendlyByteBuf p_134851_) {
      p_134851_.writeUtf(this.name, 16);
      p_134851_.writeUUID(this.profileId);
   }

   public void handle(ServerLoginPacketListener p_134848_) {
      p_134848_.handleHello(this);
   }
}
