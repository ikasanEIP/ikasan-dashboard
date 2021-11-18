package org.ikasan.dashboard;

//import com.sshtools.net.SocketTransport;
//import com.sshtools.ssh.*;
import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.client.shell.ShellTimeoutException;
import com.sshtools.client.tasks.ShellTask;
import com.sshtools.common.auth.PasswordAuthentication;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SshException;
import org.junit.jupiter.api.Test;

import java.io.*;

public class SSHTest {

    @Test
    public void test() throws ChannelOpenException, SshException, IOException, InterruptedException {
//        SshConnector con = SshConnector.createInstance();
//        SshClient ssh = con.connect(
//            new SocketTransport("127.0.0.1", 22),
//            "mick");
//
//        PasswordAuthentication pwd = new PasswordAuthentication();
//        pwd.setPassword("StJean-2019");
//
//        if(ssh.authenticate(pwd)== SshAuthentication.COMPLETE) {
//            SshSession session = ssh.openSessionChannel();
//
//            if(session.requestPseudoTerminal("vt100",
//                80,
//                24,
//                0,
//                0)) {
//                session.startShell();
//
//                session.getOutputStream().write("tail -f less /opt/platform/solr-8.2.0/server/logs/solr.log\n".getBytes());
//                InputStream inputStream = session.getInputStream();
//
//                BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(session.getInputStream()));
//                String line;
//                while((line = reader.readLine())!=null) {
//                    System.out.println(line);
//                }
//
//            }
//        } else {
//            System.out.println("Authentication failed");
//        }

        try(SshClient ssh = new SshClient("localhost", 22, "mick", "StJean-2019".toCharArray())) {

            ssh.runTask(new ShellTask(ssh) {
                @Override protected void onOpenSession(SessionChannelNG session)
                    throws IOException, SshException, ShellTimeoutException {
                    try {
                        session.getOutputStream().write("tail -f less /opt/platform/solr-8.2.0/server/logs/solr.log\n".getBytes());

                        BufferedReader reader = new BufferedReader(
                            new InputStreamReader(session.getInputStream()));
                        String line;
                        while((line = reader.readLine())!=null) {
                            System.out.println(line);
                        }
                    } catch (IOException e) {

                    }
                }
            });
        }
    }
}
