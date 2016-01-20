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
#include <boost/thread/tss.hpp>
#include <iostream>

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

class ThreadJNIEnv {
public:
    JavaVM *vm;
    JNIEnv *env;

    ThreadJNIEnv(JavaVM *vm) :
        vm(vm)
    {
        std::cout << "Attaching thread" << std::endl;
        vm->AttachCurrentThread((void **) &env, NULL);
    }

    ~ThreadJNIEnv() {
        std::cout << "Detaching thread" << std::endl;
        vm->DetachCurrentThread();
    }
};

