package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;

public class UntouchingEnchantment extends Enchantment {
   protected UntouchingEnchantment(Enchantment.Rarity p_45260_, EquipmentSlot... p_45261_) {
      super(p_45260_, EnchantmentCategory.DIGGER, p_45261_);
   }

   @Override
   public int getMinCost(int p_45264_) {
      return 15;
   }

   @Override
   public int getMaxCost(int p_45268_) {
      return super.getMinCost(p_45268_) + 50;
   }

   @Override
   public boolean checkCompatibility(Enchantment p_45266_) {
      return super.checkCompatibility(p_45266_) && p_45266_ != Enchantments.BLOCK_FORTUNE;
   }
}
