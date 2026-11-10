package web;

import database.DatabaseManager;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.Handlers;
import io.undertow.server.*;
import io.undertow.server.handlers.encoding.*;
import io.undertow.server.handlers.resource.*;
import io.undertow.util.*;
import io.undertow.websockets.core.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import server.Manager;
import server.ServerManager;

public final class WebServer {
    public static void main(String[] args) throws Exception {
        DatabaseManager.initialize();
        ServerManager.isRunning=true;
        Manager.gI();
        Manager.MAX_PLAYER=WebConfig.MAX_PLAYERS;
        Manager.MAX_PER_IP=WebConfig.MAX_PLAYERS;
        GameCatalog.load();
        GameWorld world=new GameWorld();
        ResourceHandler web=new ResourceHandler(new PathResourceManager(Path.of("web").toAbsolutePath(), 1024)).setDirectoryListingEnabled(false);
        ResourceHandler assets=new ResourceHandler(new PathResourceManager(Path.of("data").toAbsolutePath(),1024)).setDirectoryListingEnabled(false);
        HttpHandler socket=Handlers.websocket((exchange,channel)->{
            WebSession session=new WebSession(world,channel,channel.getSourceAddress().getAddress().getHostAddress());
            channel.setIdleTimeout(95000);
            channel.getReceiveSetter().set(new AbstractReceiveListener(){
                @Override protected long getMaxTextBufferSize(){return 4096;}
                @Override protected long getMaxBinaryBufferSize(){return 0;}
                @Override protected void onFullTextMessage(WebSocketChannel ch,BufferedTextMessage message){world.receive(session,message.getData());}
                @Override protected void onFullPongMessage(WebSocketChannel ch, BufferedBinaryMessage message) throws IOException {
                    super.onFullPongMessage(ch, message);
                    if (session.awaitingAutoPong) {
                        session.lastAutoPong = System.currentTimeMillis();
                        session.awaitingAutoPong = false;
                    }
                }
                @Override protected void onError(WebSocketChannel ch,Throwable error){world.disconnected(session);try{ch.close();}catch(IOException ignored){}}
            });
            channel.addCloseTask(ch->world.disconnected(session));
            world.connected(session);channel.resumeReceives();
        });
        HttpHandler router=exchange->{
            exchange.getResponseHeaders().put(Headers.X_CONTENT_TYPE_OPTIONS,"nosniff");
            exchange.getResponseHeaders().put(new HttpString("Referrer-Policy"),"same-origin");
            exchange.getResponseHeaders().put(new HttpString("Content-Security-Policy"),"default-src 'self'; img-src 'self' data:; connect-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
            String path=exchange.getRequestPath();
            if(path.equals("/ws")){
                String origin=exchange.getRequestHeaders().getFirst(Headers.ORIGIN);
                String allowed=DatabaseManager.env("PUBLIC_ORIGIN","");
                String host=exchange.getRequestHeaders().getFirst(Headers.HOST);
                boolean same=origin!=null&&(origin.equals("https://"+host)||origin.equals("http://"+host)||(!allowed.isBlank()&&origin.equals(allowed)));
                if(!same){exchange.setStatusCode(403);exchange.endExchange();return;}
                socket.handleRequest(exchange);return;
            }
            if(!exchange.getRequestMethod().equals(Methods.GET)&&!exchange.getRequestMethod().equals(Methods.HEAD)){exchange.setStatusCode(405);exchange.endExchange();return;}
            if(path.equals("/healthz")) {
                boolean healthy=world.ready&&System.currentTimeMillis()-world.lastTick<30000;
                exchange.setStatusCode(healthy?200:503);json(exchange,Map.of("ready",healthy,"online",world.online));return;
            }
            if(path.equals("/api/status")){json(exchange,Map.of("online",world.online,"maxPlayers",WebConfig.MAX_PLAYERS,"inviteRequired",!DatabaseManager.env("INVITE_CODE","").isEmpty()));return;}
            if(path.equals("/api/catalog")){exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,"application/json; charset=utf-8");exchange.getResponseHeaders().put(Headers.CACHE_CONTROL,"public, max-age=3600");exchange.getResponseSender().send(GameCatalog.json);return;}
            if(path.startsWith("/assets/")) {
                String relative=path.substring("/assets".length());
                if(!relative.matches("/(icon/x1/\\d+\\.png|item_bg_temp/x1/\\d+\\.png|res/x1/[A-Za-z0-9_.-]+|web-mob/\\d+\\.png)")){exchange.setStatusCode(404);exchange.endExchange();return;}
                exchange.setRelativePath(relative);exchange.getResponseHeaders().put(Headers.CACHE_CONTROL,"public, max-age=86400");
                if(!relative.endsWith(".png"))exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,"image/png");
                assets.handleRequest(exchange);return;
            }
            exchange.getResponseHeaders().put(Headers.CACHE_CONTROL,"no-cache");
            web.handleRequest(exchange);
        };
        int port=Integer.parseInt(DatabaseManager.env("PORT","8080"));
        var encoding=new EncodingHandler(new ContentEncodingRepository().addEncodingHandler("gzip",new GzipEncodingProvider(),50));
        encoding.setNext(router);
        Undertow server=Undertow.builder().addHttpListener(port,"0.0.0.0").setIoThreads(2).setWorkerThreads(8).setServerOption(UndertowOptions.MAX_ENTITY_SIZE,4096L).setHandler(encoding).build();
        Runtime.getRuntime().addShutdownHook(new Thread(()->{world.close();ServerManager.isRunning=false;server.stop();DatabaseManager.close();},"save-on-shutdown"));
        server.start();
        System.out.println("NRO Web ready on port "+port+"; capacity "+WebConfig.MAX_PLAYERS+" players, one shared world.");
    }
    private static void json(HttpServerExchange e,Object data){e.getResponseHeaders().put(Headers.CONTENT_TYPE,"application/json; charset=utf-8");e.getResponseSender().send(GameWorld.JSON.toJson(data));}
}
