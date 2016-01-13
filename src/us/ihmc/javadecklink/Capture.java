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
   
   public Capture()
   {
      
   }
   
   private void receivedFrameFromNative(boolean valid, int width, int height, int rowBytes, ByteBuffer dataBuffer)
   {
      System.out.println("Received" + (!valid?"in":"") + " valid frame. " + width + "x" + height + ". Size: " + rowBytes * height);
      System.out.println(dataBuffer);
      
   }
   
   public void startCapture(int decklink, int mode) throws IOException
   {
      if(ptr != 0)
      {
         throw new IOException("Capture already started");
      }
      ptr = startCaptureNative(decklink, mode);
      if(ptr == 0)
      {
         throw new IOException("Cannot open capture card");
      }
   }
   
   public void stopCapture() throws IOException
   {
      if(ptr != 0)
      {
         throw new IOException("Capture not started");
      }
      
      stopCaptureNative(ptr);
   }
   
   public static void main(String[] args) throws IOException, InterruptedException
   {
      Capture capture = new Capture();
      
      capture.startCapture(1, 9);
      
      while(true)
      {
         Thread.sleep(1000);
      }
   }
}
