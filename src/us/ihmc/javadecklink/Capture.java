package us.ihmc.javadecklink;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import us.ihmc.tools.nativelibraries.NativeLibraryLoader;

public class Capture
{
   private static final int LIBAV_SUPPORTED_VERSIONS[] = { 54, 56 };
   private final ReentrantLock lock = new ReentrantLock();
   static
   {
      boolean loaded = false;

      for(int version : LIBAV_SUPPORTED_VERSIONS)
      {
         try
         {
            System.out.println("[INFO] Trying to load JavaDecklink version " + version);
            NativeLibraryLoader.loadLibrary("us.ihmc.javadecklink.lib", "JavaDecklink" + version);
            loaded = true;
            break;
         }
         catch(UnsatisfiedLinkError e)
         {
            System.err.println("[WARNING] Cannot load JavaDecklink version " + version + ".");
         }         
      }
      if(!loaded)
      {
         throw new UnsatisfiedLinkError("[ERROR] Cannot load JavaDecklink library, make sure you have a supported libav version installed. Supported versions are " + Arrays.toString(LIBAV_SUPPORTED_VERSIONS));
      }
   }

   private native long getHardwareTime(long ptr);
   private native long startCaptureNative(String filename, int decklink, int quality);
   private native void stopCaptureNative(long ptr);

   private final CaptureHandler captureHandler;
   
   private long ptr = 0;   
   private boolean alive = true;

   
   public Capture(CaptureHandler captureHandler)
   {
      this.captureHandler = captureHandler;
            
   }
   
   public long getHardwareTime()
   {
      if(lock.tryLock())
      {
         if(!alive || ptr == 0)
         {
            return -1;
         }
         
         long hardwareTime = getHardwareTime(ptr);
         lock.unlock();
         return hardwareTime;
      }
      return -1;
   }
   
   private void receivedFrameAtHardwareTimeFromNative(long hardwareTime, long pts)
   {
      captureHandler.receivedFrameAtTime(hardwareTime, pts);
   }

   public void startCapture(String filename, int decklink, double quality) throws IOException
   {
      if(!alive)
      {
         throw new RuntimeException("This Capture interface has been stopped");
      }
      if (ptr != 0)
      {
         throw new IOException("Capture already started");
      }
      
      quality = Math.min(1, Math.max(quality, 0));
      
      int mjpegQuality = 2 + ((int)((1.0 - quality) * 30));
      
      ptr = startCaptureNative(filename, decklink, mjpegQuality);
      if (ptr == 0)
      {
         throw new IOException("Cannot open capture card");
      }
   }

   public void stopCapture() throws IOException
   {
      if (ptr == 0)
      {
         throw new IOException("Capture not started");
      }
      lock.lock();
      {
         stopCaptureNative(ptr);
         alive = false;
      }
      lock.unlock();
   }

   public static void main(String[] args) throws IOException, InterruptedException
   {
      CaptureHandlerImpl captureHandlerImpl = new CaptureHandlerImpl();
      final Capture capture = new Capture(captureHandlerImpl);
      captureHandlerImpl.setCapture(capture);
      capture.startCapture("aap.mp4", 1, 0.9);

      Thread.sleep(5000);
      capture.stopCapture();
   }
   
   private static class CaptureHandlerImpl implements CaptureHandler
   {

      @Override
      public void receivedFrameAtTime(long hardwareTime, long pts)
      {
         long currentTime = capture.getHardwareTime();
         System.out.println("Received frame at " + hardwareTime + ", current time: " + currentTime + ", delay: " + (currentTime - hardwareTime) + ",  pts: " + pts);
         
      }
      public void setCapture(Capture capture)
      {
         this.capture = capture;
      }
      private Capture capture;
      
      
   }
}
