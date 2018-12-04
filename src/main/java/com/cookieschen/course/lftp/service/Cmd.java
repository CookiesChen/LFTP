package com.cookieschen.course.lftp.service;

import picocli.CommandLine;

public class Cmd {
    @CommandLine.Option(names = {"-a", "--action" }, description = "Send or Get")
    public String action;

    @CommandLine.Option(names = { "-i", "--ip" },  description = "Ip")
    public String ip;

    @CommandLine.Option(names = { "-f", "--file" }, description = "filename")
    public String filename;
}
