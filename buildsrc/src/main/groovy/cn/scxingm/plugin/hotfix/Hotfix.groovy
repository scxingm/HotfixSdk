package cn.scxingm.plugin.hotfix

import javassist.CtClass
import javassist.CtConstructor;
import org.gradle.api.DefaultTask;

import javassist.ClassPool
import org.gradle.api.Project

import javax.inject.Inject
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry;

/**
 * Created by scxingm on 2018/1/4.
 */

public class Hotfix extends DefaultTask {

    private ClassPool pool= ClassPool.getDefault()

    public void makeHotfixJar(){
        makeJar()
    }

    private void makeJar(){
        // android开发SDK下的android.jar
        pool.appendClassPath("D:/android-studio-sdk/platforms/android-27/android.jar")
        project.logger.error "---> 开始进行Jar包打桩操作!"

        // 混淆过得sdk的jar包路径
        File jarFile = new File("E:/android_workspace/HotfixSdk/hotfix/libs/hotfix-release.jar")
        project.logger.error "---> "+jarFile.getAbsolutePath()
        if(!jarFile.exists()) return;

        // 判断路径是jar结尾
        if (jarFile.getAbsolutePath().endsWith(".jar")) {
            // jar包解压后的保存路径
            String jarZipDir = jarFile.getParent() + "/" + jarFile.getName().replace('.jar', '')

            // 解压jar包, 返回jar包中所有class的完整类名的集合（带.class后缀）
            List classNameList = unzipJar(jarFile.getAbsolutePath(), jarZipDir)

            // 添加jar解压后的路径
            pool.appendClassPath(jarZipDir)
            // 注入代码
            for (String className : classNameList) {
                // 操作类文件
                if (className.endsWith(".class") && !className.contains("Predicate.class")){
                    // class文件名
                    className = className.substring(0, className.length() - 6)
                    // class字符插入
                    injectClass(className, jarZipDir)
                }
            }

            project.logger.error "---> Jar包打桩操作完成!"

        }
    }

    // 类文件打桩
    private void injectClass(String className, String path) {
        // 获取class类
        CtClass c = pool.getCtClass(className)

        // 是否被冻结
        if (c.isFrozen()) {
            c.defrost() // 解冻
        }
        // 返回指定参数类型的所有构造器
        CtConstructor[] cts = c.getDeclaredConstructors()

        if (cts == null || cts.length == 0) { // 无构造方法
            // 添加构造方法
            insertNewConstructor(c)
        } else { // 插入字段
            cts[0].insertBeforeBody(
                    "if (Boolean.FALSE.booleanValue()){System.out.println(com.android.internal.util.Predicate.class);}"
            )
            project.logger.error "---> 类文件‘"+className+"’打桩成功!"
        }

        c.writeFile(path)
        c.detach()
    }

    // 添加构造方法
    private void insertNewConstructor(CtClass c) {
        CtConstructor constructor = new CtConstructor(new CtClass[0], c)
        constructor.insertBeforeBody(
                "if (Boolean.FALSE.booleanValue()){System.out.println(com.android.internal.util.Predicate.class);}"
        )
        c.addConstructor(constructor)
        project.logger.error "---> 类文件‘"+className+"’打桩成功!"
    }

    /**
     * 将该jar包解压到指定目录
     * @param jarPath jar包的绝对路径
     * @param destDirPath jar包解压后的保存路径
     * @return 返回该jar包中包含的所有class的完整类名类名集合，其中一条数据如：com.aitski.hotpatch.Xxxx.class
     */
    public List unzipJar(String jarPath, String destDirPath) {

        List list = new ArrayList()
        if (jarPath.endsWith('.jar')) {

            JarFile jarFile = new JarFile(jarPath)
            Enumeration<JarEntry> jarEntrys = jarFile.entries()
            while (jarEntrys.hasMoreElements()) {
                JarEntry jarEntry = jarEntrys.nextElement()
                if (jarEntry.directory) {
                    continue
                }
                String entryName = jarEntry.getName()
                if (entryName.endsWith('.class')) {
                    String className = entryName.replace('\\', '.').replace('/', '.')
                    list.add(className)
                }
                String outFileName = destDirPath + "/" + entryName
                File outFile = new File(outFileName)
                outFile.getParentFile().mkdirs()
                InputStream inputStream = jarFile.getInputStream(jarEntry)
                FileOutputStream fileOutputStream = new FileOutputStream(outFile)
                fileOutputStream << inputStream
                fileOutputStream.close()
                inputStream.close()
            }
            jarFile.close()
        }
        return list
    }
}
