https://ffmpeg.org/ffmpeg-codecs.html#Options-25

b (bitrate)
Set bitrate in bits/s. Note that FFmpeg’s b option is expressed in bits/s, while x264’s bitrate is in kilobits/s.

bf (bframes)
g (keyint)
qmin (qpmin)
Minimum quantizer scale.

qmax (qpmax)
Maximum quantizer scale.

qdiff (qpstep)
Maximum difference between quantizer scales.

qblur (qblur)
Quantizer curve blur

qcomp (qcomp)
Quantizer curve compression factor

refs (ref)
Number of reference frames each P-frame can use. The range is from 0-16.

sc_threshold (scenecut)
Sets the threshold for the scene change detection.

trellis (trellis)
Performs Trellis quantization to increase efficiency. Enabled by default.

nr (nr)
me_range (merange)
Maximum range of the motion search in pixels.

me_method (me)
Set motion estimation method. Possible values in the decreasing order of speed:

‘dia (dia)’
‘epzs (dia)’
Diamond search with radius 1 (fastest). ‘epzs’ is an alias for ‘dia’.

‘hex (hex)’
Hexagonal search with radius 2.

‘umh (umh)’
Uneven multi-hexagon search.

‘esa (esa)’
Exhaustive search.

‘tesa (tesa)’
Hadamard exhaustive search (slowest).

forced-idr
Normally, when forcing a I-frame type, the encoder can select any type of I-frame. This option forces it to choose an IDR-frame.

subq (subme)
Sub-pixel motion estimation method.

b_strategy (b-adapt)
Adaptive B-frame placement decision algorithm. Use only on first-pass.

keyint_min (min-keyint)
Minimum GOP size.

coder
Set entropy encoder. Possible values:

‘ac’
Enable CABAC.

‘vlc’
Enable CAVLC and disable CABAC. It generates the same effect as x264’s --no-cabac option.

cmp
Set full pixel motion estimation comparison algorithm. Possible values:

‘chroma’
Enable chroma in motion estimation.

‘sad’
Ignore chroma in motion estimation. It generates the same effect as x264’s --no-chroma-me option.

threads (threads)
Number of encoding threads.

thread_type
Set multithreading technique. Possible values:

‘slice’
Slice-based multithreading. It generates the same effect as x264’s --sliced-threads option.

‘frame’
Frame-based multithreading.

flags
Set encoding flags. It can be used to disable closed GOP and enable open GOP by setting it to -cgop. The result is similar to the behavior of x264’s --open-gop option.

rc_init_occupancy (vbv-init)
preset (preset)
Set the encoding preset.

tune (tune)
Set tuning of the encoding params.

profile (profile)
Set profile restrictions.

fastfirstpass
Enable fast settings when encoding first pass, when set to 1. When set to 0, it has the same effect of x264’s --slow-firstpass option.

crf (crf)
Set the quality for constant quality mode.

crf_max (crf-max)
In CRF mode, prevents VBV from lowering quality beyond this point.

qp (qp)
Set constant quantization rate control method parameter.

aq-mode (aq-mode)
Set AQ method. Possible values:

‘none (0)’
Disabled.

‘variance (1)’
Variance AQ (complexity mask).

‘autovariance (2)’
Auto-variance AQ (experimental).

aq-strength (aq-strength)
Set AQ strength, reduce blocking and blurring in flat and textured areas.

psy
Use psychovisual optimizations when set to 1. When set to 0, it has the same effect as x264’s --no-psy option.

psy-rd (psy-rd)
Set strength of psychovisual optimization, in psy-rd:psy-trellis format.

rc-lookahead (rc-lookahead)
Set number of frames to look ahead for frametype and ratecontrol.

weightb
Enable weighted prediction for B-frames when set to 1. When set to 0, it has the same effect as x264’s --no-weightb option.

weightp (weightp)
Set weighted prediction method for P-frames. Possible values:

‘none (0)’
Disabled

‘simple (1)’
Enable only weighted refs

‘smart (2)’
Enable both weighted refs and duplicates

ssim (ssim)
Enable calculation and printing SSIM stats after the encoding.

intra-refresh (intra-refresh)
Enable the use of Periodic Intra Refresh instead of IDR frames when set to 1.

avcintra-class (class)
Configure the encoder to generate AVC-Intra. Valid values are 50,100 and 200

bluray-compat (bluray-compat)
Configure the encoder to be compatible with the bluray standard. It is a shorthand for setting "bluray-compat=1 force-cfr=1".

b-bias (b-bias)
Set the influence on how often B-frames are used.

b-pyramid (b-pyramid)
Set method for keeping of some B-frames as references. Possible values:

‘none (none)’
Disabled.

‘strict (strict)’
Strictly hierarchical pyramid.

‘normal (normal)’
Non-strict (not Blu-ray compatible).

mixed-refs
Enable the use of one reference per partition, as opposed to one reference per macroblock when set to 1. When set to 0, it has the same effect as x264’s --no-mixed-refs option.

8x8dct
Enable adaptive spatial transform (high profile 8x8 transform) when set to 1. When set to 0, it has the same effect as x264’s --no-8x8dct option.

fast-pskip
Enable early SKIP detection on P-frames when set to 1. When set to 0, it has the same effect as x264’s --no-fast-pskip option.

aud (aud)
Enable use of access unit delimiters when set to 1.

mbtree
Enable use macroblock tree ratecontrol when set to 1. When set to 0, it has the same effect as x264’s --no-mbtree option.

deblock (deblock)
Set loop filter parameters, in alpha:beta form.

cplxblur (cplxblur)
Set fluctuations reduction in QP (before curve compression).

partitions (partitions)
Set partitions to consider as a comma-separated list of. Possible values in the list:

‘p8x8’
8x8 P-frame partition.

‘p4x4’
4x4 P-frame partition.

‘b8x8’
4x4 B-frame partition.

‘i8x8’
8x8 I-frame partition.

‘i4x4’
4x4 I-frame partition. (Enabling ‘p4x4’ requires ‘p8x8’ to be enabled. Enabling ‘i8x8’ requires adaptive spatial transform (8x8dct option) to be enabled.)

‘none (none)’
Do not consider any partitions.

‘all (all)’
Consider every partition.

direct-pred (direct)
Set direct MV prediction mode. Possible values:

‘none (none)’
Disable MV prediction.

‘spatial (spatial)’
Enable spatial predicting.

‘temporal (temporal)’
Enable temporal predicting.

‘auto (auto)’
Automatically decided.

slice-max-size (slice-max-size)
Set the limit of the size of each slice in bytes. If not specified but RTP payload size (ps) is specified, that is used.

stats (stats)
Set the file name for multi-pass stats.

nal-hrd (nal-hrd)
Set signal HRD information (requires vbv-bufsize to be set). Possible values:

‘none (none)’
Disable HRD information signaling.

‘vbr (vbr)’
Variable bit rate.

‘cbr (cbr)’
Constant bit rate (not allowed in MP4 container).