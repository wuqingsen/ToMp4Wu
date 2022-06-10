# ToMp4Wu

#### 介绍
TestToMp4-master 这是录制音视频流以及合成为mp4的demo，里面在录制视频的过程中添加了水印；
OpenGlRecordMp4Wu和OpenGlRecordVideoWu 录制音频和视频合成为mp4，用到opengl添加水印，打开会闪退，里面要手动授予权限，Android10以上要修改mp4文件保存位置。
其它的项目是学习的记录。

#### 软件架构
录制视频用的分为录制音频流和视频流；
1. 利用Android的Camera视频流，开启子线程VideoEncoderThread利用MediaCodec实现视频流的编码，编码为h264格式;
2. 利用Android的AudioRecord实现录制音频流，开启子线程AudioEncoderThread利用MediaCodec实现音频流的编码;
3. 最后自定义MediaMuxerThread音视频混合线程为音视频流分别添加音视频轨道，并且实时写入到mp4文件;

#### 使用说明
详见具体代码

#### 参与贡献
吴庆森

#### 博客地址
https://blog.csdn.net/wuqingsen1/article/details/103799520
https://blog.csdn.net/wuqingsen1/article/details/111165018
