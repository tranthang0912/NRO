import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import com.google.gson.*;
public class LoadTest {
    public static void main(String[] args)throws Exception{
        int seconds=args.length==0?35:Integer.parseInt(args[0]);String suffix=String.valueOf(System.currentTimeMillis()).substring(7);String password="QA-"+UUID.randomUUID();
        List<SmokeTest.Bot> bots=new ArrayList<>();
        try{
            for(int i=0;i<5;i++){
                var bot=new SmokeTest.Bot();bots.add(bot);bot.waitFor("hello",5);
                bot.send(Map.of("type","register","username","ld"+suffix+i,"password",password,"name","ld"+suffix+i,"gender",0,"invite",""));
                long id=bot.waitFor("joined",15).get("id").getAsLong();
                var start=bot.waitFor("map",5);
                var home=SmokeTest.walkThrough(bot,id,start,21);
                SmokeTest.walkThrough(bot,id,home,0);
            }
            for(var b:bots){b.queue.clear();b.measuring=true;}
            for(int t=0;t<seconds;t++){for(var b:bots)b.send(Map.of("type","move","x",t%2==0?1:-1,"y",0,"jump",false));Thread.sleep(1000);}
            List<Long> gaps=new ArrayList<>();long bytes=0;for(var b:bots){b.send(Map.of("type","move","x",0,"y",0,"jump",false));b.measuring=false;gaps.addAll(b.gaps);bytes+=b.receivedBytes;b.queue.clear();var state=b.waitFor("state",5);if(state.get("online").getAsInt()!=5||state.get("map").getAsInt()!=0||state.getAsJsonArray("players").size()<5)throw new AssertionError("A session disconnected during load test");}
            Collections.sort(gaps);long max=gaps.get(gaps.size()-1),p95=gaps.get((int)(gaps.size()*.95));
            if(max>600)throw new AssertionError("Snapshot stalled: "+max+" ms");
            String environment=System.getenv().getOrDefault("NRO_TEST_ENV", SmokeTest.BASE);
            var metrics=Map.of("sessions",5,"durationSeconds",seconds,"snapshotSamples",gaps.size(),"p95FrameGapMs",p95,"maxFrameGapMs",max,"totalOutboundKiBPerSecond",bytes/1024.0/seconds,"disconnects",0,"environment",environment);
            Files.createDirectories(Path.of(".local"));Files.writeString(Path.of(".local/load-metrics.json"),new GsonBuilder().setPrettyPrinting().create().toJson(metrics));
            System.out.println("PASS: "+metrics);
        }finally{for(var b:bots)try{b.close();}catch(Exception ignored){}}
    }
}
