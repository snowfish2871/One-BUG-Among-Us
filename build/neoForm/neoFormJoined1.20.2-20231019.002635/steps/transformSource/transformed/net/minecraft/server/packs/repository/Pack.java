package net.minecraft.server.packs.repository;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.flag.FeatureFlagSet;
import org.slf4j.Logger;

public class Pack {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String id;
   private final Pack.ResourcesSupplier resources;
   private final Component title;
   private final Pack.Info info;
   private final Pack.Position defaultPosition;
   private final boolean required;
   private final boolean fixedPosition;
   private final boolean hidden; // Forge: Allow packs to be hidden from the UI entirely
   private final PackSource packSource;

   @Nullable
   public static Pack readMetaAndCreate(
      String p_249649_,
      Component p_248632_,
      boolean p_251594_,
      Pack.ResourcesSupplier p_252210_,
      PackType p_250595_,
      Pack.Position p_248706_,
      PackSource p_251233_
   ) {
      int i = SharedConstants.getCurrentVersion().getPackVersion(p_250595_);
      Pack.Info pack$info = readPackInfo(p_249649_, p_252210_, i);
      return pack$info != null ? create(p_249649_, p_248632_, p_251594_, p_252210_, pack$info, p_248706_, false, p_251233_) : null;
   }

   public static Pack create(
      String p_252257_,
      Component p_248717_,
      boolean p_248811_,
      Pack.ResourcesSupplier p_248969_,
      Pack.Info p_251314_,
      Pack.Position p_252110_,
      boolean p_250237_,
      PackSource p_248524_
   ) {
      return new Pack(p_252257_, p_248811_, p_248969_, p_248717_, p_251314_, p_252110_, p_250237_, p_248524_);
   }

   private Pack(
      String p_252218_,
      boolean p_248829_,
      Pack.ResourcesSupplier p_249377_,
      Component p_251718_,
      Pack.Info p_250162_,
      Pack.Position p_251298_,
      boolean p_249753_,
      PackSource p_251608_
   ) {
      this.id = p_252218_;
      this.resources = p_249377_;
      this.title = p_251718_;
      this.info = p_250162_;
      this.required = p_248829_;
      this.defaultPosition = p_251298_;
      this.fixedPosition = p_249753_;
      this.packSource = p_251608_;
      this.hidden = p_250162_.isHidden();
   }

   @Nullable
   public static Pack.Info readPackInfo(String p_250591_, Pack.ResourcesSupplier p_250739_, int p_294759_) {
      try {
         Pack.Info pack$info;
         try (PackResources packresources = p_250739_.openPrimary(p_250591_)) {
            PackMetadataSection packmetadatasection = packresources.getMetadataSection(PackMetadataSection.TYPE);
            if (packmetadatasection == null) {
               LOGGER.warn("Missing metadata in pack {}", p_250591_);
               return null;
            }

            FeatureFlagsMetadataSection featureflagsmetadatasection = packresources.getMetadataSection(FeatureFlagsMetadataSection.TYPE);
            FeatureFlagSet featureflagset = featureflagsmetadatasection != null ? featureflagsmetadatasection.flags() : FeatureFlagSet.of();
            InclusiveRange<Integer> inclusiverange = getDeclaredPackVersions(p_250591_, packmetadatasection);
            PackCompatibility packcompatibility = PackCompatibility.forVersion(inclusiverange, p_294759_);
            OverlayMetadataSection overlaymetadatasection = packresources.getMetadataSection(OverlayMetadataSection.TYPE);
            List<String> list = overlaymetadatasection != null ? overlaymetadatasection.overlaysForVersion(p_294759_) : List.of();
            pack$info = new Pack.Info(packmetadatasection.description(), packmetadatasection.packFormat(PackType.SERVER_DATA), packmetadatasection.packFormat(PackType.CLIENT_RESOURCES), packcompatibility, featureflagset, list, packresources.isHidden());
         }

         return pack$info;
      } catch (Exception exception) {
         LOGGER.warn("Failed to read pack {} metadata", p_250591_, exception);
         return null;
      }
   }

   private static InclusiveRange<Integer> getDeclaredPackVersions(String p_295795_, PackMetadataSection p_294612_) {
      int i = p_294612_.packFormat();
      if (p_294612_.supportedFormats().isEmpty()) {
         return new InclusiveRange<>(i);
      } else {
         InclusiveRange<Integer> inclusiverange = p_294612_.supportedFormats().get();
         if (!inclusiverange.isValueInRange(i)) {
            LOGGER.warn("Pack {} declared support for versions {} but declared main format is {}, defaulting to {}", p_295795_, inclusiverange, i, i);
            return new InclusiveRange<>(i);
         } else {
            return inclusiverange;
         }
      }
   }

   public Component getTitle() {
      return this.title;
   }

   public Component getDescription() {
      return this.info.description();
   }

   public Component getChatLink(boolean p_10438_) {
      return ComponentUtils.wrapInSquareBrackets(this.packSource.decorate(Component.literal(this.id)))
         .withStyle(
            p_293812_ -> p_293812_.withColor(p_10438_ ? ChatFormatting.GREEN : ChatFormatting.RED)
                  .withInsertion(StringArgumentType.escapeIfRequired(this.id))
                  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.empty().append(this.title).append("\n").append(this.info.description)))
         );
   }

   public PackCompatibility getCompatibility() {
      return this.info.compatibility();
   }

   public FeatureFlagSet getRequestedFeatures() {
      return this.info.requestedFeatures();
   }

   public PackResources open() {
      return this.resources.openFull(this.id, this.info);
   }

   public String getId() {
      return this.id;
   }

   public boolean isRequired() {
      return this.required;
   }

   public boolean isFixedPosition() {
      return this.fixedPosition;
   }

   public Pack.Position getDefaultPosition() {
      return this.defaultPosition;
   }

   public PackSource getPackSource() {
      return this.packSource;
   }

   public boolean isHidden() { return hidden; }

   public boolean equals(Object p_10448_) {
      if (this == p_10448_) {
         return true;
      } else if (!(p_10448_ instanceof Pack)) {
         return false;
      } else {
         Pack pack = (Pack)p_10448_;
         return this.id.equals(pack.id);
      }
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   public static record Info(Component description, int dataFormat, int resourceFormat, PackCompatibility compatibility, FeatureFlagSet requestedFeatures, List<String> overlays, boolean isHidden) {
      public int getFormat(PackType type) {
         return type == PackType.SERVER_DATA ? this.dataFormat : this.resourceFormat;
      }
   }

   public static enum Position {
      TOP,
      BOTTOM;

      public <T> int insert(List<T> p_10471_, T p_10472_, Function<T, Pack> p_10473_, boolean p_10474_) {
         Pack.Position pack$position = p_10474_ ? this.opposite() : this;
         if (pack$position == BOTTOM) {
            int j;
            for(j = 0; j < p_10471_.size(); ++j) {
               Pack pack1 = p_10473_.apply(p_10471_.get(j));
               if (!pack1.isFixedPosition() || pack1.getDefaultPosition() != this) {
                  break;
               }
            }

            p_10471_.add(j, p_10472_);
            return j;
         } else {
            int i;
            for(i = p_10471_.size() - 1; i >= 0; --i) {
               Pack pack = p_10473_.apply(p_10471_.get(i));
               if (!pack.isFixedPosition() || pack.getDefaultPosition() != this) {
                  break;
               }
            }

            p_10471_.add(i + 1, p_10472_);
            return i + 1;
         }
      }

      public Pack.Position opposite() {
         return this == TOP ? BOTTOM : TOP;
      }
   }

   public interface ResourcesSupplier {
      PackResources openPrimary(String p_294636_);

      PackResources openFull(String p_251717_, Pack.Info p_294956_);
   }
}
