package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

public class ClientboundPlaceGhostRecipePacket implements Packet<ClientGamePacketListener> {
   private final int containerId;
   private final ResourceLocation recipe;

   public ClientboundPlaceGhostRecipePacket(int p_132647_, RecipeHolder<?> p_301206_) {
      this.containerId = p_132647_;
      this.recipe = p_301206_.id();
   }

   public ClientboundPlaceGhostRecipePacket(FriendlyByteBuf p_179027_) {
      this.containerId = p_179027_.readByte();
      this.recipe = p_179027_.readResourceLocation();
   }

   @Override
   public void write(FriendlyByteBuf p_132657_) {
      p_132657_.writeByte(this.containerId);
      p_132657_.writeResourceLocation(this.recipe);
   }

   public void handle(ClientGamePacketListener p_132654_) {
      p_132654_.handlePlaceRecipe(this);
   }

   public ResourceLocation getRecipe() {
      return this.recipe;
   }

   public int getContainerId() {
      return this.containerId;
   }
}
