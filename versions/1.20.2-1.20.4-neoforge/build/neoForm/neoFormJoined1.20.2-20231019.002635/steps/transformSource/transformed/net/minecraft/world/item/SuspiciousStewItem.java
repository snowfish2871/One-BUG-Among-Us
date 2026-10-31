package net.minecraft.world.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SuspiciousEffectHolder;

public class SuspiciousStewItem extends Item {
   public static final String EFFECTS_TAG = "effects";
   public static final int DEFAULT_DURATION = 160;

   public SuspiciousStewItem(Item.Properties p_43257_) {
      super(p_43257_);
   }

   public static void saveMobEffects(ItemStack p_298592_, List<SuspiciousEffectHolder.EffectEntry> p_299245_) {
      CompoundTag compoundtag = p_298592_.getOrCreateTag();
      SuspiciousEffectHolder.EffectEntry.LIST_CODEC
         .encodeStart(NbtOps.INSTANCE, p_299245_)
         .result()
         .ifPresent(p_299094_ -> compoundtag.put("effects", p_299094_));
   }

   public static void appendMobEffects(ItemStack p_298704_, List<SuspiciousEffectHolder.EffectEntry> p_299318_) {
      CompoundTag compoundtag = p_298704_.getOrCreateTag();
      List<SuspiciousEffectHolder.EffectEntry> list = new ArrayList<>();
      listPotionEffects(p_298704_, list::add);
      list.addAll(p_299318_);
      SuspiciousEffectHolder.EffectEntry.LIST_CODEC.encodeStart(NbtOps.INSTANCE, list).result().ifPresent(p_298283_ -> compoundtag.put("effects", p_298283_));
   }

   private static void listPotionEffects(ItemStack p_260126_, Consumer<SuspiciousEffectHolder.EffectEntry> p_259500_) {
      CompoundTag compoundtag = p_260126_.getTag();
      if (compoundtag != null && compoundtag.contains("effects", 9)) {
         SuspiciousEffectHolder.EffectEntry.LIST_CODEC
            .parse(NbtOps.INSTANCE, compoundtag.getList("effects", 10))
            .result()
            .ifPresent(p_298886_ -> p_298886_.forEach(p_259500_));
      }
   }

   @Override
   public void appendHoverText(ItemStack p_260314_, @Nullable Level p_259224_, List<Component> p_259700_, TooltipFlag p_260021_) {
      super.appendHoverText(p_260314_, p_259224_, p_259700_, p_260021_);
      if (p_260021_.isCreative()) {
         List<MobEffectInstance> list = new ArrayList<>();
         listPotionEffects(p_260314_, p_298636_ -> list.add(p_298636_.createEffectInstance()));
         PotionUtils.addPotionTooltip(list, p_259700_, 1.0F);
      }
   }

   @Override
   public ItemStack finishUsingItem(ItemStack p_43263_, Level p_43264_, LivingEntity p_43265_) {
      ItemStack itemstack = super.finishUsingItem(p_43263_, p_43264_, p_43265_);
      listPotionEffects(itemstack, p_299040_ -> p_43265_.addEffect(p_299040_.createEffectInstance()));
      return p_43265_ instanceof Player && ((Player)p_43265_).getAbilities().instabuild ? itemstack : new ItemStack(Items.BOWL);
   }
}
