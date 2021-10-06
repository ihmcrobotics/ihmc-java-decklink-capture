package us.ihmc.javadecklink;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

import us.ihmc.tools.nativelibraries.NativeLibraryLoader;

public class Capture
{
   public static final String LIBAV_VERSION = "IHMC_JAVA_DECKLINK_LIBAV_VERSION";

   public enum CodecID
   {
      AV_CODEC_ID_MJPEG(1), AV_CODEC_ID_H264(2);

      private int id;

      private CodecID(int id)
      {
         this.id = id;
      }
   }

   static private boolean loaded = false;
   private static final String LIBAV_SUPPORTED_VERSIONS[] = {"-desktopvideo10.8.5-avcodec56-swscale3-avformat56-ffmpeg",
         "-desktopvideo11.2-avcodec57-swscale4-avformat57", "-desktopvideo12.1-avcodec56-swscale3-avformat56-ffmpeg"};
   private final ReentrantLock lock = new ReentrantLock();
   static
   {
      String userVersion = System.getenv(LIBAV_VERSION);
      if (userVersion != null)
      {
         for (String version : LIBAV_SUPPORTED_VERSIONS)
         {
            if (version.startsWith(userVersion))
            {
               try
               {
                  System.out.println("[INFO] Trying to load JavaDecklink version " + version);
                  NativeLibraryLoader.loadLibrary("us.ihmc.javadecklink.lib", "JavaDecklink" + version);
                  loaded = true;
               }
               catch (UnsatisfiedLinkError e)
               {
                  System.err.println("[WARNING] Cannot load JavaDecklink version " + version + ".");
               }
               break;
            }

         }
      }
      else
      {
         System.out.println("[INFO] Environment variable " + LIBAV_VERSION + " not set. Set it to one of the LIBAV_SUPPORTED_VERSIONS in "
               + Capture.class.getName() + " to force the library version.");
      }

      if (!loaded)
      {
         for (String version : LIBAV_SUPPORTED_VERSIONS)
         {
            try
            {
               System.out.println("[INFO] Trying to load JavaDecklink version " + version);
               NativeLibraryLoader.loadLibrary("us.ihmc.javadecklink.lib", "JavaDecklink" + version);
               loaded = true;
               break;
            }
            catch (UnsatisfiedLinkError e)
            {
               System.err.println("[WARNING] Cannot load JavaDecklink version " + version + ".");
            }
         }
      }
   }

   private native long getHardwareTime(long ptr);

   private native long startCaptureNative(String filename, String format, int decklink, long captureSettings);

   private native long startCaptureNativeWithAudio(String filename, String format, int decklink, long captureSettings);

   private native void stopCaptureNative(long ptr);

   private native long createCaptureSettings(int codec);

   private native long setQuality(long captureSettings, int quality);

   private native void setOption(long captureSettings, String option, String value);

   private final CodecID codec;
   private final CaptureHandler captureHandler;
   private final long captureSettingsPtr;
   private long ptr = 0;
   private boolean alive = true;
   private String format = null;

   private boolean recordAudio = false;

   public Capture(CaptureHandler captureHandler, CodecID codec)
   {
      if (!loaded)
      {
         throw new UnsatisfiedLinkError("[ERROR] Cannot load JavaDecklink library, make sure you have a supported libav version installed. Supported versions are "
               + Arrays.toString(LIBAV_SUPPORTED_VERSIONS));
      }

      this.codec = codec;
      this.captureHandler = captureHandler;
      this.captureSettingsPtr = createCaptureSettings(codec.id);

   }

   /**
    * Enable recording audio in MP3 format
    * <p>
    * This functionality is experimental.
    * </p>
    * <p>
    * Audio settings: 128kbps 44.1kHz 2 channel
    * </p>
    * 
    * @param recordAudio set to true to record audio.
    */
   public void setRecordAudio(boolean recordAudio)
   {
      this.recordAudio = recordAudio;
   }

   /**
    * @param option
    * @param value
    */
   public void setOption(String option, String value)
   {
      setOption(captureSettingsPtr, option, value);
   }

   public void setFormat(String formatShortName)
   {
      this.format = formatShortName;
   }

   public void setMJPEGQuality(double quality)
   {
      quality = Math.min(1, Math.max(quality, 0));

      int codecQuality;
      switch (codec)
      {
         case AV_CODEC_ID_H264:
            throw new RuntimeException("Quality settings not supported");
         case AV_CODEC_ID_MJPEG:
            codecQuality = 2 + ((int) ((1.0 - quality) * 30));
            break;
         default:
            throw new RuntimeException();
      }

      setQuality(captureSettingsPtr, codecQuality);
   }

   public long getHardwareTime()
   {
      if (lock.tryLock())
      {
         if (!alive || ptr == 0)
         {
            return -1;
         }

         long hardwareTime = getHardwareTime(ptr);
         lock.unlock();
         return hardwareTime;
      }
      return -1;
   }

   private void receivedFrameAtHardwareTimeFromNative(long hardwareTime, long pts, long timeScaleNumerator, long timeScaleDenumerator)
   {
      captureHandler.receivedFrameAtTime(hardwareTime, pts, timeScaleNumerator, timeScaleDenumerator);
   }

   public void startCapture(String filename, int decklink) throws IOException
   {
      if (!alive)
      {
         throw new RuntimeException("This Capture interface has been stopped");
      }
      if (ptr != 0)
      {
         throw new IOException("Capture already started");
      }

      if (recordAudio)
      {
         ptr = startCaptureNativeWithAudio(filename, format, decklink, captureSettingsPtr);
      }
      else
      {
         ptr = startCaptureNative(filename, format, decklink, captureSettingsPtr);
      }
      if (ptr == 0)
      {
         throw new IOException("Cannot open capture card");
      }
   }

   private void stopCaptureFromNative()
   {
      try
      {
         stopCapture();
      }
      catch (IOException e)
      {
      }
   }

   public void stopCapture() throws IOException
   {
      if (!alive)
      {
         return; // Already stopped
      }
      if (ptr == 0)
      {
         throw new IOException("Capture not started");
      }
      lock.lock();
      {
         stopCaptureNative(ptr);
         alive = false;
         ptr = 0;
      }
      lock.unlock();
   }

   /**
    * Performs a video capture test. Allows to check which card ID maps to which camera. To see
    * options, run with the argument "--help".
    */
   public static void main(String[] args) throws IOException, InterruptedException, JSAPException
   {
      SimpleJSAP jsap = new SimpleJSAP("Capture test",
                                       "Test capture of one or more video capture card",
                                       new Parameter[] {
                                             new FlaggedOption("codec",
                                                               JSAP.STRING_PARSER,
                                                               "MJPEG",
                                                               JSAP.NOT_REQUIRED,
                                                               'c',
                                                               "codec",
                                                               "Codec either: H264 or MJPEG"),
                                             new FlaggedOption("outputPath",
                                                               JSAP.STRING_PARSER,
                                                               ".",
                                                               JSAP.NOT_REQUIRED,
                                                               'p',
                                                               "path",
                                                               "Path to directory where video file(s) are to be saved"),
                                             new FlaggedOption("videoQuality",
                                                               JSAP.DOUBLE_PARSER,
                                                               String.valueOf(0.85),
                                                               JSAP.NOT_REQUIRED,
                                                               'q',
                                                               "quality",
                                                               "Video quality for MJPEG"),
                                             new FlaggedOption("crf",
                                                               JSAP.INTEGER_PARSER,
                                                               String.valueOf(23),
                                                               JSAP.NOT_REQUIRED,
                                                               'r',
                                                               "crf",
                                                               "CRF (Constant rate factor) for H264. 0-51, 0 is lossless. Sane values are 18 to 28."),
                                             new FlaggedOption("captureDuration",
                                                               JSAP.INTEGER_PARSER,
                                                               "5000",
                                                               JSAP.NOT_REQUIRED,
                                                               'd',
                                                               "duration",
                                                               "Capture duration in milliseconds for each capture card"),
                                             new FlaggedOption("firstDecklinkId",
                                                               JSAP.INTEGER_PARSER,
                                                               String.valueOf(0),
                                                               JSAP.NOT_REQUIRED,
                                                               'f',
                                                               "firstId",
                                                               "ID of the first capture card to test"),
                                             new FlaggedOption("lastDecklinkId",
                                                               JSAP.INTEGER_PARSER,
                                                               String.valueOf(-1),
                                                               JSAP.NOT_REQUIRED,
                                                               'l',
                                                               "lastId",
                                                               "ID of the last capture card to test")});
      JSAPResult config = jsap.parse(args);
      if (jsap.messagePrinted())
      {
         System.out.println(jsap.getUsage());
         System.out.println(jsap.getHelp());
         System.exit(-1);
      }

      int firstId = config.getInt("firstDecklinkId");
      int lastId = config.getInt("lastDecklinkId");
      if (lastId < firstId)
         lastId = firstId;
      String outputPath = config.getString("outputPath");
      int duration = config.getInt("captureDuration");
      CodecID codec = config.getString("codec").contains("264") ? CodecID.AV_CODEC_ID_H264 : CodecID.AV_CODEC_ID_MJPEG;

      for (int id = firstId; id <= lastId; id++)
      {
         try
         {
            CaptureHandlerImpl captureHandlerImpl = new CaptureHandlerImpl();
            final Capture capture;

            switch (codec)
            {
               case AV_CODEC_ID_H264:
                  capture = new Capture(captureHandlerImpl, CodecID.AV_CODEC_ID_H264);
                  capture.setOption("g", "1");
                  capture.setOption("crf", String.valueOf(config.getInt("crf")));
                  capture.setOption("profile", "high");
                  capture.setOption("coder", "vlc");
                  break;
               case AV_CODEC_ID_MJPEG:
                  capture = new Capture(captureHandlerImpl, CodecID.AV_CODEC_ID_MJPEG);
                  capture.setMJPEGQuality(config.getDouble("videoQuality"));
                  break;
               default:
                  throw new RuntimeException();
            }
            captureHandlerImpl.setCapture(capture);
            capture.startCapture(outputPath + "capture" + id + ".mp4", id);

            try
            {
               Thread.sleep(duration);
            }
            catch (InterruptedException e)
            {
               break;
            }
            finally
            {
               capture.stopCapture();
            }
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
   }

   private static class CaptureHandlerImpl implements CaptureHandler
   {

      @Override
      public void receivedFrameAtTime(long hardwareTime, long pts, long timeScaleNumerator, long timeScaleDenumerator)
      {
         long currentTime = capture.getHardwareTime();
         System.out.println("Received frame at " + hardwareTime + ", current time: " + currentTime + ", delay: " + (currentTime - hardwareTime) + ",  pts: "
               + pts + ", timescale: " + timeScaleNumerator + "/" + timeScaleDenumerator);

      }

      public void setCapture(Capture capture)
      {
         this.capture = capture;
      }

      private Capture capture;

   }
}
