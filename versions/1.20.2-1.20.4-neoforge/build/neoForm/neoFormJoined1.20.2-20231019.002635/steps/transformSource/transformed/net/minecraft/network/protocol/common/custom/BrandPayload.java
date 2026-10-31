package net.minecraft.network.protocol.common.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record BrandPayload(String brand) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("brand");

   public BrandPayload(FriendlyByteBuf p_296001_) {
      this(p_296001_.readUtf());
   }

   @Override
   public void write(FriendlyByteBuf p_294892_) {
      p_294892_.writeUtf(this.brand);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }
}
