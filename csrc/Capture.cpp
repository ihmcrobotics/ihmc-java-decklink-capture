/* -LICENSE-START-
** Copyright (c) 2013 Blackmagic Design
**
** Permission is hereby granted, free of charge, to any person or organization
** obtaining a copy of the software and accompanying documentation covered by
** this license (the "Software") to use, reproduce, display, distribute,
** execute, and transmit the Software, and to prepare derivative works of the
** Software, and to permit third-parties to whom the Software is furnished to
** do so, all subject to the following:
**
** The copyright notices in the Software and this entire statement, including
** the above license grant, this restriction and the following disclaimer,
** must be included in all copies of the Software, in whole or in part, and
** all derivative works of the Software, unless such copies or derivative
** works are solely in the form of machine-executable object code generated by
** a source language processor.
**
** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
** FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT
** SHALL THE COPYRIGHT HOLDERS OR ANYONE DISTRIBUTING THE SOFTWARE BE LIABLE
** FOR ANY DAMAGES OR OTHER LIABILITY, WHETHER IN CONTRACT, TORT OR OTHERWISE,
** ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
** DEALINGS IN THE SOFTWARE.
** -LICENSE-END-
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <unistd.h>
#include <fcntl.h>
#include <csignal>
#include <jni.h>
#include <string>



#include "DeckLinkAPI.h"
#include "Capture.h"
#include "Util.hpp"
#include "us_ihmc_javadecklink_Capture.h"

#include <time.h>



class ThreadJNIEnv {
public:
    DeckLinkCaptureDelegate *delegate;
    JNIEnv *env;

    ThreadJNIEnv(DeckLinkCaptureDelegate* delegate) :
        delegate(delegate)
    {
        std::cout << "Attaching thread" << std::endl;
        delegate->vm->AttachCurrentThread((void **) &env, NULL);
    }

    ~ThreadJNIEnv() {
        std::cout << "Finalizing capture" << std::endl;
        env->DeleteGlobalRef(delegate->obj);
        JavaVM* vm = delegate->vm;
        delete delegate;
        vm->DetachCurrentThread();

    }
};

static boost::thread_specific_ptr<ThreadJNIEnv> envs;
boost::mutex encoderMutex;

inline JNIEnv* registerDecklinkDelegate(DeckLinkCaptureDelegate* delegate)
{
    ThreadJNIEnv *ret = envs.get();
    if(ret)
    {
        return ret->env;
    }
    else
    {
        ret = new ThreadJNIEnv(delegate);
        envs.reset(ret);
        return ret->env;
    }
}


DeckLinkCaptureDelegate::DeckLinkCaptureDelegate(std::string filename, std::string format, bool recordAudio, DecklinkCaptureSettings* settings, IDeckLink* decklink, IDeckLinkInput* decklinkInput, JavaVM* vm, jobject obj, jmethodID methodID, jmethodID stop) :
    vm(vm),
    obj(obj),
    valid(true),
    m_refCount(1),
    decklink(decklink),
    decklinkInput(decklinkInput),
    methodID(methodID),
    stop(stop),
    initial_video_pts(AV_NOPTS_VALUE),
    record_audio(recordAudio),
    initial_audio_pts(AV_NOPTS_VALUE),
    audioSampleDepth(16),
    audioChannels(2),
    settings(settings)
{
    av_register_all();
    avcodec_register_all();

    oc = avformat_alloc_context();
    
    if(format.empty())
    {
    	oc->oformat = av_guess_format(NULL, filename.c_str(), NULL);
	}
	else
	{
		oc->oformat = av_guess_format(format.c_str(), NULL, NULL);
	}
	


    if(oc->oformat == NULL)
    {
        fprintf(stderr, "AV Format %s for %s not found\n", format.c_str(), filename.c_str());
        valid = false;
    }
    else
    {
        oc->oformat->video_codec = settings->codec;
        snprintf(oc->filename, sizeof(oc->filename), "%s", filename.c_str());
        
        if(record_audio)
        {
    		oc->oformat->audio_codec = AV_CODEC_ID_MP3; 
        }
        else
        {
        	oc->oformat->audio_codec = AV_CODEC_ID_NONE;        	
        }
    }
}

ULONG DeckLinkCaptureDelegate::AddRef(void)
{
	return __sync_add_and_fetch(&m_refCount, 1);
}

ULONG DeckLinkCaptureDelegate::Release(void)
{
	int32_t newRefValue = __sync_sub_and_fetch(&m_refCount, 1);
	if (newRefValue == 0)
	{
        delete this;
		return 0;
	}
	return newRefValue;
}

HRESULT DeckLinkCaptureDelegate::VideoInputFrameArrived(IDeckLinkVideoInputFrame* videoFrame, IDeckLinkAudioInputPacket* audioFrame)
{
    void* frameBytes;


    JNIEnv* env = registerDecklinkDelegate(this);
    if(env == 0)
    {
        // Cannot throw a runtime exception because we don't have an env
        std::cerr << "Cannot load env" << std::endl;
        return S_OK;
    }

	// Handle Video Frame
    if (videoFrame)
    {


		if (videoFrame->GetFlags() & bmdFrameHasNoInputSource)
		{
            printf("Frame received - No input signal detected\n");
			return S_OK;
            //env->CallVoidMethod(obj, methodID, false, 0, 0, NULL);
		}
		else
        {
				videoFrame->GetBytes(&frameBytes);
                //write(g_videoOutputFile, frameBytes, videoFrame->GetRowBytes() * videoFrame->GetHeight());


                av_init_packet(&pkt);
                pkt.data = NULL;    // packet data will be allocated by the encoder
                pkt.size = 0;

                BMDTimeValue frameTime;
                BMDTimeValue frameDuration;
                videoFrame->GetStreamTime(&frameTime, &frameDuration, video_st->time_base.den);
                int64_t pts;
                pts = frameTime / video_st->time_base.num;




                if (initial_video_pts == AV_NOPTS_VALUE) {
                    initial_video_pts = pts;
                }

                pts -= initial_video_pts;

                pictureUYVY->pts = pts;
                pictureYUV420->pts = pts;
                pkt.pts = pkt.dts = pts;

                avpicture_fill((AVPicture*)pictureUYVY, (uint8_t*) frameBytes, AV_PIX_FMT_UYVY422, pictureUYVY->width, pictureUYVY->height);
                sws_scale(img_convert_ctx, pictureUYVY->data, pictureUYVY->linesize, 0, c->height, pictureYUV420->data, pictureYUV420->linesize);


				if(settings->quality >= 0)
				{
	                pictureYUV420->quality = settings->quality;				
				}
                /* encode the image */
                int got_output;
                int ret = avcodec_encode_video2(c, &pkt, pictureYUV420, &got_output);
                if (ret < 0) {
                    fprintf(stderr, "error encoding frame\n");
                }
                else if (got_output) {
                	pkt.stream_index = video_st->index;
                    if(av_interleaved_write_frame(oc, &pkt) != 0)
                    {
                    	fprintf(stderr, "Error while writing video frame\n");
                    }
                    av_free_packet(&pkt); //depreacted, use av_packet_unref(&pkt); after Ubuntu 16.04 comes out


                    videoFrame->GetHardwareReferenceTimestamp(1000000000, &frameTime, &frameDuration);
                    env->CallVoidMethod(obj, methodID, frameTime, pts, (jlong) video_st->time_base.num, (jlong) video_st->time_base.den);
                }


		}
	}
	
	if(record_audio && audioFrame)
	{
		int swret;
		void* audioBytes;
	
		AVFrame *frame = av_frame_alloc();
	
		audioFrame->GetBytes(&audioBytes);
        BMDTimeValue audio_pts;
		audioFrame->GetPacketTime(&audio_pts, audioContext->time_base.den);
        int64_t pts;
        pts = audio_pts / audioContext->time_base.num;
        
        if (initial_audio_pts == AV_NOPTS_VALUE) {
		    initial_audio_pts = pts;
		}
		
		pts -= initial_audio_pts;
		
		
		
        int out_num_samples = swr_get_out_samples(resample_ctx, audioFrame->GetSampleFrameCount());
		
		if(out_num_samples > max_out_num_samples)
		{	
			if(resampleBuffer == NULL)
			{
				swret = av_samples_alloc_array_and_samples(&resampleBuffer, &out_linesize, audioChannels, out_num_samples, audioContext->sample_fmt, 0);			
			}
			else
			{
				av_freep(&resampleBuffer[0]);
				swret = av_samples_alloc(resampleBuffer, &out_linesize, audioChannels, out_num_samples, audioContext->sample_fmt, 0);
			}
			
			if(swret < 0)
			{
				fprintf(stderr, "error allocating resampling buffer\n");
				return S_OK;	
			}
			
			max_out_num_samples = out_num_samples;
			
		}
		
		
		swret = swr_convert(resample_ctx, resampleBuffer, out_num_samples, (const uint8_t**) &audioBytes, audioFrame->GetSampleFrameCount());
		if(swret < 0)
		{
			fprintf(stderr, "error resampling audio\n");
			av_freep(&resampleBuffer[0]);
			return S_OK;
		}
		
		int destSize = av_samples_get_buffer_size(&out_linesize, audioChannels, swret, audioContext->sample_fmt, 0);
		
		
		
        av_init_packet(&audioPkt);
        audioPkt.data = NULL;    // packet data will be allocated by the encoder
        audioPkt.size = 0;
        


		frame->nb_samples = swret;
		frame->format = audioContext->sample_fmt;
		frame->channel_layout = audioContext->channel_layout;
		frame->channels = audioContext->channels;
		frame->sample_rate = audioContext->sample_rate;
		frame->pts = pts;
		avcodec_fill_audio_frame(frame, frame->channels, (AVSampleFormat) frame->format, resampleBuffer[0], destSize, 0);
		int got_output;
		int ret = avcodec_encode_audio2(audioContext, &audioPkt, frame, &got_output);
		if (ret < 0) {
            fprintf(stderr, "error encoding audio frame\n");
        }
        else if (got_output) {
        	av_packet_rescale_ts(&audioPkt, audioContext->time_base, audio_st->time_base);
        	audioPkt.stream_index = audio_st->index;
        	if(av_interleaved_write_frame(oc, &audioPkt) != 0)
        	{
        		fprintf(stderr, "Error while writing audio frame\n");
        	}
        	av_free_packet(&audioPkt);
        }
		av_frame_free(&frame);	
	}



	return S_OK;
}

HRESULT DeckLinkCaptureDelegate::VideoInputFormatChanged(BMDVideoInputFormatChangedEvents events, IDeckLinkDisplayMode *mode, BMDDetectedVideoInputFormatFlags formatFlags)
{
    if (!events)
    {
	    return S_OK;
    }
    if (!(events & 0) && !(events & 1) && !(events & 2))
    {
	    return S_OK;
    }

    encoderMutex.lock();

    // This only gets called if bmdVideoInputEnableFormatDetection was set
    // when enabling video input
    HRESULT	result;
    char*	displayModeName = NULL;
    BMDPixelFormat	pixelFormat = bmdFormat8BitYUV;

    JNIEnv* env = registerDecklinkDelegate(this);
    if (formatFlags & bmdDetectedVideoInputRGB444)
    {
        printf("Unsupported input format: RGB444\n");
        env->CallVoidMethod(obj, stop);
        goto bail;
    }

    mode->GetName((const char**)&displayModeName);
    printf("Video format changed to %s %s\n", displayModeName, formatFlags & bmdDetectedVideoInputRGB444 ? "RGB" : "YUV");



    if (displayModeName)
        free(displayModeName);

    if (decklinkInput)
    {
        decklinkInput->StopStreams();

        if(codec)
        {
            printf("Cannot change video resolution while capturing. Stopping capture.\n");
            env->CallVoidMethod(obj, stop);
            goto bail;
        }

	
        codec = avcodec_find_encoder(settings->codec);
	printf("Using encoder %s\n", codec->name);

        if (!codec) {
            printf("codec not found\n");
            env->CallVoidMethod(obj, stop);
            goto bail;
        }

        video_st = avformat_new_stream(oc, codec);
        if(!video_st)
        {
            printf("Cannot allocate video stream\n");
            env->CallVoidMethod(obj, stop);
            goto bail;

        }

        c = video_st->codec;

	    for (auto it = settings->options.begin() ; it != settings->options.end(); ++it)
		{
			printf("Setting %s to %s\n", it->first.c_str(), it->second.c_str());
			int error = av_opt_set(c, it->first.c_str(), it->second.c_str(), AV_OPT_SEARCH_CHILDREN);
			if(error)  	
			{
				printf("Cannot set %s to %s\n", it->first.c_str(), it->second.c_str());
	            env->CallVoidMethod(obj, stop);
	            goto bail;
			}
			
		}



        /* put sample parameters */
        /* resolution must be a multiple of two */
        c->width = mode->GetWidth();
        c->height = mode->GetHeight();
        /* frames per second */

        BMDTimeValue numerator;
        BMDTimeScale denumerator;

        mode->GetFrameRate(&numerator, &denumerator);


        c->time_base.den = denumerator;
        c->time_base.num = numerator;
        c->pix_fmt = AV_PIX_FMT_YUVJ420P;

        if(oc->oformat->flags & AVFMT_GLOBALHEADER)
        {
            c->flags |= CODEC_FLAG_GLOBAL_HEADER;
        }

		if(settings->quality >= 0)
		{
        	c->flags |= CODEC_FLAG_QSCALE;
        	c->qmin = c->qmax = settings->quality;
		}

        /* open it */
        if (avcodec_open2(c, codec, NULL) < 0) {
            printf("Could not open codec\n");
            env->CallVoidMethod(obj, stop);
            goto bail;
        }

        if(avio_open(&oc->pb, oc->filename, AVIO_FLAG_WRITE) < 0)
        {
            printf("Could not open file\n");
            env->CallVoidMethod(obj, stop);
            goto bail;

        }

        //pictureYUV420 = avcodec_alloc_frame();
        pictureYUV420 = av_frame_alloc();
       int ret = av_image_alloc(pictureYUV420->data, pictureYUV420->linesize, c->width, c->height, c->pix_fmt, 32);
        if (ret < 0) {
            printf("could not alloc raw picture buffer\n");
            env->CallVoidMethod(obj, stop);
            goto bail;

        }
        pictureYUV420->format = c->pix_fmt;
        pictureYUV420->width  = c->width;
        pictureYUV420->height = c->height;


        //pictureUYVY = avcodec_alloc_frame();
        pictureUYVY = av_frame_alloc();
        pictureUYVY->width = c->width;
        pictureUYVY->height = c->height;
        pictureUYVY->format = AV_PIX_FMT_UYVY422;


        img_convert_ctx = sws_getContext(c->width, c->height,
        AV_PIX_FMT_UYVY422,
        c->width, c->height,
        c->pix_fmt,
        sws_flags, NULL, NULL, NULL);





        
        
        if(record_audio)
        {
        	audioCodec = avcodec_find_encoder(oc->oformat->audio_codec);
        	if (!audioCodec) {
	            printf("audio codec not found\n");
	            env->CallVoidMethod(obj, stop);
	            goto bail;
	        }
	        
            audio_st = avformat_new_stream(oc, audioCodec);
		    if(!audio_st)
		    {
		        printf("Cannot allocate audio stream\n");
		        env->CallVoidMethod(obj, stop);
		        goto bail;
		
		    }
		
		    audioContext = audio_st->codec;
		    audioContext->sample_fmt = AV_SAMPLE_FMT_S16P;
		    audioContext->bit_rate = 128000;
		    audioContext->sample_rate = 44100;
		    audioContext->channels = 2;
		   	audioContext->channel_layout = av_get_default_channel_layout(audioContext->channels);
		    
	        if(oc->oformat->flags & AVFMT_GLOBALHEADER)
		    {
		        audioContext->flags |= CODEC_FLAG_GLOBAL_HEADER;
		    }

            if (avcodec_open2(audioContext, audioCodec, NULL) < 0) {
	            printf("Could not open audio codec\n");
	            env->CallVoidMethod(obj, stop);
	            goto bail;
	        }
	        
	        resample_ctx = swr_alloc_set_opts(NULL,
	        									av_get_default_channel_layout(audioContext->channels),
        										audioContext->sample_fmt,
        										audioContext->sample_rate,
        										av_get_default_channel_layout(audioChannels),
        										AV_SAMPLE_FMT_S16,
        										48000,
        										0, NULL);
			if(!resample_ctx)
			{
				printf("Could not allocate resample context\n");
	            env->CallVoidMethod(obj, stop);
	            goto bail;
			}
			
			int resample_error = swr_init(resample_ctx);
			if(resample_error < 0)
			{
				printf("Could not open resample context\n");
				swr_free(&resample_ctx);
	            env->CallVoidMethod(obj, stop);
	            goto bail;
			}
			
			

        }
        
        avformat_write_header(oc, NULL);

        
        result = decklinkInput->EnableVideoInput(mode->GetDisplayMode(), pixelFormat, bmdVideoInputFlagDefault | bmdVideoInputEnableFormatDetection);
        if (result != S_OK)
        {
            printf("Failed to switch to new video mode\n");
            env->CallVoidMethod(obj, stop);
            goto bail;

        }

        
        if(record_audio)
        {
	        result = decklinkInput->EnableAudioInput(bmdAudioSampleRate48kHz,        
	                                             getAudioSampleDepth(),
	                                             getAudioChannels());
		    if (result != S_OK)
		    {
		        fprintf(stderr, "Failed to enable audio input.\n");
		        env->CallVoidMethod(obj, stop);
		        goto bail;
		    }     
		}
        

        decklinkInput->StartStreams();
    }

    std::cout << "Detected new mode " << mode->GetWidth() << "x" << mode->GetHeight() << std::endl;

    bail:
    encoderMutex.unlock();
	return S_OK;
}

int64_t DeckLinkCaptureDelegate::getHardwareTime()
{
    if(decklinkInput)
    {
        BMDTimeValue hardwareTime;
        BMDTimeValue timeInFrame;
        BMDTimeValue ticksPerFrame;
        if(decklinkInput->GetHardwareReferenceClock(1000000000, &hardwareTime, &timeInFrame, &ticksPerFrame) == S_OK)
        {
            return (int64_t) hardwareTime;
        }
    }

    return -1;
}

void DeckLinkCaptureDelegate::Stop()
{
    printf("Stopping capture\n");
    decklinkInput->StopStreams();
    decklinkInput->DisableVideoInput();
}

DeckLinkCaptureDelegate::~DeckLinkCaptureDelegate()
{
    if(oc && oc->pb)
    {
        if(oc->pb->write_flag)
        {
            av_write_trailer(oc);
            avio_close(oc->pb);
        }
    }

    if(c != NULL)
    {
        avcodec_close(c);
        av_free(c);
    }

    if(pictureYUV420 != NULL)
    {
        av_freep(&pictureYUV420->data[0]);
        //avcodec_free_frame(&pictureYUV420);
        av_frame_free(&pictureYUV420);
    }

    if(pictureUYVY != NULL)
    {
//         avcodec_free_frame(&pictureUYVY);
        av_frame_free(&pictureUYVY);
    }


    if(video_st != NULL)
    {
        av_freep(video_st);
    }



    sws_freeContext(img_convert_ctx);
    
    if(record_audio)
    {
    	if(audioContext != NULL)
    	{	
			avcodec_close(audioContext);
			av_free(audioContext);
    	}
    	
    	if(video_st != NULL)
    	{
    		av_freep(audio_st);
    	}
    	
    	if(resampleBuffer != NULL)	
    	{
			av_freep(&resampleBuffer[0]);
			av_freep(&resampleBuffer);
    	}
    
    	swr_free(&resample_ctx);
    }
    
    
    if(oc != NULL)
    {
        av_free(oc);
    }


    if (decklinkInput != NULL)
    {
        decklinkInput->Release();
        decklinkInput = NULL;
    }

    if (decklink != NULL)
    {
        decklink->Release();
        decklink = NULL;
    }
}

JNIEXPORT jlong JNICALL Java_us_ihmc_javadecklink_Capture_createCaptureSettings
  (JNIEnv *env, jobject, jint codec)
{
	AVCodecID codecid;
	if(codec == 1)
	{
		codecid = AV_CODEC_ID_MJPEG;	
	}
	else if(codec == 2)
	{
		codecid = AV_CODEC_ID_H264;
	}
	else 
	{
        throwRuntimeException(env, "Codec ID invalid");
        return 0;
	}
	
	return (jlong) new DecklinkCaptureSettings(codecid, -1);  	
}

JNIEXPORT jlong JNICALL Java_us_ihmc_javadecklink_Capture_setQuality
  (JNIEnv *, jobject, jlong ptr, jint quality)
{
	DecklinkCaptureSettings* settings = (DecklinkCaptureSettings*) ptr;
	settings->quality = quality;
}
  
JNIEXPORT void JNICALL Java_us_ihmc_javadecklink_Capture_setOption
  (JNIEnv *env, jobject, jlong ptr, jstring option, jstring value)
{
    const char* coption = env->GetStringUTFChars(option,0);
    std::string cppoption(coption);
    env->ReleaseStringUTFChars(option, coption);

    const char* cvalue = env->GetStringUTFChars(value,0);
    std::string cppvalue(cvalue);
    env->ReleaseStringUTFChars(value, cvalue);
    


	DecklinkCaptureSettings* settings = (DecklinkCaptureSettings*) ptr;
	settings->options.push_back(std::make_pair(cppoption, cppvalue));
}




JNIEXPORT jlong JNICALL Java_us_ihmc_javadecklink_Capture_getHardwareTime
  (JNIEnv *, jobject, jlong ptr)
{
    return ((DeckLinkCaptureDelegate*) ptr)->getHardwareTime();
}


JNIEXPORT void JNICALL Java_us_ihmc_javadecklink_Capture_stopCaptureNative
  (JNIEnv *, jobject, jlong delegatePtr)
{
    DeckLinkCaptureDelegate* delegate = (DeckLinkCaptureDelegate*) delegatePtr;
    delegate->Stop();
}


jlong JNICALL startCaptureNative_Impl
(JNIEnv *env, jobject obj, jstring filename, jstring jformat, jboolean recordAudio, jint device, jlong settingsPtr)
{

	DecklinkCaptureSettings* settings = (DecklinkCaptureSettings*) settingsPtr;


	IDeckLinkIterator*				deckLinkIterator = NULL;
	IDeckLink*						deckLink = NULL;

	IDeckLinkDisplayModeIterator*	displayModeIterator = NULL;
	IDeckLinkDisplayMode*			displayMode = NULL;
    char*							displayModeName = NULL;

#ifdef DECKLINK_VERSION_10
#warning "Compiling for decklink 10"
    BMDDisplayModeSupport                   displayModeSupported;
#elseif DECKLINK_VERSION 11
    #warning "Compiling for decklink 11"
    bool			displayModeSupported;
#else
    #warning "Compiling for decklink 12"
    bool			displayModeSupported;
    BMDDisplayMode	actualDisplayMode;
#endif

	DeckLinkCaptureDelegate*		delegate = NULL;

    HRESULT                                                 result;

    IDeckLinkInput*	g_deckLinkInput = NULL;
    IDeckLinkConfiguration* deckLinkConfiguration = NULL;

    int displayModeId;
    int idx = device;

    JavaVM* vm;
    JNIassert(env, env->GetJavaVM(&vm) == 0);

    const char* str = env->GetStringUTFChars(filename,0);
    std::string cfilename(str);
    env->ReleaseStringUTFChars(filename, str);
    
    
    std::string format;
    if(jformat != NULL)
    {
	    const char* formatin = env->GetStringUTFChars(jformat,0);
		format = formatin;
		env->ReleaseStringUTFChars(jformat, formatin);
    }
    else
    {
    	format.clear();
    }

    jclass cls = env->GetObjectClass(obj);
    jmethodID method = env->GetMethodID(cls, "receivedFrameAtHardwareTimeFromNative", "(JJJJ)V");
    jmethodID stop = env->GetMethodID(cls, "stopCaptureFromNative", "()V");
    if(!method)
    {
        throwRuntimeException(env, "Cannot find method receivedFrameAtHardwareTimeFromNative");
        goto bail;
    }
    if(!stop)
    {
        throwRuntimeException(env, "Cannot find method stopCaptureFromNative");
        goto bail;
    }



	// Get the DeckLink device
	deckLinkIterator = CreateDeckLinkIteratorInstance();
	if (!deckLinkIterator)
	{
		fprintf(stderr, "This application requires the DeckLink drivers installed.\n");
		goto bail;
	}


	while ((result = deckLinkIterator->Next(&deckLink)) == S_OK)
	{
		if (idx == 0)
			break;
		--idx;

		deckLink->Release();
	}

	if (result != S_OK || deckLink == NULL)
	{
        fprintf(stderr, "Unable to get DeckLink device %u\n", device);
		goto bail;
	}

	// Get the input (capture) interface of the DeckLink device
    result = deckLink->QueryInterface(IID_IDeckLinkInput, (void**)&g_deckLinkInput);
	if (result != S_OK)
		goto bail;

/*
    result = deckLink->QueryInterface(IID_IDeckLinkConfiguration,
                                      (void **)&deckLinkConfiguration);
    if (result != S_OK) {
        printf("Cannot get configuration\n");
        goto bail;
    }

    result = deckLinkConfiguration->SetInt(bmdDeckLinkConfigVideoInputConnection, bmdVideoConnectionSDI);
    if (result != S_OK) {
        printf("Cannot switch to SDI input\n");
        goto bail;
    }
    */
    result = g_deckLinkInput->GetDisplayModeIterator(&displayModeIterator);
	if (result != S_OK)
		goto bail;

    displayModeId = 0;
	while ((result = displayModeIterator->Next(&displayMode)) == S_OK)
	{
        if (displayModeId == 0)
			break;
        --displayModeId;

		displayMode->Release();
	}

	if (result != S_OK || displayMode == NULL)
	{
        fprintf(stderr, "Unable to get display mode %d\n", displayModeId);
		goto bail;
	}

	// Get display mode name
	result = displayMode->GetName((const char**)&displayModeName);
	if (result != S_OK)
	{
		displayModeName = (char *)malloc(32);
        snprintf(displayModeName, 32, "[index %d]", displayModeId);
	}


#ifdef DECKLINK_VERSION_10

	// Check display mode is supported with given options
    result = g_deckLinkInput->DoesSupportVideoMode(displayMode->GetDisplayMode(), bmdFormat8BitYUV, bmdVideoInputFlagDefault, &displayModeSupported, NULL);
    if (result != S_OK)
		goto bail;

    if (displayModeSupported == bmdDisplayModeNotSupported)
	{
		fprintf(stderr, "The display mode %s is not supported with the selected pixel format\n", displayModeName);
		goto bail;
	}
#elseif DECKLINK_VERSION 11
    // Check display mode is supported with given options
    result = g_deckLinkInput->DoesSupportVideoMode(bmdVideoConnectionUnspecified, displayMode->GetDisplayMode(), bmdFormat8BitYUV, bmdSupportedVideoModeDefault, &displayModeSupported);
    if (result != S_OK)
        goto bail;

    if (!displayModeSupported)
    {
        fprintf(stderr, "The display mode %s is not supported with the selected pixel format\n", displayModeName);
        goto bail;
    }
#else
    // Check display mode is supported with given options
    result = g_deckLinkInput->DoesSupportVideoMode(bmdVideoConnectionUnspecified, displayMode->GetDisplayMode(), bmdFormat8BitYUV, bmdNoVideoInputConversion, bmdSupportedVideoModeDefault, &actualDisplayMode, &displayModeSupported);
    if (result != S_OK)
        goto bail;

    fprintf(stdout, "Actual display mode %d", actualDisplayMode);

    if (!displayModeSupported)
    {
        fprintf(stderr, "The display mode %s is not supported with the selected pixel format\n", displayModeName);
        goto bail;
    }
#endif

    // Configure the capture callback
    delegate = new DeckLinkCaptureDelegate(cfilename, format, recordAudio, settings, deckLink, g_deckLinkInput, vm, env->NewGlobalRef(obj), method, stop);

    if(!delegate->valid)
    {
        delete delegate;
        delegate = NULL;
        goto bail;
    }

	g_deckLinkInput->SetCallback(delegate);

    // Start capturing
    result = g_deckLinkInput->EnableVideoInput(displayMode->GetDisplayMode(), bmdFormat8BitYUV, bmdVideoInputFlagDefault | bmdVideoInputEnableFormatDetection);
    if (result != S_OK)
    {
        fprintf(stderr, "Failed to enable video input. Is another application using the card?\n");
        delete delegate;
        delegate = NULL;
        goto bail;
    }
    
    result = g_deckLinkInput->EnableAudioInput(bmdAudioSampleRate48kHz,
                                             delegate->getAudioSampleDepth(),
                                             delegate->getAudioChannels());
    if (result != S_OK)
    {
        fprintf(stderr, "Failed to enable audio input. Is another application using the card?\n");
        delete delegate;
        delegate = NULL;
        goto bail;
    }                                         
                                             

    result = g_deckLinkInput->StartStreams();
    if (result != S_OK)
    {
        delete delegate;
        delegate = NULL;
        goto bail;

    }

bail:

	if (displayModeName != NULL)
		free(displayModeName);

	if (displayMode != NULL)
		displayMode->Release();

	if (displayModeIterator != NULL)
		displayModeIterator->Release();


    if (deckLinkIterator != NULL)
		deckLinkIterator->Release();

    return (jlong) delegate;
}



JNIEXPORT jlong JNICALL Java_us_ihmc_javadecklink_Capture_startCaptureNative
  (JNIEnv *env, jobject obj, jstring filename, jstring jformat, jint device, jlong settingsPtr)
{
    return startCaptureNative_Impl(env, obj, filename, jformat, false, device, settingsPtr);
}

JNIEXPORT jlong JNICALL Java_us_ihmc_javadecklink_Capture_startCaptureNativeWithAudio
(JNIEnv *env, jobject obj, jstring filename, jstring jformat, jint device, jlong settingsPtr)
{
    return startCaptureNative_Impl(env, obj, filename, jformat, true, device, settingsPtr);
}
