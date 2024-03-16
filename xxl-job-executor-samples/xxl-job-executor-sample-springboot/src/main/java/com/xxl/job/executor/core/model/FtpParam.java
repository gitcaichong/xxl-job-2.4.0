package com.xxl.job.executor.core.model;

public class FtpParam{
	
	public String server;
	public String user;
	public String password;
	public int port;

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "FtpParam{" +
				"server='" + server + '\'' +
				", user='" + user + '\'' +
				", password='" + password + '\'' +
				", port=" + port +
				'}';
	}
}
