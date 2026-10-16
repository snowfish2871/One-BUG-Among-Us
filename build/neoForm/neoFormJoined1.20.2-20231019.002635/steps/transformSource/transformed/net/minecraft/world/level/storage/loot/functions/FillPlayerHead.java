package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class FillPlayerHead extends LootItemConditionalFunction {
   public static final Codec<FillPlayerHead> CODEC = RecordCodecBuilder.create(
      p_298087_ -> commonFields(p_298087_)
            .and(LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(p_298086_ -> p_298086_.entityTarget))
            .apply(p_298087_, FillPlayerHead::new)
   );
   private final LootContext.EntityTarget entityTarget;

   public FillPlayerHead(List<LootItemCondition> p_298265_, LootContext.EntityTarget p_80605_) {
      super(p_298265_);
      this.entityTarget = p_80605_;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.FILL_PLAYER_HEAD;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(this.entityTarget.getParam());
   }

   @Override
   public ItemStack run(ItemStack p_80608_, LootContext p_80609_) {
      if (p_80608_.is(Items.PLAYER_HEAD)) {
         Object $$3 = p_80609_.getParamOrNull(this.entityTarget.getParam());
         if ($$3 instanceof Player player) {
            GameProfile gameprofile = player.getGameProfile();
            p_80608_.getOrCreateTag().put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), gameprofile));
         }
      }

      return p_80608_;
   }

   public static LootItemConditionalFunction.Builder<?> fillPlayerHead(LootContext.EntityTarget p_165208_) {
      return simpleBuilder(p_298085_ -> new FillPlayerHead(p_298085_, p_165208_));
   }
}
