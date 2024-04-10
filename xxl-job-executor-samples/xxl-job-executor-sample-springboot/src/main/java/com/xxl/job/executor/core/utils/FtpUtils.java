package com.xxl.job.executor.core.utils;

import org.springframework.stereotype.Component;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.executor.core.model.CchsParam;
import com.xxl.job.executor.core.model.FtpParam;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

@Component
public class FtpUtils {

    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 上传文件
     * @param client client
     * @param params params
     * @return boolean
     */
    public boolean uploadFiles(FTPClient client, CchsParam params) {
        if (client == null) {
            XxlJobHelper.log("FTPClient is null.");
            return false;
        }
        int retries = 2; // 设置最大重试次数
        Exception lastException = null;
        for (int i = 0; i <= retries; i++) {
            try {
                String remotePath = params.getRemotePath();
                if (remotePath != null && !remotePath.isEmpty()) {
                    if (!client.changeWorkingDirectory(remotePath)){
                        XxlJobHelper.log("change remote directory failed, remotePath: " +remotePath);
                        return false;
                    }
                }
                // 查看上传文件夹是否有文件
                File localFile = new File(params.getLocalPath());
                if (!localFile.exists()) {
                    localFile.mkdirs();
                }
                File[] localFiles;
                if (localFile.isDirectory()) {
                    localFiles = localFile.listFiles();
                    if (localFiles == null || localFiles.length == 0) {
                        XxlJobHelper.log("uploadPath is empty.");
                        return false;
                    }
                    // 上传文件
                    Arrays.stream(localFiles).filter(File::isFile).forEach(item -> this.uploadFile(client, item));
                }
                return true;
            } catch (Exception e) {
                lastException = e; // 记录最后一次异常
                XxlJobHelper.log("Upload attempt " + (i + 1) + " failed, message: " + e.getMessage());
                if (i < retries) {
                    XxlJobHelper.log("Retrying upload (" + (retries - i) + " attempts remaining)...");
                }
            }
        }
        XxlJobHelper.log("All " + retries + " upload attempts failed.");
        XxlJobHelper.log("Final exception: ", lastException);
        return false;
    }

    private void uploadFile(FTPClient client, File localFile) {
        try (FileInputStream fis = new FileInputStream(localFile)){
            // 上传文件到FTP服务器
            String fileName = localFile.getName();
            client.storeFile(fileName, fis);
            // 删除文件
            localFile.delete();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    /**
     * 按照前缀下载文件
     * @param client client
     * @param params params
     * @return boolean
     */
    public boolean downLoadFiles(FTPClient client, CchsParam params){
        // 下载文件
        if (client == null) {
            XxlJobHelper.log("FTPClient is null.");
            return false;
        }
        try {
            String remotePath = params.getRemotePath();
            if (remotePath != null && !remotePath.isEmpty()) {
                if (!client.changeWorkingDirectory(remotePath)){
                    XxlJobHelper.log("change remote directory failed, remotePath: " +remotePath);
                    return false;
                }
            }
            // 获取需要下载的文件名
            List<String> remoteFileNames = Arrays.stream(client.listFiles())
                    .filter(FTPFile::isFile)
                    .map(FTPFile::getName)
                    .collect(Collectors.toList());

            if (remoteFileNames.isEmpty()) {
                XxlJobHelper.log("no files found on the FTP server, remotePath: " + params.getRemotePath());
                return false;
            }
            List<String> fileNames = makeFileNames(remoteFileNames, params);
            if (fileNames.isEmpty()){
                XxlJobHelper.log("no files needs to download");
                return false;
            }
            fileNames.forEach(fileName -> this.downloadFile(client, params.getLocalPath(), fileName, true));
        } catch (Exception e) {
            XxlJobHelper.log("download file failed, message " + e.getMessage());
            return false;
        }
        return true;
    }

    private List<String> makeFileNames(List<String> remoteFileNames, CchsParam params) {
        List<String> names = new ArrayList<>();
        if (params.getFixedNames() != null && !params.getFixedNames().isEmpty()) {
            names.addAll(params.getFixedNames());
        }

        if (params.getPrefixNames() != null && !params.getPrefixNames().isEmpty()) {
            params.getPrefixNames().stream().filter(prefixName -> prefixName != null && !prefixName.isEmpty())
                .forEach(prefixName -> remoteFileNames.forEach(remoteName ->
                    {
                        if (remoteName.startsWith(prefixName)){
                        names.add(remoteName);
                    }
                }));
        }
        return names;
    }

    private static LocalDate extractTimestamp(String fileName){
        String nameFirst = fileName.split("\\.")[0];
        return LocalDate.parse(nameFirst.substring(nameFirst.length() - 8), formatter);
    }

    /**
     * 下载文件
     * @param client client
     * @param filePath filePath
     * @param fileName fileName
     */
    private void downloadFile(FTPClient client, String filePath, String fileName, boolean deleteFile) {
        File downloadedFile = new File(filePath + fileName);
        try (FileOutputStream fos = new FileOutputStream(downloadedFile)) {
            if (client != null){
                // 下载文件
                boolean success = client.retrieveFile(fileName, fos);
                if (success){
                    XxlJobHelper.log("download file success, fileName: " + fileName);
                    if (deleteFile){
                        boolean deleted = client.deleteFile(fileName);
                        if (deleted){
                            XxlJobHelper.log("delete file success, fileName: " + fileName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            XxlJobHelper.log("download file failed" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 登录ftp
     * @param ftpParam dictType
     * @return FTPClient
     */
    public FTPClient loginFtp(FtpParam ftpParam) {
        try {
            FTPClient client = new FTPClient();
            client.connect(ftpParam.server, ftpParam.port);
            client.login(ftpParam.user, ftpParam.password);
            client.enterLocalPassiveMode();
            XxlJobHelper.log("login FTP successful!");
            client.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
            client.setFileType(FTP.BINARY_FILE_TYPE);
            client.setBufferSize(1024 * 1024);
            return client;
        } catch (Exception e) {
            XxlJobHelper.log("login FTP failed! " + e.getMessage());
        }
        return null;
    }

    /**
     * 备份文件
     * @param sourceDirPath sourceDirPath
     * @param destDirPath destDirPath
     * @throws IOException IOException
     */
    public static void copyFilesFromDirToDir(String sourceDirPath, String destDirPath) throws IOException {
        // 创建目标目录如果不存在
        Files.createDirectories(Paths.get(destDirPath));

        // 遍历源目录下的所有文件（假定只有第一层）
        File sourceDir = new File(sourceDirPath);
        File[] filesInSourceDir = sourceDir.listFiles();

        if (filesInSourceDir == null || filesInSourceDir.length == 0) {
            XxlJobHelper.log("Source directory is empty.");
            return;
        }

        for (File sourceFile : filesInSourceDir) {
            if (sourceFile.isFile()) {
                Path destinationFile = Paths.get(destDirPath, sourceFile.getName());
                try {
                    // 复制文件到目标目录
                    Files.copy(sourceFile.toPath(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
                    // 删除源文件
                    Files.delete(sourceFile.toPath());
                } catch (IOException e) {
                    XxlJobHelper.log("Failed to copy or delete file " + sourceFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 按照名称删除一个月前的数据
     * @param backupPath backupPath
     */
    public static void  deleteFileMonthAgo(String backupPath){
        // 添加删除一个月前文件的逻辑（假设都是在当前目录下）
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String hisDate = formatter.format(oneMonthAgo);

        File destDir = new File(backupPath);
        File[] destDirPathDir = destDir.listFiles();
        if (destDirPathDir == null){
            XxlJobHelper.log("backupPath " + backupPath + " is empty");
            return;
        }
        for (File destFile : destDirPathDir) {
            if (destFile.isFile()) {
                String fileName = destFile.getName();
                try {
                    if (fileName.contains(hisDate)){
                        Files.delete(destFile.toPath());
                    } else {
                        String datePart = fileName.substring(14, 22);
                        LocalDate fileDate = LocalDate.parse(datePart, formatter);
                        if (fileDate.isBefore(oneMonthAgo)) {
                            Files.delete(destFile.toPath());
                        }
                    }
                } catch (Exception e) {
                    // 如果解析日期失败或者删除文件时出现异常，则记录日志
                    XxlJobHelper.log("Failed to delete old file: " + destFile.getAbsolutePath());
                }
            }
        }
    }

}
