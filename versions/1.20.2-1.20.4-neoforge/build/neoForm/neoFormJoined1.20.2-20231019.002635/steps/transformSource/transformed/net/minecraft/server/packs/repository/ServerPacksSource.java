package net.minecraft.server.packs.repository;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.BuiltInMetadata;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;

public class ServerPacksSource extends BuiltInPackSource {
   private static final PackMetadataSection VERSION_METADATA_SECTION = new PackMetadataSection(
      Component.translatable("dataPack.vanilla.description"), SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA), Optional.empty()
   );
   private static final FeatureFlagsMetadataSection FEATURE_FLAGS_METADATA_SECTION = new FeatureFlagsMetadataSection(FeatureFlags.DEFAULT_FLAGS);
   private static final BuiltInMetadata BUILT_IN_METADATA = BuiltInMetadata.of(
      PackMetadataSection.TYPE, VERSION_METADATA_SECTION, FeatureFlagsMetadataSection.TYPE, FEATURE_FLAGS_METADATA_SECTION
   );
   private static final Component VANILLA_NAME = Component.translatable("dataPack.vanilla.name");
   private static final ResourceLocation PACKS_DIR = new ResourceLocation("minecraft", "datapacks");

   public ServerPacksSource(DirectoryValidator p_294494_) {
      super(PackType.SERVER_DATA, createVanillaPackSource(), PACKS_DIR, p_294494_);
   }

   @VisibleForTesting
   public static VanillaPackResources createVanillaPackSource() {
      return new VanillaPackResourcesBuilder().setMetadata(BUILT_IN_METADATA).exposeNamespace("minecraft").applyDevelopmentConfig().pushJarResources().build();
   }

   @Override
   protected Component getPackTitle(String p_249692_) {
      return Component.literal(p_249692_);
   }

   @Nullable
   @Override
   protected Pack createVanillaPack(PackResources p_250283_) {
      return Pack.readMetaAndCreate("vanilla", VANILLA_NAME, false, fixedResources(p_250283_), PackType.SERVER_DATA, Pack.Position.BOTTOM, PackSource.BUILT_IN);
   }

   @Nullable
   @Override
   protected Pack createBuiltinPack(String p_250596_, Pack.ResourcesSupplier p_249625_, Component p_249043_) {
      return Pack.readMetaAndCreate(p_250596_, p_249043_, false, p_249625_, PackType.SERVER_DATA, Pack.Position.TOP, PackSource.FEATURE);
   }

   public static PackRepository createPackRepository(Path p_251569_, DirectoryValidator p_295336_) {
      final PackRepository packRepository = new PackRepository(new ServerPacksSource(p_295336_), new FolderRepositorySource(p_251569_, PackType.SERVER_DATA, PackSource.WORLD, p_295336_));
      net.neoforged.fml.ModLoader.get().postEvent(new net.neoforged.neoforge.event.AddPackFindersEvent(PackType.SERVER_DATA, packRepository::addPackFinder));
      return packRepository;
   }

   public static PackRepository createVanillaTrustedRepository() {
      return new PackRepository(new ServerPacksSource(new DirectoryValidator(p_293813_ -> true)));
   }

   public static PackRepository createPackRepository(LevelStorageSource.LevelStorageAccess p_250213_) {
      return createPackRepository(p_250213_.getLevelPath(LevelResource.DATAPACK_DIR), p_250213_.parent().getWorldDirValidator());
   }
}
