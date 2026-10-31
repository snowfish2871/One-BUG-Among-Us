package net.minecraft.network;

import io.netty.util.Attribute;
import net.minecraft.network.protocol.Packet;

public interface ProtocolSwapHandler {
   static void swapProtocolIfNeeded(Attribute<ConnectionProtocol.CodecData<?>> p_295842_, Packet<?> p_294980_) {
      ConnectionProtocol connectionprotocol = p_294980_.nextProtocol();
      if (connectionprotocol != null) {
         ConnectionProtocol.CodecData<?> codecdata = p_295842_.get();
         ConnectionProtocol connectionprotocol1 = codecdata.protocol();
         if (connectionprotocol != connectionprotocol1) {
            ConnectionProtocol.CodecData<?> codecdata1 = connectionprotocol.codec(codecdata.flow());
            p_295842_.set(codecdata1);
         }
      }
   }
}
