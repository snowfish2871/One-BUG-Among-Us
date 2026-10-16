package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record NbtPredicate(CompoundTag tag) {
   public static final Codec<NbtPredicate> CODEC = TagParser.AS_CODEC.xmap(NbtPredicate::new, NbtPredicate::tag);

   public boolean matches(ItemStack p_57480_) {
      return this.matches(p_57480_.getTag());
   }

   public boolean matches(Entity p_57478_) {
      return this.matches(getEntityTagToCompare(p_57478_));
   }

   public boolean matches(@Nullable Tag p_57484_) {
      return p_57484_ != null && NbtUtils.compareNbt(this.tag, p_57484_, true);
   }

   public static CompoundTag getEntityTagToCompare(Entity p_57486_) {
      CompoundTag compoundtag = p_57486_.saveWithoutId(new CompoundTag());
      if (p_57486_ instanceof Player) {
         ItemStack itemstack = ((Player)p_57486_).getInventory().getSelected();
         if (!itemstack.isEmpty()) {
            compoundtag.put("SelectedItem", itemstack.save(new CompoundTag()));
         }
      }

      return compoundtag;
   }
}
