package tech.allydoes.web;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.List;
import java.util.Map;

public interface RequestHandler {
    ChannelFuture processRequest(ChannelHandlerContext channelHandlerContext, FullHttpRequest request, Map<String, List<String>> parameters);
    String getRequestName();
    String getRequestType();
    String[] getRequiredParameters();
}
