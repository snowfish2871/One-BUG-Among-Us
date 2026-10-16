package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import org.slf4j.Logger;

public class PacketFlowValidator extends MessageToMessageCodec<Packet<?>, Packet<?>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final AttributeKey<ConnectionProtocol.CodecData<?>> decoderKey;
   private final AttributeKey<ConnectionProtocol.CodecData<?>> encoderKey;

   public PacketFlowValidator(AttributeKey<ConnectionProtocol.CodecData<?>> p_295922_, AttributeKey<ConnectionProtocol.CodecData<?>> p_295833_) {
      this.decoderKey = p_295922_;
      this.encoderKey = p_295833_;
   }

   private static void validatePacket(
      ChannelHandlerContext p_294940_, Packet<?> p_294901_, List<Object> p_296334_, AttributeKey<ConnectionProtocol.CodecData<?>> p_296035_
   ) {
      Attribute<ConnectionProtocol.CodecData<?>> attribute = p_294940_.channel().attr(p_296035_);
      ConnectionProtocol.CodecData<?> codecdata = attribute.get();
      if (!codecdata.isValidPacketType(p_294901_)) {
         LOGGER.error("Unrecognized packet in pipeline {}:{} - {}", codecdata.protocol().id(), codecdata.flow(), p_294901_);
      }

      ReferenceCountUtil.retain(p_294901_);
      p_296334_.add(p_294901_);
      ProtocolSwapHandler.swapProtocolIfNeeded(attribute, p_294901_);
   }

   protected void decode(ChannelHandlerContext p_294271_, Packet<?> p_296315_, List<Object> p_294768_) throws Exception {
      validatePacket(p_294271_, p_296315_, p_294768_, this.decoderKey);
   }

   protected void encode(ChannelHandlerContext p_296253_, Packet<?> p_296139_, List<Object> p_295975_) throws Exception {
      validatePacket(p_296253_, p_296139_, p_295975_, this.encoderKey);
   }
}
