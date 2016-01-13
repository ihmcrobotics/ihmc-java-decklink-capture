package us.ihmc.javadecklink;

import java.io.IOException;
import java.nio.ByteBuffer;

import us.ihmc.tools.nativelibraries.NativeLibraryLoader;

public class Capture
{
   static
   {
      NativeLibraryLoader.loadLibrary("us.ihmc.javadecklink.lib", "JavaDecklink");
   }

   private native long startCaptureNative(int decklink, int mode);

   private native void stopCaptureNative(long ptr);

   private long ptr = 0;

   private final CaptureHandler handler;
   
   private boolean alive = true;

   public Capture(CaptureHandler handler)
   {
      this.handler = handler;
   }

   private synchronized void receivedFrameFromNative(boolean valid, int width, int height, int rowBytes, ByteBuffer dataBuffer)
   {
      if(alive)
      {
         if (valid)
         {
            handler.receivedFrame(width, height, rowBytes, dataBuffer);
         }
         else
         {
            handler.receivedInvalidFrame();
         }
      }
   }

   public synchronized void startCapture(int decklink, int mode) throws IOException
   {
      if(!alive)
      {
         throw new RuntimeException("This Capture interface has been stopped");
      }
      if (ptr != 0)
      {
         throw new IOException("Capture already started");
      }
      ptr = startCaptureNative(decklink, mode);
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
      synchronized(this)
      {
         alive = false;
         handler.stop();
      }
   }

   public static void main(String[] args) throws IOException, InterruptedException
   {
      final Capture capture = new Capture(new MJPEGEncoder());

      capture.startCapture(1, 9);

      Thread.sleep(5000);
      capture.stopCapture();
   }
}
