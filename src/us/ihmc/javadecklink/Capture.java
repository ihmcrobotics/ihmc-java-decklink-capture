package us.ihmc.javadecklink;

import java.io.IOException;

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
   
   private void receivedFrameFromNative(boolean valid, int width, int height, int rowBytes)
   {
      System.out.println("Received" + (!valid?"in":"") + "valid frame. " + height + "x" + width + ". Size: " + rowBytes * height);
      
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
      
      capture.startCapture(0, 14);
      
      while(true)
      {
         Thread.sleep(1000);
      }
   }
}
