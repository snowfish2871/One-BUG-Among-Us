package net.minecraft.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundCustomQueryPacket(int transactionId, CustomQueryPayload payload) implements Packet<ClientLoginPacketListener> {
   private static final int MAX_PAYLOAD_SIZE = 1048576;

   public ClientboundCustomQueryPacket(FriendlyByteBuf p_179810_) {
      this(p_179810_.readVarInt(), readPayload(p_179810_.readResourceLocation(), p_179810_));
   }

   private static CustomQueryPayload readPayload(ResourceLocation p_295267_, FriendlyByteBuf p_295117_) {
      return readUnknownPayload(p_295267_, p_295117_);
   }

   private static net.neoforged.neoforge.network.custom.payload.SimpleQueryPayload readUnknownPayload(ResourceLocation p_294837_, FriendlyByteBuf p_296380_) {
      int i = p_296380_.readableBytes();
      if (i >= 0 && i <= MAX_PAYLOAD_SIZE) {
         return net.neoforged.neoforge.network.custom.payload.SimpleQueryPayload.inbound(p_296380_, p_294837_);
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
      }
   }

   @Override
   public void write(FriendlyByteBuf p_134757_) {
      p_134757_.writeVarInt(this.transactionId);
      p_134757_.writeResourceLocation(this.payload.id());
      this.payload.write(p_134757_);
   }

   public void handle(ClientLoginPacketListener p_134754_) {
      p_134754_.handleCustomQuery(this);
   }
}
