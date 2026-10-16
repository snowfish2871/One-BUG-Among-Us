package net.minecraft.network.protocol.common;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerboundCustomPayloadPacket(CustomPacketPayload payload) implements Packet<ServerCommonPacketListener> {
   private static final int MAX_PAYLOAD_SIZE = 32767;
   private static final Map<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>> KNOWN_TYPES = ImmutableMap.<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>>builder(
         
      )
      .put(BrandPayload.ID, BrandPayload::new)
      .build();

   public ServerboundCustomPayloadPacket(FriendlyByteBuf p_296108_) {
      this(readPayload(p_296108_.readResourceLocation(), p_296108_));
   }

   private static CustomPacketPayload readPayload(ResourceLocation p_294367_, FriendlyByteBuf p_294321_) {
      FriendlyByteBuf.Reader<? extends CustomPacketPayload> reader = KNOWN_TYPES.get(p_294367_);
      return reader != null ? reader.apply(p_294321_) : readUnknownPayload(p_294367_, p_294321_);
   }

   private static net.neoforged.neoforge.network.custom.payload.SimplePayload readUnknownPayload(ResourceLocation p_294973_, FriendlyByteBuf p_296037_) {
      int i = p_296037_.readableBytes();
      if (i >= 0 && i <= MAX_PAYLOAD_SIZE) {
         return net.neoforged.neoforge.network.custom.payload.SimplePayload.inbound(p_296037_, p_294973_);
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 32767 bytes");
      }
   }

   @Override
   public void write(FriendlyByteBuf p_295621_) {
      p_295621_.writeResourceLocation(this.payload.id());
      this.payload.write(p_295621_);
   }

   public void handle(ServerCommonPacketListener p_295862_) {
      p_295862_.handleCustomPayload(this);
   }
}
