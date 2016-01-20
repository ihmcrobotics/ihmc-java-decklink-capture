package us.ihmc.javadecklink;

public interface CaptureHandler
{
   public void receivedFrameAtTime(long hardwareTime, long pts);
}
