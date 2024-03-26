package com.xxl.job.executor.service.jobhandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.executor.core.model.CchsParam;
import com.xxl.job.executor.core.utils.FtpUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.net.ftp.FTPClient;

@Component
public class CchsJob {

    private final FtpUtils ftpUtils;

    @Autowired
    public CchsJob(FtpUtils ftpUtils) {
        this.ftpUtils = ftpUtils;
    }

    /**
     * 1、简单任务示例（Bean模式）
     */
    @XxlJob("downloadFile")
    public void downloadFile() throws Exception {
        FTPClient client = null;
        try {
            XxlJobHelper.log("CchsJob downloadFile start.");
            String command = XxlJobHelper.getJobParam();
            Gson gson = new Gson();
            CchsParam params = gson.fromJson(command, CchsParam.class);
            XxlJobHelper.log("CchsJob downloadFile params: " + params);
            client = ftpUtils.loginFtp(params.getFtpParam());
            File file = new File(params.getLocalPath());
            if (!file.exists()) {
                file.mkdirs();
            }
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null && files.length > 0) {
                    Arrays.stream(files).forEach(File::delete);
                }
            }
            if (ftpUtils.downLoadFiles(client, params)){
                XxlJobHelper.log("CchsJob downloadFile succeed.");
            } else {
                XxlJobHelper.log("CchsJob downloadFile failed.");
            }
        }catch (Exception e) {
            XxlJobHelper.log("CchsJob downloadFile failed. message: " + e.getMessage());
        } finally {
            if (client != null && client.isConnected()) {
                try {
                    client.disconnect();
                } catch (IOException ioe) {
                    XxlJobHelper.log("failed to disconnect from FTP server: " + ioe.getMessage());
                }
            }
        }
    }

    @XxlJob("uploadFile")
    public void uploadFile() throws Exception {
        FTPClient client = null;
        try {
            XxlJobHelper.log("CchsJob uploadFile start.");
            String command = XxlJobHelper.getJobParam();
            Gson gson = new Gson();
            CchsParam params = gson.fromJson(command, CchsParam.class);
            XxlJobHelper.log("CchsJob uploadFile params: " + params);
            client = ftpUtils.loginFtp(params.getFtpParam());
            if (ftpUtils.uploadFiles(client, params)){
//                // 备份文件
//                FtpUtils.copyFilesFromDirToDir(params.getLocalPath(), params.getBackupPath());
//                // 删除一个月前的数据
//                FtpUtils.deleteFileMonthAgo(params.getBackupPath());
                XxlJobHelper.log("CchsJob uploadFile succeed.");
            } else {
                XxlJobHelper.log("CchsJob uploadFile failed.");
            }
        }catch (Exception e) {
            XxlJobHelper.log("CchsJob uploadFile failed. message: " + e.getMessage());
        } finally {
            if (client != null && client.isConnected()) {
                try {
                    client.disconnect();
                } catch (IOException ioe) {
                    XxlJobHelper.log("failed to uploadFile from FTP server: " + ioe.getMessage());
                }
            }
        }
    }
}
