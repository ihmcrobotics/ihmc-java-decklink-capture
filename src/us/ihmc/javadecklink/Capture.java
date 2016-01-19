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
   private boolean alive = true;

   public Capture()
   {
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
      }
   }

   public static void main(String[] args) throws IOException, InterruptedException
   {
      final Capture capture = new Capture();

      capture.startCapture(1, 9);

      Thread.sleep(5000);
      capture.stopCapture();
   }
}
