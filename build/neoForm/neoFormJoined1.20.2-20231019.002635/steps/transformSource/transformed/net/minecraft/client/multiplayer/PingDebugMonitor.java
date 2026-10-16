package net.minecraft.client.multiplayer;

import net.minecraft.Util;
import net.minecraft.network.protocol.status.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.status.ServerboundPingRequestPacket;
import net.minecraft.util.SampleLogger;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PingDebugMonitor {
   private final ClientPacketListener connection;
   private final SampleLogger delayTimer;

   public PingDebugMonitor(ClientPacketListener p_298598_, SampleLogger p_298618_) {
      this.connection = p_298598_;
      this.delayTimer = p_298618_;
   }

   public void tick() {
      this.connection.send(new ServerboundPingRequestPacket(Util.getMillis()));
   }

   public void onPongReceived(ClientboundPongResponsePacket p_298505_) {
      this.delayTimer.logSample(Util.getMillis() - p_298505_.getTime());
   }
}
