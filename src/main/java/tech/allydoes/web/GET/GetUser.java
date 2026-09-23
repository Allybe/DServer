package tech.allydoes.web.GET;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import tech.allydoes.web.RequestHandler;

import java.util.List;
import java.util.Map;

public class GetUser implements RequestHandler {

    @Override
    public ChannelFuture processRequest(ChannelHandlerContext channelHandlerContext, FullHttpRequest request, Map<String, List<String>> parameters) {
        return null;
    }

    @Override
    public String getRequestName() {
        return "/GetUser";
    }

    @Override
    public String getRequestType() {
        return "GET";
    }

    @Override
    public String[] getRequiredParameters() {
        return new String[0];
    }
}
