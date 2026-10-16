package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

public class LootItemConditions {
   private static final Codec<LootItemCondition> TYPED_CODEC = BuiltInRegistries.LOOT_CONDITION_TYPE
      .byNameCodec()
      .dispatch("condition", LootItemCondition::getType, LootItemConditionType::codec);
   public static final Codec<LootItemCondition> CODEC = ExtraCodecs.lazyInitializedCodec(
      () -> ExtraCodecs.withAlternative(TYPED_CODEC, AllOfCondition.INLINE_CODEC)
   );
   public static final LootItemConditionType INVERTED = register("inverted", InvertedLootItemCondition.CODEC);
   public static final LootItemConditionType ANY_OF = register("any_of", AnyOfCondition.CODEC);
   public static final LootItemConditionType ALL_OF = register("all_of", AllOfCondition.CODEC);
   public static final LootItemConditionType RANDOM_CHANCE = register("random_chance", LootItemRandomChanceCondition.CODEC);
   public static final LootItemConditionType RANDOM_CHANCE_WITH_LOOTING = register("random_chance_with_looting", LootItemRandomChanceWithLootingCondition.CODEC);
   public static final LootItemConditionType ENTITY_PROPERTIES = register("entity_properties", LootItemEntityPropertyCondition.CODEC);
   public static final LootItemConditionType KILLED_BY_PLAYER = register("killed_by_player", LootItemKilledByPlayerCondition.CODEC);
   public static final LootItemConditionType ENTITY_SCORES = register("entity_scores", EntityHasScoreCondition.CODEC);
   public static final LootItemConditionType BLOCK_STATE_PROPERTY = register("block_state_property", LootItemBlockStatePropertyCondition.CODEC);
   public static final LootItemConditionType MATCH_TOOL = register("match_tool", MatchTool.CODEC);
   public static final LootItemConditionType TABLE_BONUS = register("table_bonus", BonusLevelTableCondition.CODEC);
   public static final LootItemConditionType SURVIVES_EXPLOSION = register("survives_explosion", ExplosionCondition.CODEC);
   public static final LootItemConditionType DAMAGE_SOURCE_PROPERTIES = register("damage_source_properties", DamageSourceCondition.CODEC);
   public static final LootItemConditionType LOCATION_CHECK = register("location_check", LocationCheck.CODEC);
   public static final LootItemConditionType WEATHER_CHECK = register("weather_check", WeatherCheck.CODEC);
   public static final LootItemConditionType REFERENCE = register("reference", ConditionReference.CODEC);
   public static final LootItemConditionType TIME_CHECK = register("time_check", TimeCheck.CODEC);
   public static final LootItemConditionType VALUE_CHECK = register("value_check", ValueCheckCondition.CODEC);

   private static LootItemConditionType register(String p_81832_, Codec<? extends LootItemCondition> p_298549_) {
      return Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, new ResourceLocation(p_81832_), new LootItemConditionType(p_298549_));
   }

   public static <T> Predicate<T> andConditions(List<? extends Predicate<T>> p_299005_) {
      List<Predicate<T>> list = List.copyOf(p_299005_);

      return switch(list.size()) {
         case 0 -> p_81840_ -> true;
         case 1 -> (Predicate)list.get(0);
         case 2 -> list.get(0).and(list.get(1));
         default -> p_298184_ -> {
         for(Predicate<T> predicate : list) {
            if (!predicate.test((T)p_298184_)) {
               return false;
            }
         }

         return true;
      };
      };
   }

   public static <T> Predicate<T> orConditions(List<? extends Predicate<T>> p_298918_) {
      List<Predicate<T>> list = List.copyOf(p_298918_);

      return switch(list.size()) {
         case 0 -> p_81830_ -> false;
         case 1 -> (Predicate)list.get(0);
         case 2 -> list.get(0).or(list.get(1));
         default -> p_298186_ -> {
         for(Predicate<T> predicate : list) {
            if (predicate.test((T)p_298186_)) {
               return true;
            }
         }

         return false;
      };
      };
   }
}
