package us.ihmc.javadecklink;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

import us.ihmc.javadecklink.Capture.CodecID;

public class Stream
{
   private final Capture capture;
   
   public Stream(int card, String videoURL, Map<String, String> options) throws IOException
   {
      System.out.println("Streaming Blackmagic card " + card + " to " + videoURL);
      
      CaptureHandlerImpl captureHandlerImpl = new CaptureHandlerImpl();
      capture = new Capture(captureHandlerImpl, CodecID.AV_CODEC_ID_H264);
      
      for(Entry<String, String> entry : options.entrySet())
      {
         capture.setOption(entry.getKey(), entry.getValue());
      }
      capture.setRecordAudio(true);
      capture.setOption("g", "120");
      capture.setFormat("flv");
      capture.startCapture(videoURL, card);
   }
   
   public void stop() throws IOException
   {
      capture.stopCapture();
     
   }
   
   public static void main(String[] args) throws IOException, InterruptedException, JSAPException
   {
      
      SimpleJSAP jsap = new SimpleJSAP("Stream", "Stream Decklink capture card to streaming services", new Parameter[] {
            new FlaggedOption("preset", JSAP.STRING_PARSER, "veryfast", JSAP.NOT_REQUIRED,  JSAP.NO_SHORTFLAG, "preset",
                  "H264 preset (ultrafast,superfast, veryfast, faster, fast, medium, slow, slower, veryslow)"),
            new FlaggedOption("profile", JSAP.STRING_PARSER, "high", JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, "profile",
                  "H264 profile (baseline, main, high, high10, high422, high444)"),
            new FlaggedOption("crf", JSAP.INTEGER_PARSER, "26", JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, "crf",
                  "Constant rate factor. 0-51. 26 default, useful range 18-28"),
            new FlaggedOption("card", JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'c', "card", "Capture card to use"), 
            new UnflaggedOption("videoURL", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, false, "URL to stream the video to")
      });
      JSAPResult config = jsap.parse(args);
      if (jsap.messagePrinted())
      {
         System.out.println(jsap.getUsage());
         System.out.println(jsap.getHelp());
         System.exit(-1);
      }

      HashMap<String, String> options = new HashMap<>();
      options.put("preset", config.getString("preset"));
      options.put("profile", config.getString("profile"));
      options.put("crf", String.valueOf(config.getInt("crf")));
      
      Stream stream = new Stream(config.getInt("card"), config.getString("videoURL"), options);
      Runtime.getRuntime().addShutdownHook(new CaptureShutdownHook(stream));
      
      Thread.currentThread().join();
   }

   private static class CaptureShutdownHook extends Thread
   {
      private final Stream stream;
      
      public CaptureShutdownHook(Stream stream)
      {
         this.stream = stream;
      }
      
      @Override
      public void run()
      {
         try
         {
            stream.stop();
         }
         catch (IOException e)
         {
            e.printStackTrace();
         }
      }
   }
   
   private class CaptureHandlerImpl implements CaptureHandler
   {
      private int frame = 0;
      @Override
      public void receivedFrameAtTime(long hardwareTime, long pts, long timeScaleNumerator, long timeScaleDenumerator)
      {
         if(frame % 120 == 0)
         {
            System.out.println("[Streaming] Published frame " + frame + " at time " + hardwareTime + "ns. pts: " + pts);
         }
         ++frame;
      }
      
   }
}
