package net.minecraft.advancements;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record AdvancementHolder(ResourceLocation id, Advancement value) {
   public void write(FriendlyByteBuf p_300966_) {
      p_300966_.writeResourceLocation(this.id);
      this.value.write(p_300966_);
   }

   public static AdvancementHolder read(FriendlyByteBuf p_301204_) {
      return new AdvancementHolder(p_301204_.readResourceLocation(), Advancement.read(p_301204_));
   }

   @Override
   public boolean equals(Object p_301145_) {
      if (this == p_301145_) {
         return true;
      } else {
         if (p_301145_ instanceof AdvancementHolder advancementholder && this.id.equals(advancementholder.id)) {
            return true;
         }

         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   @Override
   public String toString() {
      return this.id.toString();
   }
}
