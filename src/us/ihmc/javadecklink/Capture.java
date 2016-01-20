package us.ihmc.javadecklink;

import java.io.IOException;

import us.ihmc.tools.nativelibraries.NativeLibraryLoader;

public class Capture
{
   static
   {
      NativeLibraryLoader.loadLibrary("us.ihmc.javadecklink.lib", "JavaDecklink");
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
      if(!alive)
      {
         throw new RuntimeException("This Capture interface has been stopped");
      }
      if (ptr == 0)
      {
         return -1;
      }
      
      return getHardwareTime(ptr);
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

      stopCaptureNative(ptr);
      alive = false;
   }

   public static void main(String[] args) throws IOException, InterruptedException
   {
      final Capture capture = new Capture(new CaptureHandler()
      {
         
         @Override
         public void receivedFrameAtTime(long hardwareTime, long pts)
         {
            System.out.println("Received frame at " + hardwareTime + ", pts: " + pts);
         }
      });

      capture.startCapture("aap.mp4", 1, 0.9);

      Thread.sleep(5000);
      capture.stopCapture();
   }
}
