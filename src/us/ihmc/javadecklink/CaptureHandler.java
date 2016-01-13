package us.ihmc.javadecklink;

import java.nio.ByteBuffer;

public interface CaptureHandler
{
   public void receivedFrame(int width, int height, int rowBytes, ByteBuffer data);
   
   public void receivedInvalidFrame();
}
