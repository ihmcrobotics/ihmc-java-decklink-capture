package us.ihmc.javadecklink;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import us.ihmc.codecs.builder.MP4MJPEGMovieBuilder;
import us.ihmc.codecs.builder.MovieBuilder;
import us.ihmc.codecs.generated.YUVPicture;
import us.ihmc.codecs.generated.YUVPicture.YUVSubsamplingType;

public class MJPEGEncoder implements CaptureHandler
{
   private MovieBuilder builder;
   
   public MJPEGEncoder()
   {
      
   }

   @Override
   public void receivedFrame(int width, int height, int rowBytes, ByteBuffer data)
   {
      if(builder == null)
      {
         try
         {
            builder = new MP4MJPEGMovieBuilder(new File("test.mp4"), width, height, 60, 95);
         }
         catch (IOException e)
         {
            e.printStackTrace();
            return;
         }
      }
      
      ByteBuffer Y = ByteBuffer.allocateDirect(width * height);
      ByteBuffer U = ByteBuffer.allocateDirect(width/2 * height);
      ByteBuffer V = ByteBuffer.allocateDirect(width/2 * height);
      
      YUVPicture picture = new YUVPicture(YUVSubsamplingType.YUV422, width, height, width, width/2, width/2, Y, U, V);
      
      
      while(data.hasRemaining())
      {
         U.put(data.get());
         Y.put(data.get());
         V.put(data.get());
         Y.put(data.get());
      }
      
      System.out.println(data);
      
      try
      {
         builder.encodeFrame(picture);
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
      
      
   }

   @Override
   public void receivedInvalidFrame()
   {
      // TODO Auto-generated method stub
      
   }

}
