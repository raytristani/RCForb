#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include "speex/speex.h"
#include "speex/speex_bits.h"

#define LOG_TAG "SpeexJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

typedef struct {
    void *dec_state;
    SpeexBits bits;
    int frame_size;
} SpeexHandle;

JNIEXPORT jlong JNICALL
Java_com_rcforb_android_audio_SpeexNative_nativeCreate(JNIEnv *env, jclass clazz) {
    SpeexHandle *h = (SpeexHandle *)calloc(1, sizeof(SpeexHandle));
    if (!h) return 0;

    h->dec_state = speex_decoder_init(&speex_nb_mode);
    speex_bits_init(&h->bits);

    int enh = 1;
    speex_decoder_ctl(h->dec_state, SPEEX_SET_ENH, &enh);
    speex_decoder_ctl(h->dec_state, SPEEX_GET_FRAME_SIZE, &h->frame_size);

    return (jlong)(intptr_t)h;
}

JNIEXPORT jbyteArray JNICALL
Java_com_rcforb_android_audio_SpeexNative_nativeDecode(JNIEnv *env, jclass clazz,
        jlong handle, jbyteArray encoded) {
    SpeexHandle *h = (SpeexHandle *)(intptr_t)handle;
    if (!h || !h->dec_state) return NULL;

    jsize in_len = (*env)->GetArrayLength(env, encoded);
    jbyte *in_data = (*env)->GetByteArrayElements(env, encoded, NULL);

    speex_bits_read_from(&h->bits, (const char *)in_data, in_len);
    (*env)->ReleaseByteArrayElements(env, encoded, in_data, JNI_ABORT);

    // Decode all frames in the packet
    int total_samples = 0;
    short output[160 * 10]; // max 10 frames
    while (speex_bits_remaining(&h->bits) > 10) {
        int ret = speex_decode_int(h->dec_state, &h->bits, output + total_samples);
        if (ret != 0) break;
        total_samples += h->frame_size;
        if (total_samples >= 160 * 10) break;
    }

    if (total_samples == 0) return NULL;

    int out_bytes = total_samples * 2;
    jbyteArray result = (*env)->NewByteArray(env, out_bytes);
    (*env)->SetByteArrayRegion(env, result, 0, out_bytes, (jbyte *)output);
    return result;
}

JNIEXPORT void JNICALL
Java_com_rcforb_android_audio_SpeexNative_nativeDestroy(JNIEnv *env, jclass clazz, jlong handle) {
    SpeexHandle *h = (SpeexHandle *)(intptr_t)handle;
    if (!h) return;
    if (h->dec_state) {
        speex_bits_destroy(&h->bits);
        speex_decoder_destroy(h->dec_state);
    }
    free(h);
}

/* --- Encoder --- */

typedef struct {
    void *enc_state;
    SpeexBits bits;
    int frame_size;
} SpeexEncHandle;

JNIEXPORT jlong JNICALL
Java_com_rcforb_android_audio_SpeexNative_nativeCreateEncoder(JNIEnv *env, jclass clazz, jint quality) {
    SpeexEncHandle *h = (SpeexEncHandle *)calloc(1, sizeof(SpeexEncHandle));
    if (!h) return 0;

    h->enc_state = speex_encoder_init(&speex_nb_mode);
    speex_bits_init(&h->bits);

    int q = quality;
    speex_encoder_ctl(h->enc_state, SPEEX_SET_QUALITY, &q);
    speex_encoder_ctl(h->enc_state, SPEEX_GET_FRAME_SIZE, &h->frame_size);

    LOGI("Encoder created: quality=%d, frame_size=%d", q, h->frame_size);
    return (jlong)(intptr_t)h;
}

JNIEXPORT jbyteArray JNICALL
Java_com_rcforb_android_audio_SpeexNative_nativeEncode(JNIEnv *env, jclass clazz,
        jlong handle, jbyteArray pcm) {
    SpeexEncHandle *h = (SpeexEncHandle *)(intptr_t)handle;
    if (!h || !h->enc_state) return NULL;

    jsize in_len = (*env)->GetArrayLength(env, pcm);
    int num_samples = in_len / 2;
    if (num_samples < h->frame_size) return NULL;

    jbyte *in_data = (*env)->GetByteArrayElements(env, pcm, NULL);
    short *samples = (short *)in_data;

    // Encode all complete frames into one bitstream (matching NSpeex behavior)
    speex_bits_reset(&h->bits);
    int frames_encoded = 0;
    int offset = 0;
    while (offset + h->frame_size <= num_samples) {
        speex_encode_int(h->enc_state, samples + offset, &h->bits);
        offset += h->frame_size;
        frames_encoded++;
    }
    (*env)->ReleaseByteArrayElements(env, pcm, in_data, JNI_ABORT);

    if (frames_encoded == 0) return NULL;

    char output[4096];
    int written = speex_bits_write(&h->bits, output, sizeof(output));
    if (written <= 0) return NULL;

    jbyteArray result = (*env)->NewByteArray(env, written);
    (*env)->SetByteArrayRegion(env, result, 0, written, (jbyte *)output);
    return result;
}

JNIEXPORT void JNICALL
Java_com_rcforb_android_audio_SpeexNative_nativeDestroyEncoder(JNIEnv *env, jclass clazz, jlong handle) {
    SpeexEncHandle *h = (SpeexEncHandle *)(intptr_t)handle;
    if (!h) return;
    if (h->enc_state) {
        speex_bits_destroy(&h->bits);
        speex_encoder_destroy(h->enc_state);
    }
    free(h);
}
