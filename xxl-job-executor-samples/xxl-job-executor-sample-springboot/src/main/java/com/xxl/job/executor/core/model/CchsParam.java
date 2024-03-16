package com.xxl.job.executor.core.model;

import java.util.List;

public class CchsParam {
    
    private FtpParam ftpParam;
    
    private String localPath;
    
    private String backupPath;
    
    private String remotePath;
    
    private List<String> fixedNames;
    
    private List<String> prefixNames;

    public FtpParam getFtpParam() {
        return ftpParam;
    }

    public void setFtpParam(FtpParam ftpParam) {
        this.ftpParam = ftpParam;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public List<String> getFixedNames() {
        return fixedNames;
    }

    public void setFixedNames(List<String> fixedNames) {
        this.fixedNames = fixedNames;
    }

    public List<String> getPrefixNames() {
        return prefixNames;
    }

    public void setPrefixNames(List<String> prefixNames) {
        this.prefixNames = prefixNames;
    }

    @Override
    public String toString() {
        return "CchsParam{" +
                "ftpParam=" + ftpParam +
                ", localPath='" + localPath + '\'' +
                ", backupPath='" + backupPath + '\'' +
                ", remotePath='" + remotePath + '\'' +
                ", fixedNames=" + fixedNames +
                ", prefixNames=" + prefixNames +
                '}';
    }
}
