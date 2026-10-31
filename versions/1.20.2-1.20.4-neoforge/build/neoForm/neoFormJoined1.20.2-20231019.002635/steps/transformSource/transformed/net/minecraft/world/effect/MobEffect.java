package net.minecraft.world.effect;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class MobEffect implements net.neoforged.neoforge.common.extensions.IMobEffectExtension {
   private final Map<Attribute, AttributeModifierTemplate> attributeModifiers = Maps.newHashMap();
   private final MobEffectCategory category;
   private final int color;
   @Nullable
   private String descriptionId;
   private Supplier<MobEffectInstance.FactorData> factorDataFactory = () -> null;
   private final Holder.Reference<MobEffect> builtInRegistryHolder = BuiltInRegistries.MOB_EFFECT.createIntrusiveHolder(this);

   protected MobEffect(MobEffectCategory p_19451_, int p_19452_) {
      this.category = p_19451_;
      this.color = p_19452_;
      initClient();
   }

   public Optional<MobEffectInstance.FactorData> createFactorData() {
      return Optional.ofNullable(this.factorDataFactory.get());
   }

   public void applyEffectTick(LivingEntity p_19467_, int p_19468_) {
   }

   public void applyInstantenousEffect(@Nullable Entity p_19462_, @Nullable Entity p_19463_, LivingEntity p_19464_, int p_19465_, double p_19466_) {
      this.applyEffectTick(p_19464_, p_19465_);
   }

   public boolean shouldApplyEffectTickThisTick(int p_295329_, int p_295167_) {
      return false;
   }

   public void onEffectStarted(LivingEntity p_296490_, int p_296147_) {
   }

   public boolean isInstantenous() {
      return false;
   }

   protected String getOrCreateDescriptionId() {
      if (this.descriptionId == null) {
         this.descriptionId = Util.makeDescriptionId("effect", BuiltInRegistries.MOB_EFFECT.getKey(this));
      }

      return this.descriptionId;
   }

   public String getDescriptionId() {
      return this.getOrCreateDescriptionId();
   }

   public Component getDisplayName() {
      return Component.translatable(this.getDescriptionId());
   }

   public MobEffectCategory getCategory() {
      return this.category;
   }

   public int getColor() {
      return this.color;
   }

   public MobEffect addAttributeModifier(Attribute p_19473_, String p_19474_, double p_19475_, AttributeModifier.Operation p_19476_) {
      this.attributeModifiers.put(p_19473_, new MobEffect.MobEffectAttributeModifierTemplate(UUID.fromString(p_19474_), p_19475_, p_19476_));
      return this;
   }

   public MobEffect setFactorDataFactory(Supplier<MobEffectInstance.FactorData> p_216880_) {
      this.factorDataFactory = p_216880_;
      return this;
   }

   public Map<Attribute, AttributeModifierTemplate> getAttributeModifiers() {
      return this.attributeModifiers;
   }

   public void removeAttributeModifiers(AttributeMap p_19470_) {
      for(Entry<Attribute, AttributeModifierTemplate> entry : this.attributeModifiers.entrySet()) {
         AttributeInstance attributeinstance = p_19470_.getInstance(entry.getKey());
         if (attributeinstance != null) {
            attributeinstance.removeModifier(entry.getValue().getAttributeModifierId());
         }
      }
   }

   public void addAttributeModifiers(AttributeMap p_19479_, int p_19480_) {
      for(Entry<Attribute, AttributeModifierTemplate> entry : this.attributeModifiers.entrySet()) {
         AttributeInstance attributeinstance = p_19479_.getInstance(entry.getKey());
         if (attributeinstance != null) {
            attributeinstance.removeModifier(entry.getValue().getAttributeModifierId());
            attributeinstance.addPermanentModifier(entry.getValue().create(p_19480_));
         }
      }
   }

   public boolean isBeneficial() {
      return this.category == MobEffectCategory.BENEFICIAL;
   }

   // FORGE START
   private Object effectRenderer;

   /*
      DO NOT CALL, IT WILL DISAPPEAR IN THE FUTURE
      Call RenderProperties.getEffectRenderer instead
    */
   public Object getEffectRendererInternal() {
      return effectRenderer;
   }

   private void initClient() {
      // Minecraft instance isn't available in datagen, so don't call initializeClient if in datagen
      if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT && !net.neoforged.fml.loading.FMLLoader.getLaunchHandler().isData()) {
         initializeClient(properties -> {
            this.effectRenderer = properties;
         });
      }
   }

   public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions> consumer) {
   }
   // END FORGE


   @Deprecated
   public Holder.Reference<MobEffect> builtInRegistryHolder() {
      return this.builtInRegistryHolder;
   }

   class MobEffectAttributeModifierTemplate implements AttributeModifierTemplate {
      private final UUID id;
      private final double amount;
      private final AttributeModifier.Operation operation;

      public MobEffectAttributeModifierTemplate(UUID p_295616_, double p_294530_, AttributeModifier.Operation p_294958_) {
         this.id = p_295616_;
         this.amount = p_294530_;
         this.operation = p_294958_;
      }

      @Override
      public UUID getAttributeModifierId() {
         return this.id;
      }

      @Override
      public AttributeModifier create(int p_294228_) {
         return new AttributeModifier(this.id, MobEffect.this.getDescriptionId() + " " + p_294228_, this.amount * (double)(p_294228_ + 1), this.operation);
      }
   }
}
