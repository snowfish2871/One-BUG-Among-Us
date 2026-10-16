package net.minecraft.advancements;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record Advancement(
   Optional<ResourceLocation> parent,
   Optional<DisplayInfo> display,
   AdvancementRewards rewards,
   Map<String, Criterion<?>> criteria,
   AdvancementRequirements requirements,
   boolean sendsTelemetryEvent,
   Optional<Component> name
) {
   public Advancement(
      Optional<ResourceLocation> p_300893_,
      Optional<DisplayInfo> p_301147_,
      AdvancementRewards p_286389_,
      Map<String, Criterion<?>> p_286635_,
      AdvancementRequirements p_301002_,
      boolean p_286478_
   ) {
      this(p_300893_, p_301147_, p_286389_, Map.copyOf(p_286635_), p_301002_, p_286478_, p_301147_.map(Advancement::decorateName));
   }

   private static Component decorateName(DisplayInfo p_301019_) {
      Component component = p_301019_.getTitle();
      ChatFormatting chatformatting = p_301019_.getFrame().getChatColor();
      Component component1 = ComponentUtils.mergeStyles(component.copy(), Style.EMPTY.withColor(chatformatting))
         .append("\n")
         .append(p_301019_.getDescription());
      Component component2 = component.copy().withStyle(p_138316_ -> p_138316_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component1)));
      return ComponentUtils.wrapInSquareBrackets(component2).withStyle(chatformatting);
   }

   public static Component name(AdvancementHolder p_300875_) {
      return p_300875_.value().name().orElseGet(() -> Component.literal(p_300875_.id().toString()));
   }

   public JsonObject serializeToJson() {
      JsonObject jsonobject = new JsonObject();
      this.parent.ifPresent(p_300650_ -> jsonobject.addProperty("parent", p_300650_.toString()));
      this.display.ifPresent(p_300652_ -> jsonobject.add("display", p_300652_.serializeToJson()));
      jsonobject.add("rewards", this.rewards.serializeToJson());
      JsonObject jsonobject1 = new JsonObject();

      for(Entry<String, Criterion<?>> entry : this.criteria.entrySet()) {
         jsonobject1.add(entry.getKey(), entry.getValue().serializeToJson());
      }

      jsonobject.add("criteria", jsonobject1);
      jsonobject.add("requirements", this.requirements.toJson());
      jsonobject.addProperty("sends_telemetry_event", this.sendsTelemetryEvent);
      return jsonobject;
   }

   /** @deprecated Forge: use {@linkplain #fromJson(JsonObject, DeserializationContext, com.mojang.serialization.DynamicOps) overload with context}. */
   @Deprecated
   @Nullable
   public static Advancement fromJson(JsonObject p_301286_, DeserializationContext p_300878_) {
      return fromJson(p_301286_, p_300878_, com.mojang.serialization.JsonOps.INSTANCE);
   }

   private static final com.mojang.serialization.Codec<net.minecraft.util.Unit> DUMMY_CODEC = com.mojang.serialization.Codec.unit(net.minecraft.util.Unit.INSTANCE);
   
   @Nullable
   public static Advancement fromJson(JsonObject p_301286_, DeserializationContext p_300878_, com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> jsonElementOps) {
      if (!net.neoforged.neoforge.common.conditions.ICondition.conditionsMatched(jsonElementOps, p_301286_)) return null;
      Optional<ResourceLocation> optional = p_301286_.has("parent")
         ? Optional.of(new ResourceLocation(GsonHelper.getAsString(p_301286_, "parent")))
         : Optional.empty();
      Optional<DisplayInfo> optional1 = p_301286_.has("display")
         ? Optional.of(DisplayInfo.fromJson(GsonHelper.getAsJsonObject(p_301286_, "display")))
         : Optional.empty();
      AdvancementRewards advancementrewards = p_301286_.has("rewards")
         ? AdvancementRewards.deserialize(GsonHelper.getAsJsonObject(p_301286_, "rewards"))
         : AdvancementRewards.EMPTY;
      Map<String, Criterion<?>> map = Criterion.criteriaFromJson(GsonHelper.getAsJsonObject(p_301286_, "criteria"), p_300878_);
      if (map.isEmpty()) {
         throw new JsonSyntaxException("Advancement criteria cannot be empty");
      } else {
         JsonArray jsonarray = GsonHelper.getAsJsonArray(p_301286_, "requirements", new JsonArray());
         AdvancementRequirements advancementrequirements;
         if (jsonarray.isEmpty()) {
            advancementrequirements = AdvancementRequirements.allOf(map.keySet());
         } else {
            advancementrequirements = AdvancementRequirements.fromJson(jsonarray, map.keySet());
         }

         boolean flag = GsonHelper.getAsBoolean(p_301286_, "sends_telemetry_event", false);
         return new Advancement(optional, optional1, advancementrewards, map, advancementrequirements, flag);
      }
   }

   public void write(FriendlyByteBuf p_301055_) {
      p_301055_.writeOptional(this.parent, FriendlyByteBuf::writeResourceLocation);
      p_301055_.writeOptional(this.display, (p_300647_, p_300648_) -> p_300648_.serializeToNetwork(p_300647_));
      this.requirements.write(p_301055_);
      p_301055_.writeBoolean(this.sendsTelemetryEvent);
   }

   public static Advancement read(FriendlyByteBuf p_301040_) {
      return new Advancement(
         p_301040_.readOptional(FriendlyByteBuf::readResourceLocation),
         p_301040_.readOptional(DisplayInfo::fromNetwork),
         AdvancementRewards.EMPTY,
         Map.of(),
         new AdvancementRequirements(p_301040_),
         p_301040_.readBoolean()
      );
   }

   public boolean isRoot() {
      return this.parent.isEmpty();
   }

   public static class Builder implements net.neoforged.neoforge.common.extensions.IAdvancementBuilderExtension {
      private Optional<ResourceLocation> parent = Optional.empty();
      private Optional<DisplayInfo> display = Optional.empty();
      private AdvancementRewards rewards = AdvancementRewards.EMPTY;
      private final ImmutableMap.Builder<String, Criterion<?>> criteria = ImmutableMap.builder();
      private Optional<AdvancementRequirements> requirements = Optional.empty();
      private AdvancementRequirements.Strategy requirementsStrategy = AdvancementRequirements.Strategy.AND;
      private boolean sendsTelemetryEvent;

      public static Advancement.Builder advancement() {
         return new Advancement.Builder().sendsTelemetryEvent();
      }

      public static Advancement.Builder recipeAdvancement() {
         return new Advancement.Builder();
      }

      public Advancement.Builder parent(AdvancementHolder p_301226_) {
         this.parent = Optional.of(p_301226_.id());
         return this;
      }

      @Deprecated(
         forRemoval = true
      )
      public Advancement.Builder parent(ResourceLocation p_138397_) {
         this.parent = Optional.of(p_138397_);
         return this;
      }

      public Advancement.Builder display(
         ItemStack p_138363_,
         Component p_138364_,
         Component p_138365_,
         @Nullable ResourceLocation p_138366_,
         FrameType p_138367_,
         boolean p_138368_,
         boolean p_138369_,
         boolean p_138370_
      ) {
         return this.display(new DisplayInfo(p_138363_, p_138364_, p_138365_, p_138366_, p_138367_, p_138368_, p_138369_, p_138370_));
      }

      public Advancement.Builder display(
         ItemLike p_138372_,
         Component p_138373_,
         Component p_138374_,
         @Nullable ResourceLocation p_138375_,
         FrameType p_138376_,
         boolean p_138377_,
         boolean p_138378_,
         boolean p_138379_
      ) {
         return this.display(new DisplayInfo(new ItemStack(p_138372_.asItem()), p_138373_, p_138374_, p_138375_, p_138376_, p_138377_, p_138378_, p_138379_));
      }

      public Advancement.Builder display(DisplayInfo p_138359_) {
         this.display = Optional.of(p_138359_);
         return this;
      }

      public Advancement.Builder rewards(AdvancementRewards.Builder p_138355_) {
         return this.rewards(p_138355_.build());
      }

      public Advancement.Builder rewards(AdvancementRewards p_138357_) {
         this.rewards = p_138357_;
         return this;
      }

      public Advancement.Builder addCriterion(String p_138384_, Criterion<?> p_138385_) {
         this.criteria.put(p_138384_, p_138385_);
         return this;
      }

      public Advancement.Builder requirements(AdvancementRequirements.Strategy p_300955_) {
         this.requirementsStrategy = p_300955_;
         return this;
      }

      public Advancement.Builder requirements(AdvancementRequirements p_301103_) {
         this.requirements = Optional.of(p_301103_);
         return this;
      }

      public Advancement.Builder sendsTelemetryEvent() {
         this.sendsTelemetryEvent = true;
         return this;
      }

      public AdvancementHolder build(ResourceLocation p_138404_) {
         Map<String, Criterion<?>> map = this.criteria.buildOrThrow();
         AdvancementRequirements advancementrequirements = this.requirements.orElseGet(() -> this.requirementsStrategy.create(map.keySet()));
         return new AdvancementHolder(
            p_138404_, new Advancement(this.parent, this.display, this.rewards, map, advancementrequirements, this.sendsTelemetryEvent)
         );
      }

      public AdvancementHolder save(Consumer<AdvancementHolder> p_138390_, String p_138391_) {
         AdvancementHolder advancementholder = this.build(new ResourceLocation(p_138391_));
         p_138390_.accept(advancementholder);
         return advancementholder;
      }
   }
}
