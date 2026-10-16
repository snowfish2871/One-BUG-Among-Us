package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundResourcePackPacket implements Packet<ServerCommonPacketListener> {
   private final ServerboundResourcePackPacket.Action action;

   public ServerboundResourcePackPacket(ServerboundResourcePackPacket.Action p_294469_) {
      this.action = p_294469_;
   }

   public ServerboundResourcePackPacket(FriendlyByteBuf p_295986_) {
      this.action = p_295986_.readEnum(ServerboundResourcePackPacket.Action.class);
   }

   @Override
   public void write(FriendlyByteBuf p_295360_) {
      p_295360_.writeEnum(this.action);
   }

   public void handle(ServerCommonPacketListener p_296386_) {
      p_296386_.handleResourcePackResponse(this);
   }

   public ServerboundResourcePackPacket.Action getAction() {
      return this.action;
   }

   public static enum Action {
      SUCCESSFULLY_LOADED,
      DECLINED,
      FAILED_DOWNLOAD,
      ACCEPTED;
   }
}
