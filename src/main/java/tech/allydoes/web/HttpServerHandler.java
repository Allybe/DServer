package tech.allydoes.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger LOGGER = LogManager.getLogger(HttpServerHandler.class);
    private final HashMap<String, RequestHandler> requestHandlers = new HashMap<>();

    public HttpServerHandler() {
        RequestHandler[] handlers = {

        };
        for (RequestHandler handler : handlers) {
            requestHandlers.put(handler.getRequestName().toLowerCase(), handler);
        }
    }

    public static ChannelFuture sendContent(String content, FullHttpRequest request, ChannelHandlerContext channelHandlerContext) {
        ByteBuf byteBuf = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK, byteBuf);
        httpResponse.headers().set(CONTENT_TYPE, "application/json; charset=utf-8");
        httpResponse.headers().set(CONTENT_LENGTH, byteBuf.readableBytes());
        return channelHandlerContext.writeAndFlush(httpResponse);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext channelHandlerContext) {
        channelHandlerContext.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) {
        ChannelFuture future = processRequest(channelHandlerContext, request);

        request.headers().set(CONNECTION, CLOSE);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    private ChannelFuture processRequest(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) {
        String path = request.uri().split("\\?", 15)[0].toLowerCase();
        RequestHandler requestHandler = requestHandlers.get(path);
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();

        if (requestHandler != null && requestHandler.getRequestType().equalsIgnoreCase(request.method().name())) {
            if (!hasRequiredParameters(params, requestHandler.getRequiredParameters())) {
                return channelHandlerContext.writeAndFlush(new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.BAD_REQUEST));
            }

            return requestHandler.processRequest(channelHandlerContext, request, params);
        } else {
            return channelHandlerContext.writeAndFlush(new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.NOT_FOUND));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable cause) {
        LOGGER.info("Unhandled exception in pipeline", cause);

        channelHandlerContext.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR));
        channelHandlerContext.close();
    }

    private boolean hasRequiredParameters(Map<String, List<String>> parameters, String[] requiredParameters) {
        for (String requiredParameter : requiredParameters) {
            if (!parameters.containsKey(requiredParameter)) {
                return false;
            }
        }

        for (List<String> parameterArray: parameters.values()) {
            String parameter = parameterArray.getFirst();
            if (parameter.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
