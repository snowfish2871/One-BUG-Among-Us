package net.minecraft.world.level.storage.loot.functions;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class SetNameFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<SetNameFunction> CODEC = RecordCodecBuilder.create(
      p_298145_ -> commonFields(p_298145_)
            .and(
               p_298145_.group(
                  ExtraCodecs.strictOptionalField(ExtraCodecs.COMPONENT, "name").forGetter(p_298144_ -> p_298144_.name),
                  ExtraCodecs.strictOptionalField(LootContext.EntityTarget.CODEC, "entity").forGetter(p_298153_ -> p_298153_.resolutionContext)
               )
            )
            .apply(p_298145_, SetNameFunction::new)
   );
   private final Optional<Component> name;
   private final Optional<LootContext.EntityTarget> resolutionContext;

   private SetNameFunction(List<LootItemCondition> p_299241_, Optional<Component> p_298804_, Optional<LootContext.EntityTarget> p_298545_) {
      super(p_299241_);
      this.name = p_298804_;
      this.resolutionContext = p_298545_;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_NAME;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.resolutionContext.<Set<LootContextParam<?>>>map(p_298146_ -> Set.of(p_298146_.getParam())).orElse(Set.of());
   }

   public static UnaryOperator<Component> createResolver(LootContext p_81140_, @Nullable LootContext.EntityTarget p_81141_) {
      if (p_81141_ != null) {
         Entity entity = p_81140_.getParamOrNull(p_81141_.getParam());
         if (entity != null) {
            CommandSourceStack commandsourcestack = entity.createCommandSourceStack().withPermission(2);
            return p_81147_ -> {
               try {
                  return ComponentUtils.updateForEntity(commandsourcestack, p_81147_, entity, 0);
               } catch (CommandSyntaxException commandsyntaxexception) {
                  LOGGER.warn("Failed to resolve text component", (Throwable)commandsyntaxexception);
                  return p_81147_;
               }
            };
         }
      }

      return p_81152_ -> p_81152_;
   }

   @Override
   public ItemStack run(ItemStack p_81137_, LootContext p_81138_) {
      this.name.ifPresent(p_298149_ -> p_81137_.setHoverName(createResolver(p_81138_, this.resolutionContext.orElse(null)).apply(p_298149_)));
      return p_81137_;
   }

   public static LootItemConditionalFunction.Builder<?> setName(Component p_165458_) {
      return simpleBuilder(p_298143_ -> new SetNameFunction(p_298143_, Optional.of(p_165458_), Optional.empty()));
   }

   public static LootItemConditionalFunction.Builder<?> setName(Component p_165460_, LootContext.EntityTarget p_165461_) {
      return simpleBuilder(p_298152_ -> new SetNameFunction(p_298152_, Optional.of(p_165460_), Optional.of(p_165461_)));
   }
}
