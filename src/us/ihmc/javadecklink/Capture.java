package us.ihmc.javadecklink;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

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

   public static void main(String[] args) throws IOException, InterruptedException
   {
      CaptureHandlerImpl captureHandlerImpl = new CaptureHandlerImpl();
      final Capture capture;
      if (args.length > 0 && args[0].contains("264"))
      {
         capture = new Capture(captureHandlerImpl, CodecID.AV_CODEC_ID_H264);
         //	capture.setMJPEGQuality(0.9);
         capture.setOption("preset", "medium");
         capture.setOption("g", "1");
         capture.setOption("crf", "25");
      }
      else
      {
         capture = new Capture(captureHandlerImpl, CodecID.AV_CODEC_ID_MJPEG);
         capture.setMJPEGQuality(0.9);
      }
      captureHandlerImpl.setCapture(capture);
      capture.startCapture("aap.mp4", 1);

      Thread.sleep(5000);
      capture.stopCapture();
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
