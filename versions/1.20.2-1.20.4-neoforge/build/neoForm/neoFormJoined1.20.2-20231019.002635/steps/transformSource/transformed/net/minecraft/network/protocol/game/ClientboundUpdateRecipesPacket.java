package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public class ClientboundUpdateRecipesPacket implements Packet<ClientGamePacketListener> {
   private final List<RecipeHolder<?>> recipes;

   public ClientboundUpdateRecipesPacket(Collection<RecipeHolder<?>> p_133632_) {
      this.recipes = Lists.newArrayList(p_133632_);
   }

   public ClientboundUpdateRecipesPacket(FriendlyByteBuf p_179468_) {
      this.recipes = p_179468_.readList(ClientboundUpdateRecipesPacket::fromNetwork);
   }

   @Override
   public void write(FriendlyByteBuf p_133646_) {
      p_133646_.writeCollection(this.recipes, ClientboundUpdateRecipesPacket::toNetwork);
   }

   public void handle(ClientGamePacketListener p_133641_) {
      p_133641_.handleUpdateRecipes(this);
   }

   public List<RecipeHolder<?>> getRecipes() {
      return this.recipes;
   }

   private static RecipeHolder<?> fromNetwork(FriendlyByteBuf p_133648_) {
      ResourceLocation resourcelocation = p_133648_.readResourceLocation();
      ResourceLocation resourcelocation1 = p_133648_.readResourceLocation();
      Recipe<?> recipe = BuiltInRegistries.RECIPE_SERIALIZER
         .getOptional(resourcelocation)
         .orElseThrow(() -> new IllegalArgumentException("Unknown recipe serializer " + resourcelocation))
         .fromNetwork(p_133648_);
      return new RecipeHolder<>(resourcelocation1, recipe);
   }

   public static <T extends Recipe<?>> void toNetwork(FriendlyByteBuf p_179470_, RecipeHolder<?> p_301050_) {
      p_179470_.writeResourceLocation(BuiltInRegistries.RECIPE_SERIALIZER.getKey(p_301050_.value().getSerializer()));
      p_179470_.writeResourceLocation(p_301050_.id());
      ((net.minecraft.world.item.crafting.RecipeSerializer<T>)p_301050_.value().getSerializer()).toNetwork(p_179470_, (T)p_301050_.value());
   }
}
