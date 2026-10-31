package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.Nullable;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.config.JoinWorldTask;
import net.minecraft.server.network.config.ServerResourcePackConfigurationTask;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.flag.FeatureFlags;
import org.slf4j.Logger;

public class ServerConfigurationPacketListenerImpl extends ServerCommonPacketListenerImpl implements TickablePacketListener, ServerConfigurationPacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component DISCONNECT_REASON_INVALID_DATA = Component.translatable("multiplayer.disconnect.invalid_player_data");
   private final GameProfile gameProfile;
   private final Queue<ConfigurationTask> configurationTasks = new ConcurrentLinkedQueue<>();
   @Nullable
   private ConfigurationTask currentTask;
   private ClientInformation clientInformation;

   public ServerConfigurationPacketListenerImpl(MinecraftServer p_294645_, Connection p_295787_, CommonListenerCookie p_302003_) {
      super(p_294645_, p_295787_, p_302003_);
      this.gameProfile = p_302003_.gameProfile();
      this.clientInformation = p_302003_.clientInformation();
   }

   @Override
   protected GameProfile playerProfile() {
      return this.gameProfile;
   }

   @Override
   public void onDisconnect(Component p_295037_) {
      LOGGER.info("{} lost connection: {}", this.gameProfile, p_295037_.getString());
      super.onDisconnect(p_295037_);
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   public void startConfiguration() {
      this.send(new ClientboundCustomPayloadPacket(new BrandPayload(this.server.getServerModName())));
      LayeredRegistryAccess<RegistryLayer> layeredregistryaccess = this.server.registries();
      this.send(new ClientboundUpdateEnabledFeaturesPacket(FeatureFlags.REGISTRY.toNames(this.server.getWorldData().enabledFeatures())));
      this.send(
         new ClientboundRegistryDataPacket(
            new RegistryAccess.ImmutableRegistryAccess(RegistrySynchronization.networkedRegistries(layeredregistryaccess)).freeze()
         )
      );
      this.send(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(layeredregistryaccess)));
      this.addOptionalTasks();
      this.configurationTasks.add(new JoinWorldTask());
      this.startNextTask();
   }

   public void returnToWorld() {
      this.configurationTasks.add(new JoinWorldTask());
      this.startNextTask();
   }

   private void addOptionalTasks() {
      this.server.getServerResourcePack().ifPresent(p_296496_ -> this.configurationTasks.add(new ServerResourcePackConfigurationTask(p_296496_)));
   }

   @Override
   public void handleClientInformation(ServerboundClientInformationPacket p_302032_) {
      this.clientInformation = p_302032_.information();
   }

   @Override
   public void handleResourcePackResponse(ServerboundResourcePackPacket p_294250_) {
      super.handleResourcePackResponse(p_294250_);
      if (p_294250_.getAction() != ServerboundResourcePackPacket.Action.ACCEPTED) {
         this.finishCurrentTask(ServerResourcePackConfigurationTask.TYPE);
      }
   }

   @Override
   public void handleConfigurationFinished(ServerboundFinishConfigurationPacket p_294283_) {
      this.connection.suspendInboundAfterProtocolChange();
      PacketUtils.ensureRunningOnSameThread(p_294283_, this, this.server);
      this.finishCurrentTask(JoinWorldTask.TYPE);

      try {
         PlayerList playerlist = this.server.getPlayerList();
         if (playerlist.getPlayer(this.gameProfile.getId()) != null) {
            this.disconnect(PlayerList.DUPLICATE_LOGIN_DISCONNECT_MESSAGE);
            return;
         }

         Component component = playerlist.canPlayerLogin(this.connection.getRemoteAddress(), this.gameProfile);
         if (component != null) {
            this.disconnect(component);
            return;
         }

         ServerPlayer serverplayer = playerlist.getPlayerForLogin(this.gameProfile, this.clientInformation);
         playerlist.placeNewPlayer(this.connection, serverplayer, this.createCookie(this.clientInformation));
         this.connection.resumeInboundAfterProtocolChange();
      } catch (Exception exception) {
         LOGGER.error("Couldn't place player in world", (Throwable)exception);
         this.connection.send(new ClientboundDisconnectPacket(DISCONNECT_REASON_INVALID_DATA));
         this.connection.disconnect(DISCONNECT_REASON_INVALID_DATA);
      }
   }

   @Override
   public void tick() {
      this.keepConnectionAlive();
   }

   private void startNextTask() {
      if (this.currentTask != null) {
         throw new IllegalStateException("Task " + this.currentTask.type().id() + " has not finished yet");
      } else if (this.isAcceptingMessages()) {
         ConfigurationTask configurationtask = this.configurationTasks.poll();
         if (configurationtask != null) {
            this.currentTask = configurationtask;
            configurationtask.start(this::send);
         }
      }
   }

   private void finishCurrentTask(ConfigurationTask.Type p_294853_) {
      ConfigurationTask.Type configurationtask$type = this.currentTask != null ? this.currentTask.type() : null;
      if (!p_294853_.equals(configurationtask$type)) {
         throw new IllegalStateException("Unexpected request for task finish, current task: " + configurationtask$type + ", requested: " + p_294853_);
      } else {
         this.currentTask = null;
         this.startNextTask();
      }
   }
}
