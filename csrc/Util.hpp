#include <jni.h>
#include <iostream>
#include <pthread.h>
#include <sys/types.h>
#include <string>
#include <sstream>
#include <cerrno>
#include <cstring>

#include <unistd.h>
#include <sys/syscall.h>


/**
 * Check if cond == true, otherwise throw a Java RuntimeException
 *
 * @param env Java environment
 * @Param
 */
#define JNIassert(env, cond) \
    do { \
        if(!(cond)) { \
            throwRuntimeException((env), __FILE__, __PRETTY_FUNCTION__, __LINE__, errno); \
        } \
    } while(0)

inline void throwRuntimeException(JNIEnv* env, std::string file, std::string function, int line, int err);

/**
 * Attach the current thread to the VM if necessary and return the Java environment
 *
 * @param vm Java VM
 */
inline JNIEnv* getEnv(JavaVM* vm)
{
    // Get the java environment
    JNIEnv* env;
    int getEnvStat = vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED)
    {
        std::cout << "Attaching native thread to JVM" << std::endl;

        if (vm->AttachCurrentThread((void **) &env, NULL)
                != 0)
        {
            std::cerr << "Failed to attach" << std::endl;
            return 0;
        }

    }
    else if (getEnvStat == JNI_EVERSION)
    {
        std::cerr << "GetEnv: Version not supported" << std::endl;
        return 0;
    }
    else if (getEnvStat == JNI_OK)
    {
        //
    }

    return env;
}

inline void releaseEnv(JavaVM* vm)
{
    vm->DetachCurrentThread();
}

/**
 * Helper function to throw a Java runtime exception
 *
 * @param env Java environment
 * @param msg Message to throw
 */
inline void throwRuntimeException(JNIEnv* env, std::string msg)
{
    jclass exClass = env->FindClass("java/lang/RuntimeException");
    env->ThrowNew(exClass, msg.c_str());
}

/**
 * Helper function to throw a formatted runtime exception
 *
 * @param env Java environment
 * @param file Caller file name
 * @param function Caller function name
 * @param line Caller line #
 */
inline void throwRuntimeException(JNIEnv* env, std::string file, std::string function, int line, int err)
{
    std::stringstream s;
    s << "Exception in " << file << ", " << function << " at line " << line << ": " << strerror(err) << " ";
    throwRuntimeException(env, s.str());
    JavaVM* vm;
    env->GetJavaVM(&vm);
    vm->DetachCurrentThread();
}
