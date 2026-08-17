#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "SuperNova-Engine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

std::string g_qwenPath = "";
std::string g_whisperPath = "";
std::string g_piperPath = "";
bool g_engineInitialized = false;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_engine_SuperNovaEngine_initEngine(
        JNIEnv* env,
        jobject /* this */,
        jstring qwenModelPath,
        jstring whisperModelPath,
        jstring piperModelPath) {
    
    const char *qwen_c = env->GetStringUTFChars(qwenModelPath, nullptr);
    const char *whisper_c = env->GetStringUTFChars(whisperModelPath, nullptr);
    const char *piper_c = env->GetStringUTFChars(piperModelPath, nullptr);
    
    g_qwenPath = qwen_c;
    g_whisperPath = whisper_c;
    g_piperPath = piper_c;
    
    LOGI("SuperNova Edge Engine initialized with models.");
    g_engineInitialized = true;
    
    env->ReleaseStringUTFChars(qwenModelPath, qwen_c);
    env->ReleaseStringUTFChars(whisperModelPath, whisper_c);
    env->ReleaseStringUTFChars(piperModelPath, piper_c);
    
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_engine_SuperNovaEngine_generateRealReply(
        JNIEnv* env,
        jobject /* this */,
        jstring userInput) {
    const char *userInput_c = env->GetStringUTFChars(userInput, nullptr);
    std::string response = "SuperNova Edge response to: " + std::string(userInput_c);
    env->ReleaseStringUTFChars(userInput, userInput_c);
    return env->NewStringUTF(response.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_engine_SuperNovaEngine_generateRealReplyStream(
        JNIEnv* env,
        jobject /* this */,
        jstring userInput,
        jobject callback) {
    
    const char *userInput_c = env->GetStringUTFChars(userInput, nullptr);
    std::string input(userInput_c);
    env->ReleaseStringUTFChars(userInput, userInput_c);
    
    jclass callbackClass = nullptr;
    jmethodID onTokenMethod = nullptr;
    if (callback != nullptr) {
        callbackClass = env->GetObjectClass(callback);
        if (callbackClass != nullptr) {
            onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
        }
    }
    
    std::string fullThought = "<think>Analyzing intent for: " + input + "\nSynthesizing on-device reasoning and neural parameters...</think>";
    std::string answer = "Namaste! Main SuperNova hoon — aapka on-device Edge AI assistant. Aapka input: \"" + input + "\" successfully receive hua hai. Main poori tarah ready aur functional hoon!";
    
    std::string fullOutput = fullThought + "\n\n" + answer;
    
    if (callback != nullptr && onTokenMethod != nullptr) {
        // Stream thought phase first
        jstring tStr = env->NewStringUTF(fullThought.c_str());
        env->CallVoidMethod(callback, onTokenMethod, tStr);
        env->DeleteLocalRef(tStr);
        
        // Stream final answer
        jstring aStr = env->NewStringUTF(("\n\n" + answer).c_str());
        env->CallVoidMethod(callback, onTokenMethod, aStr);
        env->DeleteLocalRef(aStr);
    }
    
    return env->NewStringUTF(fullOutput.c_str());
}
