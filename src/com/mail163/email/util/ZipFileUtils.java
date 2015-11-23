package com.mail163.email.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public final class ZipFileUtils
{
    private static final int BUFFER = 4096;

    private ZipFileUtils()
    {
    }
    /**
     * 将SD卡上ZIP文件里面的内容复制到软件目录�?
     * @param zipFile ZIP路径
     * @param targetDir 手机上的路径
     */
    public static void Unzip(String zipFile, String targetDir)
    {
        try
        {
            //文件输入�?
            FileInputStream fis = new FileInputStream(zipFile);
            //ZIP文件输入�?
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            //ZIP里面文件的对�?
            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null)
            {
                int count;
                byte data[] = new byte[BUFFER];
                File entryFile = new File(targetDir + entry.getName());
                File entryDir = new File(entryFile.getParent());
                //如果没这个文件就创建这个文件
                if (!entryDir.exists())
                {
                    entryDir.mkdirs();
                }
                //文件输出�?
                FileOutputStream fos = new FileOutputStream(entryFile);
                //缓冲输出�?
                BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1)
                {
                    bos.write(data, 0, count);
                }
                bos.flush();
                bos.close();
                fos.close();
            }
            zis.close();
            fis.close();
        }
        catch (Exception e)
        {
            System.out.println("复制皮肤出错!"+e.toString());
        }
    }
    /**
     * 获得皮肤包下的配置文件信�?
     * @param zipFile
     * @return
     */
    public static String getSkinInfo(String zipFile)
    {
    	//皮肤文件的配置信�?
    	String skinInfo = "";
        try
        {
            //文件输入�?
            FileInputStream fis = new FileInputStream(zipFile);
            //ZIP文件输入�?
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            //ZIP里面文件的对�?
            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null)
            {
                int count;
                byte data[] = new byte[1024];
                
                if(entry.getName().equals("skininfo.txt"))
                {
                	ByteArrayOutputStream baos = new ByteArrayOutputStream();
                	zis.skip(3);
        			while((count = zis.read(data)) != -1)
        			{
        				baos.write(data,0,count);
        			}
        			skinInfo = new String(baos.toByteArray(),"UTF_8");
        			baos.close();
        			return skinInfo;
                }
                else
                {
                	continue;
                }
            }
            zis.close();
            fis.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return skinInfo;
    }
    
    /**
     * 解压缩一个文件
     * 
     * @param zipFile 压缩文件
     * @param folderPath 解压缩的目标目录
     * @throws IOException 当解压缩过程出错时抛出
     */
    public static void upZipFile(File zipFile, String folderPath) throws ZipException, IOException {
    	int BUFF_SIZE = 1024 * 1024; // 1M Byte
        File desDir = new File(folderPath);
        if (!desDir.exists()) {
            desDir.mkdirs();
        }
        ZipFile zf = new ZipFile(zipFile);
        for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements();) {
            ZipEntry entry = ((ZipEntry)entries.nextElement());
            InputStream in = zf.getInputStream(entry);
            String str = folderPath + File.separator + entry.getName();
            str = new String(str.getBytes("8859_1"), "UTF_8");
            File desFile = new File(str);
            if (!desFile.exists()) {
                File fileParentDir = desFile.getParentFile();
                if (!fileParentDir.exists()) {
                    fileParentDir.mkdirs();
                }
                desFile.createNewFile();
            }
            OutputStream out = new FileOutputStream(desFile);
            byte buffer[] = new byte[BUFF_SIZE];
            int realLength;
            while ((realLength = in.read(buffer)) > 0) {
                out.write(buffer, 0, realLength);
            }
        }
    }

}
