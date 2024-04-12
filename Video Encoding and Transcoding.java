import java.io.File;
import java.io.IOException;

import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.PointerPointer;

public class VideoTranscoder {
    public static void main(String[] args) {
        try {
            // Load the FFmpeg native libraries
            Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);

            // Open the input video file
            AVFormatContext inputFormatContext = new AVFormatContext(null);
            avformat.avformat_open_input(inputFormatContext, "input.mp4", (AVCodecContext)null, (PointerPointer)null);
            avformat.avformat_find_stream_info(inputFormatContext, (PointerPointer)null);

            // Get the first video stream
            int videoStreamIndex = av_find_best_stream(inputFormatContext, avformat.AVMEDIA_TYPE_VIDEO, -1, -1, (AVCodecContext)null, 0);
            AVCodecContext inputCodecContext = inputFormatContext.streams(videoStreamIndex).codec();

            // Open the output video file
            AVFormatContext outputFormatContext = new AVFormatContext(null);
            avformat.avformat_alloc_output_context2(outputFormatContext, (AVFormatContext)null, "mp4", "output.mp4");
            AVCodecContext outputCodecContext = outputFormatContext.streams(0).codec();

            // Configure the output codec
            AVCodec outputCodec = avcodec.avcodec_find_encoder(avcodec.AV_CODEC_ID_H264);
            avcodec.avcodec_open2(outputCodecContext, outputCodec, (AvDictionary)null);
            outputCodecContext.width(inputCodecContext.width());
            outputCodecContext.height(inputCodecContext.height());
            outputCodecContext.time_base(inputFormatContext.streams(videoStreamIndex).time_base());
            outputCodecContext.pix_fmt(inputCodecContext.pix_fmt());

            // Transcode the video
            BytePointer inputBuffer = new BytePointer(1024 * 1024);
            BytePointer outputBuffer = new BytePointer(1024 * 1024);
            while (true) {
                int readBytes = avformat.av_read_frame(inputFormatContext, inputBuffer);
                if (readBytes < 0) {
                    break;
                }
                int encodedBytes = avcodec.avcodec_encode_video2(outputCodecContext, outputBuffer, inputBuffer, readBytes);
                if (encodedBytes >= 0) {
                    avformat.av_write_frame(outputFormatContext, outputBuffer);
                }
            }

            // Flush the output
            avcodec.avcodec_encode_video2(outputCodecContext, outputBuffer, (BytePointer)null, 0);
            avformat.av_write_trailer(outputFormatContext);

            // Close the input and output files
            avformat.avformat_close_input(inputFormatContext);
            avformat.avformat_free_context(outputFormatContext);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int av_find_best_stream(AVFormatContext formatContext, int mediaType, int streamIndex, int wantedStreamIndex, AVCodecContext avCodecContext, int flags) {
        return avformat.av_find_best_stream(formatContext, mediaType, streamIndex, wantedStreamIndex, (AVCodec)avCodecContext, flags);
    }
}
