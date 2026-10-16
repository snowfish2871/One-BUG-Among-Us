package net.minecraft.network.protocol.login;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryAnswerPayload;

public record ServerboundCustomQueryAnswerPacket(int transactionId, @Nullable CustomQueryAnswerPayload payload) implements Packet<ServerLoginPacketListener> {
   private static final int MAX_PAYLOAD_SIZE = 1048576;

   public static ServerboundCustomQueryAnswerPacket read(FriendlyByteBuf p_295711_) {
      int i = p_295711_.readVarInt();
      return new ServerboundCustomQueryAnswerPacket(i, readPayload(i, p_295711_));
   }

   private static net.neoforged.neoforge.network.custom.payload.SimpleQueryPayload readPayload(int p_296215_, FriendlyByteBuf p_295168_) {
      return readUnknownPayload(p_295168_);
   }

   private static net.neoforged.neoforge.network.custom.payload.SimpleQueryPayload readUnknownPayload(FriendlyByteBuf p_294928_) {
      int i = p_294928_.readableBytes();
      if (i >= 0 && i <= MAX_PAYLOAD_SIZE) {
         return p_294928_.readNullable(net.neoforged.neoforge.network.custom.payload.SimpleQueryPayload::inbound);
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
      }
   }

   @Override
   public void write(FriendlyByteBuf p_296127_) {
      p_296127_.writeVarInt(this.transactionId);
      p_296127_.writeNullable(this.payload, (p_295443_, p_295588_) -> p_295588_.write(p_295443_));
   }

   public void handle(ServerLoginPacketListener p_294750_) {
      p_294750_.handleCustomQueryPacket(this);
   }
}
