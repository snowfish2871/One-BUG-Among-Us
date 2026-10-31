package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ClientInformation;

public record ServerboundClientInformationPacket(ClientInformation information) implements Packet<ServerCommonPacketListener> {
   public ServerboundClientInformationPacket(FriendlyByteBuf p_302025_) {
      this(new ClientInformation(p_302025_));
   }

   @Override
   public void write(FriendlyByteBuf p_301974_) {
      this.information.write(p_301974_);
   }

   public void handle(ServerCommonPacketListener p_302031_) {
      p_302031_.handleClientInformation(this);
   }
}
