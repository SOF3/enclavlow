package io.github.sof3.enclavlow.example;

import edu.hku.cs.uranus.IntelSGX;
import edu.hku.cs.uranus.IntelSGXOcall;
import io.github.sof3.enclavlow.api.Enclavlow;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Entry {
    public static void main(String[] args) throws IOException {
        DatagramSocket sock = new DatagramSocket();
        sock.bind(new InetSocketAddress("0.0.0.0", 12345));

        while (!sock.isClosed()) {
            heartbeat(sock);
        }
    }

    static void heartbeat(DatagramSocket sock) throws IOException {
        DatagramPacket pack = new DatagramPacket(new byte[1500], 1500);
        sock.receive(pack);
        byte[] resp = process(pack.getData());
        pack.setData(resp);
        sock.send(pack);
    }

    @IntelSGX
    static byte[] process(byte[] enc) {
        byte[] secret = getSecret();
        byte[] dec = new byte[secret.length];

        long time = time1();

        for (int i = 0; i < secret.length; i++) {
            dec[i] = (byte) (enc[i] ^ secret[i] ^ time ^ time2());
        }

        int sum = 0;
        for (byte b : dec) {
            sum += b;
        }

        int result = Enclavlow.intSinkMarker(sum % 1000000);
        byte[] resp = new byte[6];
        for (int i = 5; i >= 0; i--) {
            resp[i] = (byte) (result % 10 + (int) '0');
            result /= 10;
        }
        return resp;
    }

    @IntelSGXOcall
    static long time1() {
        return System.currentTimeMillis() / 60000;
    }

    @IntelSGXOcall
    static long time2() {
        return System.currentTimeMillis() / 60000;
    }

    static byte[] getSecret() {
        return Enclavlow.sourceMarker(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    }
}
