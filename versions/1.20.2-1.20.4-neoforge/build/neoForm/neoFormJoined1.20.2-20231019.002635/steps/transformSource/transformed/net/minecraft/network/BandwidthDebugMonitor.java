package net.minecraft.network;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.SampleLogger;

public class BandwidthDebugMonitor {
   private final AtomicInteger bytesReceived = new AtomicInteger();
   private final SampleLogger bandwidthLogger;

   public BandwidthDebugMonitor(SampleLogger p_298644_) {
      this.bandwidthLogger = p_298644_;
   }

   public void onReceive(int p_298836_) {
      this.bytesReceived.getAndAdd(p_298836_);
   }

   public void tick() {
      this.bandwidthLogger.logSample((long)this.bytesReceived.getAndSet(0));
   }
}
